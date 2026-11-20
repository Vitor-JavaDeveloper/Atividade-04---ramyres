package br.senai.healthcare.repositorio;

import br.senai.healthcare.modelo.FichaTriagem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/** acesso a tabela ficha_triagem. */
public interface FichaTriagemRepositorio extends JpaRepository<FichaTriagem, Long> {

    List<FichaTriagem> findByPacienteIdOrderByDataHoraDesc(Long pacienteId);

    /** fichas feitas dentro de um periodo (usado no relatorio). */
    List<FichaTriagem> findByDataHoraGreaterThanEqualAndDataHoraLessThan(LocalDateTime de, LocalDateTime ate);
}
