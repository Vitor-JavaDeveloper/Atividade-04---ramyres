package br.senai.healthcare.modelo;

import jakarta.persistence.*;


/** administrador (herda de usuario). e quem gera os relatorios (hu05). */
@Entity
@DiscriminatorValue("ADMINISTRADOR")
public class Administrador extends Usuario {

    private String nivelAcesso;

    protected Administrador() { } // exigido pelo jpa

    public Administrador(String nome, String login, String senha, String nivelAcesso) {
        super(nome, login, senha, PerfilUsuario.ADMINISTRADOR);
        this.nivelAcesso = nivelAcesso;
    }

    public String getNivelAcesso() { return nivelAcesso; }
    public void setNivelAcesso(String nivelAcesso) { this.nivelAcesso = nivelAcesso; }
}
