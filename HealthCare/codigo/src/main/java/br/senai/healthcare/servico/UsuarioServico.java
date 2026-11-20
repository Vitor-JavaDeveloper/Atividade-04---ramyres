package br.senai.healthcare.servico;

import br.senai.healthcare.modelo.Administrador;
import br.senai.healthcare.modelo.Enfermeiro;
import br.senai.healthcare.modelo.Medico;
import br.senai.healthcare.modelo.PerfilUsuario;
import br.senai.healthcare.modelo.Recepcionista;
import br.senai.healthcare.modelo.Usuario;
import br.senai.healthcare.repositorio.MedicoRepositorio;
import br.senai.healthcare.repositorio.UsuarioRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * cadastro de funcionarios do sistema (usuarios com login).
 *
 * o administrador gerencia todos os perfis, inclusive outros administradores,
 * na mesma tela e na mesma tabela. o admin nao e um usuario especial
 * gravado fora da lista: e so um registro com perfil administrador.
 *
 * pacientes continuam numa tabela a parte e nao tem login.
 * medico pode nascer com o usuario junto, ou receber o login depois
 * (quando ja existia so o cadastro profissional, sem acesso ao sistema).
 */
@Service
public class UsuarioServico {

    private final UsuarioRepositorio usuarios;
    private final MedicoRepositorio medicos;
    private final AuthServico auth;

    public UsuarioServico(UsuarioRepositorio usuarios, MedicoRepositorio medicos, AuthServico auth) {
        this.usuarios = usuarios;
        this.medicos = medicos;
        this.auth = auth;
    }

    /** lista so quem tem login (os funcionarios). medico sem acesso nao aparece aqui. */
    public List<Usuario> listar() {
        return usuarios.findByLoginIsNotNullOrderByNomeAsc();
    }

    /** busca um funcionario pelo id. medico sem login nao conta como usuario do sistema. */
    public Usuario buscar(Long id) {
        Usuario u = usuarios.findById(id).orElseThrow(() -> ErroApi.naoEncontrado("Usuário"));
        if (u.getLogin() == null || u.getLogin().isBlank()) {
            throw ErroApi.naoEncontrado("Usuário");
        }
        return u;
    }

    /** cadastra um funcionario. se o perfil for medico, cria o medico ou vincula um ja existente. */
    public Usuario cadastrar(Map<String, Object> dados) {
        String nome = CadastroServico.limpar(CadastroServico.texto(dados.get("nome")));
        String login = CadastroServico.limpar(CadastroServico.texto(dados.get("login"))).toLowerCase();
        String senha = CadastroServico.texto(dados.get("senha"));
        PerfilUsuario perfil = lerPerfil(dados.get("perfil"));
        Long medicoId = lerId(dados.get("medicoId"));

        ErroApi.Validador v = new ErroApi.Validador()
                .exigir(nome.length() >= 3 || (medicoId != null && nome.isEmpty()), "nome", "Informe o nome completo.")
                .exigir(!login.isEmpty(), "login", "Informe o usuário de acesso.")
                .exigir(ErroApi.preenchido(senha), "senha", "Informe a senha.")
                .exigir(!ErroApi.preenchido(senha) || senha.length() >= 6, "senha", "A senha deve ter pelo menos 6 caracteres.")
                .exigir(perfil != null, "perfil", "Selecione o perfil.");

        String crm = CadastroServico.limpar(CadastroServico.texto(dados.get("crm"))).toUpperCase();
        String especialidade = CadastroServico.limpar(CadastroServico.texto(dados.get("especialidade")));
        if (perfil == PerfilUsuario.MEDICO && medicoId == null) {
            v.exigir(!crm.isEmpty(), "crm", "Informe o CRM.")
                    .exigir(!especialidade.isEmpty(), "especialidade", "Informe a especialidade.");
        }
        v.lancarSeTiverErro();

        if (usuarios.existsByLoginIgnoreCase(login)) {
            throw new ErroApi(HttpStatus.CONFLICT, "Este usuário já existe.", Map.of("login", "Usuário já existe."));
        }

        if (perfil == PerfilUsuario.MEDICO && medicoId != null) {
            return vincularMedico(medicoId, nome, login, senha);
        }
        if (perfil == PerfilUsuario.MEDICO) {
            if (medicos.existsByCrmIgnoreCase(crm)) {
                throw new ErroApi(HttpStatus.CONFLICT, "Este CRM já está cadastrado.", Map.of("crm", "CRM já cadastrado."));
            }
            boolean disponivel = dados.get("disponivel") == null
                    || Boolean.parseBoolean(String.valueOf(dados.get("disponivel")));
            return medicos.save(new Medico(nome, login, senha, crm, especialidade, disponivel));
        }

        Usuario novo;
        if (perfil == PerfilUsuario.ADMINISTRADOR) {
            String nivel = CadastroServico.limpar(CadastroServico.texto(dados.get("nivelAcesso")));
            novo = new Administrador(nome, login, senha, nivel.isEmpty() ? "TOTAL" : nivel);
        } else if (perfil == PerfilUsuario.RECEPCIONISTA) {
            novo = new Recepcionista(nome, login, senha, CadastroServico.limpar(CadastroServico.texto(dados.get("matricula"))));
        } else if (perfil == PerfilUsuario.ENFERMEIRO) {
            novo = new Enfermeiro(nome, login, senha, CadastroServico.limpar(CadastroServico.texto(dados.get("coren"))));
        } else {
            throw new ErroApi(HttpStatus.BAD_REQUEST, "Perfil inválido.", Map.of("perfil", "Selecione o perfil."));
        }
        return usuarios.save(novo);
    }

    /** altera nome, login e os dados do perfil. o perfil em si nao muda depois do cadastro. */
    public Usuario editar(Long id, Map<String, Object> dados) {
        Usuario u = buscar(id);
        String nome = CadastroServico.limpar(CadastroServico.texto(dados.get("nome")));
        String login = CadastroServico.limpar(CadastroServico.texto(dados.get("login"))).toLowerCase();

        new ErroApi.Validador()
                .exigir(nome.length() >= 3, "nome", "Informe o nome completo.")
                .exigir(!login.isEmpty(), "login", "Informe o usuário de acesso.")
                .lancarSeTiverErro();

        if (usuarios.existsByLoginIgnoreCaseAndIdNot(login, id)) {
            throw new ErroApi(HttpStatus.CONFLICT, "Este usuário já existe.", Map.of("login", "Usuário já existe."));
        }

        if (u instanceof Medico m) {
            String crm = CadastroServico.limpar(CadastroServico.texto(dados.get("crm"))).toUpperCase();
            String especialidade = CadastroServico.limpar(CadastroServico.texto(dados.get("especialidade")));
            new ErroApi.Validador()
                    .exigir(!crm.isEmpty(), "crm", "Informe o CRM.")
                    .exigir(!especialidade.isEmpty(), "especialidade", "Informe a especialidade.")
                    .lancarSeTiverErro();
            if (medicos.existsByCrmIgnoreCaseAndIdNot(crm, id)) {
                throw new ErroApi(HttpStatus.CONFLICT, "Este CRM já está cadastrado.", Map.of("crm", "CRM já cadastrado."));
            }
            m.setCrm(crm);
            m.setEspecialidade(especialidade);
        } else if (u instanceof Administrador a) {
            String nivel = CadastroServico.limpar(CadastroServico.texto(dados.get("nivelAcesso")));
            if (!nivel.isEmpty()) a.setNivelAcesso(nivel);
        } else if (u instanceof Recepcionista r) {
            r.setMatricula(CadastroServico.limpar(CadastroServico.texto(dados.get("matricula"))));
        } else if (u instanceof Enfermeiro e) {
            e.setCoren(CadastroServico.limpar(CadastroServico.texto(dados.get("coren"))));
        }

        u.setNome(nome);
        u.setLogin(login);
        return usuarios.save(u);
    }

    /**
     * ativa ou desativa o acesso de um funcionario.
     * nao deixa o admin desativar a si mesmo nem o ultimo administrador ativo.
     */
    public Usuario alterarAtivo(Long id, boolean ativo, Usuario quemPediu) {
        Usuario alvo = buscar(id);
        if (!ativo) {
            conferirDesativacao(alvo, quemPediu, usuarios.countByPerfilAndAtivoTrue(PerfilUsuario.ADMINISTRADOR));
        }
        alvo.setAtivo(ativo);
        usuarios.save(alvo);
        if (!ativo) auth.encerrarSessoesDo(id);
        return alvo;
    }

    /** troca a senha de um funcionario (grava so o hash bcrypt). */
    public void redefinirSenha(Long id, String senha) {
        new ErroApi.Validador()
                .exigir(ErroApi.preenchido(senha), "senha", "Informe a nova senha.")
                .exigir(!ErroApi.preenchido(senha) || senha.length() >= 6, "senha", "A senha deve ter pelo menos 6 caracteres.")
                .lancarSeTiverErro();
        Usuario u = buscar(id);
        u.definirSenha(senha);
        usuarios.save(u);
    }

    /**
     * regras de desativacao, extraidas para os testes unitarios.
     * nao precisa de banco: recebe o alvo, quem pediu e quantos admins ativos existem.
     */
    public static void conferirDesativacao(Usuario alvo, Usuario quemPediu, long adminsAtivos) {
        if (alvo.getId() != null && alvo.getId().equals(quemPediu.getId())) {
            throw new ErroApi(HttpStatus.CONFLICT, "Você não pode desativar o próprio usuário.");
        }
        if (alvo.getPerfil() == PerfilUsuario.ADMINISTRADOR && alvo.isAtivo() && adminsAtivos <= 1) {
            throw new ErroApi(HttpStatus.CONFLICT, "Não é possível desativar o último administrador ativo.");
        }
    }

    // liga um login a um medico que ja estava cadastrado sem acesso ao sistema
    private Medico vincularMedico(Long medicoId, String nome, String login, String senha) {
        Medico m = medicos.findById(medicoId).orElseThrow(() -> ErroApi.naoEncontrado("Médico"));
        if (m.isTemAcesso()) {
            throw new ErroApi(HttpStatus.CONFLICT, "Este médico já tem um usuário de acesso.",
                    Map.of("medicoId", "Médico já possui acesso."));
        }
        if (!nome.isEmpty()) m.setNome(nome);
        m.setLogin(login);
        m.definirSenha(senha);
        m.setAtivo(true);
        return medicos.save(m);
    }

    private static PerfilUsuario lerPerfil(Object o) {
        if (o == null || !ErroApi.preenchido(String.valueOf(o))) return null;
        try {
            return PerfilUsuario.valueOf(String.valueOf(o).trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Long lerId(Object o) {
        if (o == null || "".equals(String.valueOf(o)) || "null".equalsIgnoreCase(String.valueOf(o))) return null;
        try {
            return Long.valueOf(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
