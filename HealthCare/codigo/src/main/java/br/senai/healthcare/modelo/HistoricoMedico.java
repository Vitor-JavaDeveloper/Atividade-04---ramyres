package br.senai.healthcare.modelo;

import jakarta.persistence.*;


import java.time.LocalDateTime;

/**
 * registro de uma consulta que ja aconteceu (rf05).
 * e criado quando o medico finaliza o atendimento.
 * a tela de historico do paciente lista todos esses registros.
 */
@Entity
@Table(name = "historico_medico")
public class HistoricoMedico extends Entidade {

    private LocalDateTime dataConsulta = LocalDateTime.now();
    private String diagnostico;
    private String observacoes;
    private String prescricao;
    @ManyToOne(optional = false)
    private Paciente paciente;
    @ManyToOne
    private Medico medico;
    @ManyToOne
    private FichaTriagem ficha; // triagem que originou a consulta (mostra gravidade e sintomas)

    /** metodo registraratendimento do diagrama. */
    public void registrarAtendimento(String diagnostico, String observacoes, String prescricao) {
        this.diagnostico = diagnostico;
        this.observacoes = observacoes;
        this.prescricao = prescricao;
    }

    public LocalDateTime getDataConsulta() { return dataConsulta; }
    public void setDataConsulta(LocalDateTime dataConsulta) { this.dataConsulta = dataConsulta; }
    public String getDiagnostico() { return diagnostico; }
    public String getObservacoes() { return observacoes; }
    public String getPrescricao() { return prescricao; }
    public Paciente getPaciente() { return paciente; }
    public void setPaciente(Paciente paciente) { this.paciente = paciente; }
    public Medico getMedico() { return medico; }
    public void setMedico(Medico medico) { this.medico = medico; }
    public FichaTriagem getFicha() { return ficha; }
    public void setFicha(FichaTriagem ficha) { this.ficha = ficha; }
}
