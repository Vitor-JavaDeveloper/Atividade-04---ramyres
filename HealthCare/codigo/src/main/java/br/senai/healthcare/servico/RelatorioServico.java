package br.senai.healthcare.servico;

import br.senai.healthcare.modelo.FichaTriagem;
import br.senai.healthcare.modelo.FilaAtendimento;
import br.senai.healthcare.modelo.NivelGravidade;
import br.senai.healthcare.modelo.Relatorio;
import br.senai.healthcare.modelo.StatusFila;
import br.senai.healthcare.modelo.Usuario;
import br.senai.healthcare.repositorio.FichaTriagemRepositorio;
import br.senai.healthcare.repositorio.FilaAtendimentoRepositorio;
import br.senai.healthcare.repositorio.PacienteRepositorio;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * relatorios de atendimento (rf08 e hu05), usados pelo administrador.
 * tambem calcula a media de tempo de espera por gravidade (escopo do tap:
 * "calcular media de tempo para o atendimento em cada gravidade").
 */
@Service
public class RelatorioServico {

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final FichaTriagemRepositorio fichas;
    private final FilaAtendimentoRepositorio fila;
    private final PacienteRepositorio pacientes;

    public RelatorioServico(FichaTriagemRepositorio fichas, FilaAtendimentoRepositorio fila, PacienteRepositorio pacientes) {
        this.fichas = fichas;
        this.fila = fila;
        this.pacientes = pacientes;
    }

    /** metodo gerarrelatorio(periodo) do diagrama. o periodo vai de "inicio" ate "fim", incluindo os dois dias. */
    public Relatorio gerarRelatorio(LocalDate inicio, LocalDate fim, Usuario quemGerou) {
        if (inicio == null) inicio = LocalDate.now().minusDays(30); // padrao: ultimos 30 dias
        if (fim == null) fim = LocalDate.now();
        if (fim.isBefore(inicio)) {
            throw new ErroApi(HttpStatus.BAD_REQUEST, "A data final não pode ser antes da inicial.");
        }
        LocalDateTime de = inicio.atStartOfDay();
        LocalDateTime ate = fim.plusDays(1).atStartOfDay(); // ate o fim do ultimo dia

        // triagens feitas no periodo
        List<FichaTriagem> triagens = fichas.findByDataHoraGreaterThanEqualAndDataHoraLessThan(de, ate);
        // atendimentos finalizados no periodo
        List<FilaAtendimento> finalizados = fila.findByStatusAndFimAtendimentoGreaterThanEqualAndFimAtendimentoLessThan(StatusFila.FINALIZADO, de, ate);

        Relatorio r = new Relatorio();
        r.setInicio(inicio);
        r.setFim(fim);
        r.setPeriodo(inicio.format(BR) + " a " + fim.format(BR));
        r.setGeradoPor(quemGerou == null ? "" : quemGerou.getNome());
        r.setTotalTriagens(triagens.size());
        r.setTotalAtendimentos(finalizados.size());
        r.setTotalPacientes((int) triagens.stream().map(t -> t.getPaciente().getId()).distinct().count());
        r.setNovosPacientes((int) pacientes.countByCadastradoEmBetween(de, ate));
        r.setCancelados(fila.findByStatusAndFimAtendimentoGreaterThanEqualAndFimAtendimentoLessThan(StatusFila.CANCELADO, de, ate).size());

        // uma linha por gravidade, com a media real de espera (da chegada ate o medico chamar)
        for (NivelGravidade g : NivelGravidade.values()) {
            List<FilaAtendimento> atendidosG = finalizados.stream().filter(f -> f.getFicha().getGravidade() == g).toList();
            r.getPorGravidade().add(new Relatorio.LinhaGravidade(
                    g, g.getDescricao(),
                    (int) triagens.stream().filter(t -> t.getGravidade() == g).count(),
                    atendidosG.size(),
                    FilaServico.media(atendidosG.stream().map(FilaAtendimento::getMinutosEsperando).toList()),
                    g.getTempoMaximoEsperaMin()));
        }
        return r;
    }

    /** metodo exportarpdf() do diagrama: monta um pdf simples com os mesmos numeros da tela. */
    public byte[] exportarPDF(Relatorio r) {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        Document doc = new Document();
        PdfWriter.getInstance(doc, saida);
        doc.open();
        Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font negrito = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);

        doc.add(new Paragraph("HealthCare - Relatório de Atendimentos", titulo));
        doc.add(new Paragraph("Período: " + r.getPeriodo(), normal));
        doc.add(new Paragraph("Gerado em " + r.getDataGeracao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                + " por " + r.getGeradoPor(), normal));
        doc.add(new Paragraph(" "));

        // tabela de totais
        PdfPTable totais = new PdfPTable(2);
        totais.setWidthPercentage(60);
        totais.setHorizontalAlignment(PdfPTable.ALIGN_LEFT);
        String[][] linhas = {
                {"Atendimentos realizados", String.valueOf(r.getTotalAtendimentos())},
                {"Triagens registradas", String.valueOf(r.getTotalTriagens())},
                {"Pacientes atendidos (diferentes)", String.valueOf(r.getTotalPacientes())},
                {"Pacientes novos cadastrados", String.valueOf(r.getNovosPacientes())},
                {"Atendimentos cancelados", String.valueOf(r.getCancelados())}};
        for (String[] l : linhas) {
            totais.addCell(new Paragraph(l[0], normal));
            totais.addCell(new Paragraph(l[1], negrito));
        }
        doc.add(totais);
        doc.add(new Paragraph(" "));

        // tabela por gravidade
        doc.add(new Paragraph("Por nível de gravidade", negrito));
        doc.add(new Paragraph(" "));
        PdfPTable tabela = new PdfPTable(new float[]{3, 2, 2, 3, 3});
        tabela.setWidthPercentage(100);
        for (String cab : new String[]{"Gravidade", "Triagens", "Atendimentos", "Espera média (min)", "Tempo máx. ref. (min)"}) {
            tabela.addCell(new Paragraph(cab, negrito));
        }
        for (Relatorio.LinhaGravidade l : r.getPorGravidade()) {
            tabela.addCell(new Paragraph(l.descricao(), normal));
            tabela.addCell(new Paragraph(String.valueOf(l.triagens()), normal));
            tabela.addCell(new Paragraph(String.valueOf(l.atendimentos()), normal));
            tabela.addCell(new Paragraph(l.tempoMedioEsperaMin() == null ? "-" : String.valueOf(l.tempoMedioEsperaMin()), normal));
            tabela.addCell(new Paragraph(String.valueOf(l.tempoMaximoReferenciaMin()), normal));
        }
        doc.add(tabela);
        doc.close();
        return saida.toByteArray();
    }
}
