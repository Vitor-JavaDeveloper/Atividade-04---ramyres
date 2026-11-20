package br.senai.healthcare.servico;

import br.senai.healthcare.modelo.Medico;
import br.senai.healthcare.modelo.Paciente;
import br.senai.healthcare.modelo.PerfilUsuario;
import br.senai.healthcare.modelo.Usuario;
import br.senai.healthcare.repositorio.MedicoRepositorio;
import br.senai.healthcare.repositorio.PacienteRepositorio;
import br.senai.healthcare.repositorio.UsuarioRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * regras de cadastro de pacientes (rf01) e medicos (rf02),
 * mais a consulta e atualizacao da disponibilidade dos medicos (rf07).
 */
@Service
public class CadastroServico {

    private final PacienteRepositorio pacientes;
    private final MedicoRepositorio medicos;
    private final UsuarioRepositorio usuarios;

    public CadastroServico(PacienteRepositorio pacientes, MedicoRepositorio medicos, UsuarioRepositorio usuarios) {
        this.pacientes = pacientes;
        this.medicos = medicos;
        this.usuarios = usuarios;
    }

    // ===================== pacientes =====================

    /**
     * cadastra um paciente.
     * criterios do hu01: valida o cpf, exige nome, cpf e data de nascimento
     * e informa qual campo esta errado.
     */
    public Paciente cadastrarPaciente(Map<String, String> dados) {
        String nome = limpar(dados.get("nome"));
        String cpf = somenteNumeros(dados.get("cpf"));
        LocalDate nascimento = lerData(dados.get("dataNascimento"));
        String telefone = limpar(dados.get("telefone"));

        new ErroApi.Validador()
                .exigir(nome.length() >= 3, "nome", "Informe o nome completo.")
                .exigir(!cpf.isEmpty(), "cpf", "Informe o CPF.")
                .exigir(cpf.isEmpty() || cpfValido(cpf), "cpf", "CPF inválido.")
                .exigir(nascimento != null, "dataNascimento", "Informe uma data de nascimento válida.")
                .exigir(nascimento == null || !nascimento.isAfter(LocalDate.now()), "dataNascimento", "A data de nascimento não pode ser no futuro.")
                .exigir(telefone.isEmpty() || somenteNumeros(telefone).length() >= 10, "telefone", "Telefone deve ter DDD e número.")
                .lancarSeTiverErro();

        // cpf tem que ser unico: nao deixa cadastrar a mesma pessoa duas vezes
        if (pacientes.existsByCpf(cpf)) {
            throw new ErroApi(HttpStatus.CONFLICT, "Este CPF já está cadastrado.", Map.of("cpf", "CPF já cadastrado."));
        }

        Paciente p = new Paciente();
        p.setNome(nome);
        p.setCpf(cpf);
        p.setDataNascimento(nascimento);
        p.setSexo(limpar(dados.get("sexo")));
        p.setTelefone(telefone);
        p.setEndereco(limpar(dados.get("endereco")));
        return pacientes.save(p); // insert na tabela paciente
    }

    /** lista pacientes em ordem alfabetica, filtrando por nome ou cpf se tiver busca. */
    public List<Paciente> listarPacientes(String busca) {
        String termo = limpar(busca);
        if (termo.isEmpty()) return pacientes.findAllByOrderByNomeAsc();
        String termoCpf = somenteNumeros(busca);
        // se a busca nao tem numero, usamos um texto impossivel para o cpf nao casar com tudo
        return pacientes.findByNomeContainingIgnoreCaseOrCpfContainingOrderByNomeAsc(termo, termoCpf.isEmpty() ? "x" : termoCpf);
    }

    public Paciente buscarPaciente(Long id) {
        return pacientes.findById(id).orElseThrow(() -> ErroApi.naoEncontrado("Paciente"));
    }

    // ===================== medicos =====================

    /** cadastra um medico (hu07). login e senha sao opcionais. */
    public Medico cadastrarMedico(Map<String, Object> dados) {
        String nome = limpar(texto(dados.get("nome")));
        String crm = limpar(texto(dados.get("crm"))).toUpperCase();
        String especialidade = limpar(texto(dados.get("especialidade")));
        String login = limpar(texto(dados.get("login"))).toLowerCase();
        String senha = texto(dados.get("senha"));
        Object disponivel = dados.get("disponivel");

        new ErroApi.Validador()
                .exigir(nome.length() >= 3, "nome", "Informe o nome completo.")
                .exigir(!crm.isEmpty(), "crm", "Informe o CRM.")
                .exigir(!especialidade.isEmpty(), "especialidade", "Informe a especialidade.")
                .exigir(disponivel != null && !"".equals(disponivel), "disponivel", "Selecione o status de disponibilidade.")
                .exigir(login.isEmpty() || ErroApi.preenchido(senha), "senha", "Informe a senha para o login.")
                .exigir(!ErroApi.preenchido(senha) || senha.length() >= 6, "senha", "A senha deve ter pelo menos 6 caracteres.")
                .exigir(ErroApi.preenchido(login) || !ErroApi.preenchido(senha), "login", "Informe o usuário para a senha.")
                .lancarSeTiverErro();

        if (medicos.existsByCrmIgnoreCase(crm)) {
            throw new ErroApi(HttpStatus.CONFLICT, "Este CRM já está cadastrado.", Map.of("crm", "CRM já cadastrado."));
        }
        if (!login.isEmpty() && usuarios.existsByLoginIgnoreCase(login)) {
            throw new ErroApi(HttpStatus.CONFLICT, "Este usuário já existe.", Map.of("login", "Usuário já existe."));
        }

        boolean disp = Boolean.parseBoolean(String.valueOf(disponivel));
        Medico m = new Medico(nome, login.isEmpty() ? null : login, senha, crm, especialidade, disp);
        return medicos.save(m);
    }

    /** lista os medicos. se somentedisponiveis for true, mostra so quem esta livre agora (rf07). */
    public List<Medico> listarMedicos(boolean somenteDisponiveis) {
        return somenteDisponiveis ? medicos.findByDisponivelTrueOrderByNomeAsc() : medicos.findAllByOrderByNomeAsc();
    }

    public Medico buscarMedico(Long id) {
        return medicos.findById(id).orElseThrow(() -> ErroApi.naoEncontrado("Médico"));
    }

    /**
     * muda a disponibilidade de um medico.
     * um medico logado so pode mudar a propria; recepcao e admin podem mudar de qualquer um.
     */
    public Medico atualizarDisponibilidade(Long medicoId, boolean disponivel, Usuario quemPediu) {
        Medico m = buscarMedico(medicoId);
        if (quemPediu.getPerfil() == PerfilUsuario.MEDICO && !quemPediu.getId().equals(m.getId())) {
            throw new ErroApi(HttpStatus.FORBIDDEN, "Você só pode alterar a sua própria disponibilidade.");
        }
        m.atualizarDisponibilidade(disponivel);
        return medicos.save(m);
    }

    // ===================== ajudantes =====================

    /**
     * valida o cpf pelos digitos verificadores (regra oficial da receita).
     * os dois ultimos numeros sao calculados a partir dos 9 primeiros;
     * se o calculo nao bater, o cpf foi digitado errado.
     */
    public static boolean cpfValido(String cpf) {
        if (cpf == null || !cpf.matches("\\d{11}") || cpf.chars().distinct().count() == 1) return false;
        for (int tamanho = 9; tamanho <= 10; tamanho++) {
            int soma = 0;
            for (int i = 0; i < tamanho; i++) {
                soma += (cpf.charAt(i) - '0') * (tamanho + 1 - i);
            }
            int digito = (soma * 10) % 11;
            if (digito == 10) digito = 0;
            if (digito != cpf.charAt(tamanho) - '0') return false;
        }
        return true;
    }

    static String somenteNumeros(String s) { return s == null ? "" : s.replaceAll("\\D", ""); }
    static String limpar(String s) { return s == null ? "" : s.trim(); }
    static String texto(Object o) { return o == null ? null : String.valueOf(o); }

    /** converte "aaaa-mm-dd" (formato que o campo de data do navegador manda) em localdate. */
    static LocalDate lerData(String s) {
        try {
            return ErroApi.preenchido(s) ? LocalDate.parse(s.trim()) : null;
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
