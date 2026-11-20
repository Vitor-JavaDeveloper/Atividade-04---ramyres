package br.senai.healthcare.modelo;

import jakarta.persistence.*;


/** enfermeiro (herda de usuario). faz a triagem dos pacientes (hu02). */
@Entity
@DiscriminatorValue("ENFERMEIRO")
public class Enfermeiro extends Usuario {

    private String coren;

    protected Enfermeiro() { } // exigido pelo jpa

    public Enfermeiro(String nome, String login, String senha, String coren) {
        super(nome, login, senha, PerfilUsuario.ENFERMEIRO);
        this.coren = coren;
    }

    public String getCoren() { return coren; }
    public void setCoren(String coren) { this.coren = coren; }
}
