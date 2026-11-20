package br.senai.healthcare.modelo;

/**
 * niveis de gravidade da triagem (rd01), do mais grave para o menos grave.
 *
 * a ordem em que estao escritos aqui importa: o java numera cada valor
 * (emergencia = 0, muito_urgente = 1 ...) e a fila usa esse numero para
 * ordenar os pacientes (rd02). quanto menor o numero, mais na frente fica.
 *
 * cada nivel tem um tempo maximo de espera de referencia em minutos (rd03),
 * baseado no protocolo de manchester, que e o mais usado nos prontos socorros.
 */
public enum NivelGravidade {
    EMERGENCIA("Emergência", 0),
    MUITO_URGENTE("Muito Urgente", 10),
    URGENTE("Urgente", 60),
    POUCO_URGENTE("Pouco Urgente", 120),
    NAO_URGENTE("Não Urgente", 240);

    private final String descricao;
    private final int tempoMaximoEsperaMin;

    NivelGravidade(String descricao, int tempoMaximoEsperaMin) {
        this.descricao = descricao;
        this.tempoMaximoEsperaMin = tempoMaximoEsperaMin;
    }

    public String getDescricao() { return descricao; }
    public int getTempoMaximoEsperaMin() { return tempoMaximoEsperaMin; }
}
