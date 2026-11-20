package br.senai.healthcare.repositorio;

import br.senai.healthcare.modelo.PerfilUsuario;
import br.senai.healthcare.modelo.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * acesso a tabela usuario.
 *
 * repare que e so uma interface: o spring data cria a implementacao sozinho.
 * metodos como save, findbyid, findall e count ja vem prontos do jparepository.
 * os que escrevemos aqui sao "consultas pelo nome": o spring le o nome do metodo
 * e monta o sql (findbyloginignorecase vira select ... where lower(login) = lower(?)).
 */
public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByLoginIgnoreCase(String login);

    boolean existsByLoginIgnoreCase(String login);

    // na edicao, o login pode continuar o mesmo do proprio usuario
    boolean existsByLoginIgnoreCaseAndIdNot(String login, Long id);

    // so quem tem login e funcionario do sistema (medico sem acesso nao entra nesta lista)
    List<Usuario> findByLoginIsNotNullOrderByNomeAsc();

    long countByPerfilAndAtivoTrue(PerfilUsuario perfil);
}
