package br.senai.healthcare.repositorio;

import br.senai.healthcare.modelo.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/** acesso a tabela paciente. */
public interface PacienteRepositorio extends JpaRepository<Paciente, Long> {

    boolean existsByCpf(String cpf);

    /** busca por parte do nome (sem diferenciar maiuscula) ou parte do cpf. */
    List<Paciente> findByNomeContainingIgnoreCaseOrCpfContainingOrderByNomeAsc(String nome, String cpf);

    List<Paciente> findAllByOrderByNomeAsc();

    long countByCadastradoEmBetween(LocalDateTime de, LocalDateTime ate);
}
