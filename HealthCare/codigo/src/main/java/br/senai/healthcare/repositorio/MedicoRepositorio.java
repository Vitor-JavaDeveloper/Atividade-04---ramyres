package br.senai.healthcare.repositorio;

import br.senai.healthcare.modelo.Medico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** acesso aos medicos (mesma tabela usuario, mas so as linhas com tipo = medico). */
public interface MedicoRepositorio extends JpaRepository<Medico, Long> {

    List<Medico> findAllByOrderByNomeAsc();

    /** medicos disponiveis agora (rf07). */
    List<Medico> findByDisponivelTrueOrderByNomeAsc();

    long countByDisponivelTrue();

    boolean existsByCrmIgnoreCase(String crm);

    // na edicao, o crm pode continuar o mesmo do proprio medico
    boolean existsByCrmIgnoreCaseAndIdNot(String crm, Long id);
}
