package br.senai.healthcare.repositorio;

import br.senai.healthcare.modelo.FilaAtendimento;
import br.senai.healthcare.modelo.StatusFila;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/** acesso a tabela fila_atendimento. */
public interface FilaAtendimentoRepositorio extends JpaRepository<FilaAtendimento, Long> {

    List<FilaAtendimento> findByStatus(StatusFila status);

    boolean existsByStatusAndMedicoId(StatusFila status, Long medicoId);

    boolean existsByStatusInAndFichaPacienteId(List<StatusFila> status, Long pacienteId);

    /** atendimentos encerrados (finalizados ou cancelados) dentro de um periodo. */
    List<FilaAtendimento> findByStatusAndFimAtendimentoGreaterThanEqualAndFimAtendimentoLessThan(
            StatusFila status, LocalDateTime de, LocalDateTime ate);
}
