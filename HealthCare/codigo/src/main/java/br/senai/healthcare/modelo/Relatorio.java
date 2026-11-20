package br.senai.healthcare.modelo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * relatorio de atendimentos de um periodo (rf08 e hu05).
 * nao e guardado no repositorio: e montado na hora, sempre com os dados atuais.
 */
public class Relatorio {

    private String periodo;
    private LocalDate inicio;
    private LocalDate fim;
    private int totalAtendimentos;    // consultas finalizadas no periodo
    private int totalTriagens;        // fichas de triagem feitas no periodo
    private int totalPacientes;       // pacientes diferentes que passaram pela triagem
    private int novosPacientes;       // pacientes cadastrados no periodo
    private int cancelados;
    private LocalDateTime dataGeracao = LocalDateTime.now();
    private String geradoPor;
    private List<LinhaGravidade> porGravidade = new ArrayList<>();

    /** uma linha da tabela "por gravidade": quantidade e tempo medio real de espera. */
    public record LinhaGravidade(NivelGravidade gravidade, String descricao, int triagens,
                                 int atendimentos, Integer tempoMedioEsperaMin, int tempoMaximoReferenciaMin) { }

    public String getPeriodo() { return periodo; }
    public void setPeriodo(String periodo) { this.periodo = periodo; }
    public LocalDate getInicio() { return inicio; }
    public void setInicio(LocalDate inicio) { this.inicio = inicio; }
    public LocalDate getFim() { return fim; }
    public void setFim(LocalDate fim) { this.fim = fim; }
    public int getTotalAtendimentos() { return totalAtendimentos; }
    public void setTotalAtendimentos(int v) { this.totalAtendimentos = v; }
    public int getTotalTriagens() { return totalTriagens; }
    public void setTotalTriagens(int v) { this.totalTriagens = v; }
    public int getTotalPacientes() { return totalPacientes; }
    public void setTotalPacientes(int v) { this.totalPacientes = v; }
    public int getNovosPacientes() { return novosPacientes; }
    public void setNovosPacientes(int v) { this.novosPacientes = v; }
    public int getCancelados() { return cancelados; }
    public void setCancelados(int v) { this.cancelados = v; }
    public LocalDateTime getDataGeracao() { return dataGeracao; }
    public String getGeradoPor() { return geradoPor; }
    public void setGeradoPor(String geradoPor) { this.geradoPor = geradoPor; }
    public List<LinhaGravidade> getPorGravidade() { return porGravidade; }
}
