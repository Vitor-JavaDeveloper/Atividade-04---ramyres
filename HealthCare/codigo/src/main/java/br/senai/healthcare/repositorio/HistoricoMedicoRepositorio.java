package br.senai.healthcare.repositorio;

import br.senai.healthcare.modelo.HistoricoMedico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** acesso a tabela historico_medico. */
public interface HistoricoMedicoRepositorio extends JpaRepository<HistoricoMedico, Long> {

    /** consultas anteriores do paciente, da mais recente para a mais antiga (hu04). */
    List<HistoricoMedico> findByPacienteIdOrderByDataConsultaDesc(Long pacienteId);
}
