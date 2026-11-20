package br.senai.healthcare.controle;

import br.senai.healthcare.config.Perfis;
import br.senai.healthcare.config.SegurancaInterceptor;
import br.senai.healthcare.modelo.FilaAtendimento;
import br.senai.healthcare.modelo.HistoricoMedico;
import br.senai.healthcare.modelo.Medico;
import br.senai.healthcare.modelo.Usuario;
import br.senai.healthcare.servico.FilaServico;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static br.senai.healthcare.modelo.PerfilUsuario.ADMINISTRADOR;
import static br.senai.healthcare.modelo.PerfilUsuario.ENFERMEIRO;
import static br.senai.healthcare.modelo.PerfilUsuario.MEDICO;
import static br.senai.healthcare.modelo.PerfilUsuario.RECEPCIONISTA;

/** endpoints de triagem (rf03), fila (rf04) e atendimento medico (rf05). */
@RestController
@RequestMapping("/api")
public class FilaController {

    private final FilaServico fila;

    public FilaController(FilaServico fila) {
        this.fila = fila;
    }

    /** post /api/triagens  enfermeiro registra a ficha e o paciente entra na fila. */
    @PostMapping("/triagens")
    @ResponseStatus(HttpStatus.CREATED)
    @Perfis({ENFERMEIRO, ADMINISTRADOR})
    public FilaAtendimento registrarTriagem(@RequestBody Map<String, Object> dados,
                                            @RequestAttribute(SegurancaInterceptor.USUARIO) Usuario u) {
        return fila.registrarTriagem(dados, u);
    }

    /**
     * get /api/fila  fila atual ja ordenada, com posicao e tempo estimado,
     * mais um resumo (medicos livres e tempo medio por gravidade).
     */
    @GetMapping("/fila")
    public Map<String, Object> filaAtual() {
        return Map.of(
                "itens", fila.filaAtiva(),
                "temposPorGravidade", fila.temposPorGravidade(),
                "medicosDisponiveis", fila.medicosDisponiveis(),
                "tempoMedioConsultaMin", fila.getTempoMedioConsultaMin());
    }

    /** post /api/fila/chamar-proximo  o medico chama o paciente mais prioritario. */
    @PostMapping("/fila/chamar-proximo")
    @Perfis({MEDICO})
    public FilaAtendimento chamarProximo(@RequestAttribute(SegurancaInterceptor.USUARIO) Usuario u) {
        return fila.chamarProximo((Medico) u);
    }

    /** post /api/fila/{id}/chamar  o medico chama um paciente especifico. */
    @PostMapping("/fila/{id}/chamar")
    @Perfis({MEDICO})
    public FilaAtendimento chamar(@PathVariable Long id, @RequestAttribute(SegurancaInterceptor.USUARIO) Usuario u) {
        return fila.chamar(id, (Medico) u);
    }

    /** get /api/fila/{id}  detalhes de um atendimento (usado na tela de consulta). */
    @GetMapping("/fila/{id}")
    public FilaAtendimento detalhe(@PathVariable Long id) {
        return fila.buscar(id);
    }

    /** post /api/fila/{id}/finalizar  corpo: diagnostico, observacoes, prescricao. grava no historico. */
    @PostMapping("/fila/{id}/finalizar")
    @Perfis({MEDICO})
    public HistoricoMedico finalizar(@PathVariable Long id, @RequestBody Map<String, String> dados,
                                     @RequestAttribute(SegurancaInterceptor.USUARIO) Usuario u) {
        return fila.finalizar(id, dados, (Medico) u);
    }

    /** post /api/fila/{id}/cancelar  tira o paciente da fila sem consulta. */
    @PostMapping("/fila/{id}/cancelar")
    @Perfis({MEDICO, ENFERMEIRO, RECEPCIONISTA, ADMINISTRADOR})
    public FilaAtendimento cancelar(@PathVariable Long id) {
        return fila.cancelar(id);
    }
}
