package br.senai.healthcare.controle;

import br.senai.healthcare.config.Perfis;
import br.senai.healthcare.config.SegurancaInterceptor;
import br.senai.healthcare.modelo.Relatorio;
import br.senai.healthcare.modelo.Usuario;
import br.senai.healthcare.servico.RelatorioServico;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

import static br.senai.healthcare.modelo.PerfilUsuario.ADMINISTRADOR;

/** endpoints de relatorio (rf08), so para o administrador (hu05). */
@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

    private final RelatorioServico relatorios;

    public RelatorioController(RelatorioServico relatorios) {
        this.relatorios = relatorios;
    }

    /** get /api/relatorios?inicio=2026-11-01&fim=2026-11-30 */
    @GetMapping
    @Perfis({ADMINISTRADOR})
    public Relatorio gerar(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                           @RequestAttribute(SegurancaInterceptor.USUARIO) Usuario u) {
        return relatorios.gerarRelatorio(inicio, fim, u);
    }

    /** get /api/relatorios/pdf?inicio=...&fim=...  devolve o mesmo relatorio em pdf para baixar. */
    @GetMapping("/pdf")
    @Perfis({ADMINISTRADOR})
    public ResponseEntity<byte[]> pdf(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                                      @RequestAttribute(SegurancaInterceptor.USUARIO) Usuario u) {
        byte[] pdf = relatorios.exportarPDF(relatorios.gerarRelatorio(inicio, fim, u));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=relatorio-healthcare.pdf")
                .body(pdf);
    }
}
