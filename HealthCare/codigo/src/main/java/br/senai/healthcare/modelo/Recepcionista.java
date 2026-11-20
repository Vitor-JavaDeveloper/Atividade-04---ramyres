package br.senai.healthcare.modelo;

import jakarta.persistence.*;


/** recepcionista (herda de usuario). cadastra pacientes e medicos (hu01 e hu07). */
@Entity
@DiscriminatorValue("RECEPCIONISTA")
public class Recepcionista extends Usuario {

    private String matricula;

    protected Recepcionista() { } // exigido pelo jpa

    public Recepcionista(String nome, String login, String senha, String matricula) {
        super(nome, login, senha, PerfilUsuario.RECEPCIONISTA);
        this.matricula = matricula;
    }

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }
}
