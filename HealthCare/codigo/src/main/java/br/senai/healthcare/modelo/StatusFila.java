package br.senai.healthcare.modelo;

/**
 * situacoes possiveis de um paciente na fila (enumeration statusfila do diagrama).
 * aguardando: esperando ser chamado.
 * em_atendimento: o medico ja chamou e esta atendendo.
 * finalizado: consulta concluida, virou historico medico.
 * cancelado: saiu da fila sem consulta (desistiu, foi transferido etc).
 */
public enum StatusFila {
    AGUARDANDO, EM_ATENDIMENTO, FINALIZADO, CANCELADO
}
