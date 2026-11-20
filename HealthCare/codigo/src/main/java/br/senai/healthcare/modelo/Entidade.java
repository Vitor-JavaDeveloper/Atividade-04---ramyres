package br.senai.healthcare.modelo;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * classe base de tudo que e gravado no banco.
 * a unica coisa em comum entre todas as entidades e o id (chave primaria).
 * identity quer dizer que quem gera o numero e o proprio postgresql (bigserial).
 */
@MappedSuperclass
public abstract class Entidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}
