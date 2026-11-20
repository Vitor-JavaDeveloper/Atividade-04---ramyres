package br.senai.healthcare.controle;

import br.senai.healthcare.config.Perfis;
import br.senai.healthcare.config.SegurancaInterceptor;
import br.senai.healthcare.modelo.Usuario;
import br.senai.healthcare.servico.UsuarioServico;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static br.senai.healthcare.modelo.PerfilUsuario.ADMINISTRADOR;

/**
 * endpoints de cadastro de funcionarios, so para o administrador.
 * o administrador e um usuario comum nesta mesma lista (perfil administrador).
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioServico usuarios;

    public UsuarioController(UsuarioServico usuarios) {
        this.usuarios = usuarios;
    }

    /** get /api/usuarios  lista os funcionarios (quem tem login). */
    @GetMapping
    @Perfis({ADMINISTRADOR})
    public List<Usuario> listar() {
        return usuarios.listar();
    }

    @GetMapping("/{id}")
    @Perfis({ADMINISTRADOR})
    public Usuario buscar(@PathVariable Long id) {
        return usuarios.buscar(id);
    }

    /** post /api/usuarios  cadastra um funcionario (e o medico, se for o caso). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Perfis({ADMINISTRADOR})
    public Usuario cadastrar(@RequestBody Map<String, Object> dados) {
        return usuarios.cadastrar(dados);
    }

    /** put /api/usuarios/{id}  altera nome, login e dados do perfil (o perfil nao muda). */
    @PutMapping("/{id}")
    @Perfis({ADMINISTRADOR})
    public Usuario editar(@PathVariable Long id, @RequestBody Map<String, Object> dados) {
        return usuarios.editar(id, dados);
    }

    /** patch /api/usuarios/{id}/ativo  corpo: {"ativo": false}  desativa ou reativa o acesso. */
    @PatchMapping("/{id}/ativo")
    @Perfis({ADMINISTRADOR})
    public Usuario alterarAtivo(@PathVariable Long id, @RequestBody Map<String, Boolean> dados,
                                @RequestAttribute(SegurancaInterceptor.USUARIO) Usuario quem) {
        return usuarios.alterarAtivo(id, Boolean.TRUE.equals(dados.get("ativo")), quem);
    }

    /** post /api/usuarios/{id}/senha  corpo: {"senha": "..."}  redefine a senha (bcrypt). */
    @PostMapping("/{id}/senha")
    @Perfis({ADMINISTRADOR})
    public Map<String, String> redefinirSenha(@PathVariable Long id, @RequestBody Map<String, String> dados) {
        usuarios.redefinirSenha(id, dados.get("senha"));
        return Map.of("mensagem", "Senha redefinida.");
    }
}
