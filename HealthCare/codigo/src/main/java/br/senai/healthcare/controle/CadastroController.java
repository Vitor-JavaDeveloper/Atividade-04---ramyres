package br.senai.healthcare.controle;

import br.senai.healthcare.config.Perfis;
import br.senai.healthcare.config.SegurancaInterceptor;
import br.senai.healthcare.modelo.Medico;
import br.senai.healthcare.modelo.Paciente;
import br.senai.healthcare.modelo.Usuario;
import br.senai.healthcare.servico.CadastroServico;
import br.senai.healthcare.servico.FilaServico;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static br.senai.healthcare.modelo.PerfilUsuario.ADMINISTRADOR;
import static br.senai.healthcare.modelo.PerfilUsuario.MEDICO;
import static br.senai.healthcare.modelo.PerfilUsuario.RECEPCIONISTA;

/** endpoints de pacientes (rf01, rf05) e medicos (rf02, rf07). */
@RestController
@RequestMapping("/api")
public class CadastroController {

    private final CadastroServico cadastro;
    private final FilaServico filaServico;

    public CadastroController(CadastroServico cadastro, FilaServico filaServico) {
        this.cadastro = cadastro;
        this.filaServico = filaServico;
    }

    // ---------- pacientes ----------

    /** get /api/pacientes?busca=maria  lista (ou pesquisa) os pacientes. */
    @GetMapping("/pacientes")
    public List<Paciente> listarPacientes(@RequestParam(required = false) String busca) {
        return cadastro.listarPacientes(busca);
    }

    /** post /api/pacientes  cadastra um paciente (recepcao ou admin). */
    @PostMapping("/pacientes")
    @ResponseStatus(HttpStatus.CREATED)
    @Perfis({RECEPCIONISTA, ADMINISTRADOR})
    public Paciente cadastrarPaciente(@RequestBody Map<String, String> dados) {
        return cadastro.cadastrarPaciente(dados);
    }

    @GetMapping("/pacientes/{id}")
    public Paciente buscarPaciente(@PathVariable Long id) {
        return cadastro.buscarPaciente(id);
    }

    /** get /api/pacientes/{id}/historico  historico medico do paciente (hu04: medico; admin tambem). */
    @GetMapping("/pacientes/{id}/historico")
    @Perfis({MEDICO, ADMINISTRADOR})
    public Map<String, Object> historico(@PathVariable Long id) {
        return filaServico.historicoDoPaciente(id);
    }

    // ---------- medicos ----------

    /** get /api/medicos?disponiveis=true  lista os medicos, ou so os disponiveis (rf07). */
    @GetMapping("/medicos")
    public List<Medico> listarMedicos(@RequestParam(defaultValue = "false") boolean disponiveis) {
        return cadastro.listarMedicos(disponiveis);
    }

    /** post /api/medicos  cadastra um medico (recepcao ou admin). */
    @PostMapping("/medicos")
    @ResponseStatus(HttpStatus.CREATED)
    @Perfis({RECEPCIONISTA, ADMINISTRADOR})
    public Medico cadastrarMedico(@RequestBody Map<String, Object> dados) {
        return cadastro.cadastrarMedico(dados);
    }

    /** patch /api/medicos/{id}/disponibilidade  corpo: {"disponivel": true} */
    @PatchMapping("/medicos/{id}/disponibilidade")
    @Perfis({MEDICO, RECEPCIONISTA, ADMINISTRADOR})
    public Medico disponibilidade(@PathVariable Long id, @RequestBody Map<String, Boolean> dados,
                                  @RequestAttribute(SegurancaInterceptor.USUARIO) Usuario u) {
        return cadastro.atualizarDisponibilidade(id, Boolean.TRUE.equals(dados.get("disponivel")), u);
    }
}
