package br.senai.healthcare.modelo;

import jakarta.persistence.*;


import java.time.LocalDateTime;

/**
 * ficha de triagem (rf03).
 * guarda os sintomas, a gravidade escolhida pelo enfermeiro e fica ligada
 * ao paciente, ao enfermeiro que fez e ao medico responsavel (rnf04).
 */
@Entity
@Table(name = "ficha_triagem")
public class FichaTriagem extends Entidade {

    private LocalDateTime dataHora = LocalDateTime.now();
    private String sintomas;
    @Enumerated(EnumType.STRING)
    private NivelGravidade gravidade;
    private int tempoMaximoEsperaMin;
    @ManyToOne(optional = false) // muitas fichas para um paciente
    private Paciente paciente;
    @ManyToOne
    private Enfermeiro enfermeiro;
    @ManyToOne
    private Medico medico; // medico responsavel; pode ser definido na triagem ou quando ele chama o paciente

    /**
     * metodo classificargravidade do diagrama.
     * quem decide a gravidade e o enfermeiro (classificar sozinho pelos sintomas
     * esta fora do escopo do tap). aqui so gravamos o nivel escolhido e
     * puxamos o tempo maximo de espera de referencia dele (rd03).
     */
    public NivelGravidade classificarGravidade(NivelGravidade nivelEscolhido) {
        this.gravidade = nivelEscolhido;
        this.tempoMaximoEsperaMin = nivelEscolhido.getTempoMaximoEsperaMin();
        return nivelEscolhido;
    }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public String getSintomas() { return sintomas; }
    public void setSintomas(String sintomas) { this.sintomas = sintomas; }
    public NivelGravidade getGravidade() { return gravidade; }
    public int getTempoMaximoEsperaMin() { return tempoMaximoEsperaMin; }
    public Paciente getPaciente() { return paciente; }
    public void setPaciente(Paciente paciente) { this.paciente = paciente; }
    public Enfermeiro getEnfermeiro() { return enfermeiro; }
    public void setEnfermeiro(Enfermeiro enfermeiro) { this.enfermeiro = enfermeiro; }
    public Medico getMedico() { return medico; }
    public void setMedico(Medico medico) { this.medico = medico; }
}
