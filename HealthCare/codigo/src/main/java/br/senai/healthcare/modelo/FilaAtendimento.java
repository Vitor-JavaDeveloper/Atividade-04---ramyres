package br.senai.healthcare.modelo;

import jakarta.persistence.*;


import java.time.Duration;
import java.time.LocalDateTime;

/**
 * uma entrada na fila de atendimento (rf04).
 * toda ficha de triagem salva gera uma entrada dessas automaticamente.
 * a posicao e o tempo estimado sao recalculados toda vez que a fila e consultada,
 * por isso a fila se reordena sozinha quando chega alguem mais grave.
 */
@Entity
@Table(name = "fila_atendimento")
public class FilaAtendimento extends Entidade {

    @OneToOne(optional = false) // cada ficha gera uma unica entrada na fila
    private FichaTriagem ficha;
    @ManyToOne
    private Medico medico;               // medico que chamou o paciente
    @Enumerated(EnumType.STRING)
    private StatusFila status = StatusFila.AGUARDANDO;
    // @transient: estes dois nao vao para o banco, sao calculados toda vez que a fila e lida
    @Transient
    private int posicao;
    @Transient
    private int tempoEsperaEstimado;     // em minutos
    private LocalDateTime entradaEm = LocalDateTime.now();
    private LocalDateTime inicioAtendimento;
    private LocalDateTime fimAtendimento;

    /** metodo calcularposicao do diagrama: a posicao e passada pelo servico que ordena a fila. */
    public int calcularPosicao(int posicaoNaFilaOrdenada) {
        this.posicao = posicaoNaFilaOrdenada;
        return posicao;
    }

    /**
     * metodo calculartempoespera do diagrama (rnf02 e rnf03).
     * conta quantos pacientes estao na frente, multiplica pelo tempo medio de uma consulta
     * e divide pelo numero de medicos disponiveis (no minimo 1 para nao dividir por zero).
     * emergencia sempre fica com 0, porque tem que ser atendida na hora.
     */
    public int calcularTempoEspera(int pacientesNaFrente, int tempoMedioConsultaMin, int medicosDisponiveis) {
        if (ficha.getGravidade() == NivelGravidade.EMERGENCIA) {
            tempoEsperaEstimado = 0;
        } else {
            tempoEsperaEstimado = (int) Math.ceil(pacientesNaFrente * tempoMedioConsultaMin / (double) Math.max(1, medicosDisponiveis));
        }
        return tempoEsperaEstimado;
    }

    /** metodo atualizarstatus do diagrama. marca tambem os horarios de inicio e fim. */
    public void atualizarStatus(StatusFila novo) {
        this.status = novo;
        if (novo == StatusFila.EM_ATENDIMENTO) inicioAtendimento = LocalDateTime.now();
        if (novo == StatusFila.FINALIZADO || novo == StatusFila.CANCELADO) fimAtendimento = LocalDateTime.now();
    }

    /** ha quantos minutos o paciente esta esperando (ou esperou ate ser chamado). */
    public long getMinutosEsperando() {
        LocalDateTime fim = inicioAtendimento != null ? inicioAtendimento : LocalDateTime.now();
        return Duration.between(entradaEm, fim).toMinutes();
    }

    /** verdadeiro se ja passou do tempo maximo de referencia da gravidade (rd03). */
    public boolean isAcimaDoTempoMaximo() {
        return status == StatusFila.AGUARDANDO && getMinutosEsperando() > ficha.getTempoMaximoEsperaMin();
    }

    public FichaTriagem getFicha() { return ficha; }
    public void setFicha(FichaTriagem ficha) { this.ficha = ficha; }
    public Medico getMedico() { return medico; }
    public void setMedico(Medico medico) { this.medico = medico; }
    public StatusFila getStatus() { return status; }
    public int getPosicao() { return posicao; }
    public int getTempoEsperaEstimado() { return tempoEsperaEstimado; }
    public LocalDateTime getEntradaEm() { return entradaEm; }
    public void setEntradaEm(LocalDateTime entradaEm) { this.entradaEm = entradaEm; }
    public LocalDateTime getInicioAtendimento() { return inicioAtendimento; }
    public void setInicioAtendimento(LocalDateTime inicioAtendimento) { this.inicioAtendimento = inicioAtendimento; }
    public LocalDateTime getFimAtendimento() { return fimAtendimento; }
    public void setFimAtendimento(LocalDateTime fimAtendimento) { this.fimAtendimento = fimAtendimento; }
}
