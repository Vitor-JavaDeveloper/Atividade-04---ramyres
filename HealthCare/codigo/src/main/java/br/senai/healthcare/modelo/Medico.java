package br.senai.healthcare.modelo;

import jakarta.persistence.*;


/**
 * medico (herda de usuario).
 * tem crm, especialidade e o status de disponibilidade (rf02 e rf07).
 * o login e a senha sao opcionais: um medico pode ser cadastrado so para
 * aparecer na lista de disponibilidade, sem acessar o sistema.
 */
@Entity
@DiscriminatorValue("MEDICO")
public class Medico extends Usuario {

    private String crm;
    private String especialidade;
    private boolean disponivel;

    protected Medico() { } // exigido pelo jpa

    public Medico(String nome, String login, String senha, String crm, String especialidade, boolean disponivel) {
        super(nome, login, senha, PerfilUsuario.MEDICO);
        this.crm = crm;
        this.especialidade = especialidade;
        this.disponivel = disponivel;
    }

    /** metodo atualizardisponibilidade(disponivel) do diagrama. */
    public void atualizarDisponibilidade(boolean disponivel) { this.disponivel = disponivel; }

    public String getCrm() { return crm; }
    public void setCrm(String crm) { this.crm = crm; }
    public String getEspecialidade() { return especialidade; }
    public void setEspecialidade(String especialidade) { this.especialidade = especialidade; }
    public boolean isDisponivel() { return disponivel; }
    public void setDisponivel(boolean disponivel) { this.disponivel = disponivel; }

    /** diz para a tela se este medico consegue fazer login (tem usuario e senha). */
    public boolean isTemAcesso() { return getLogin() != null && !getLogin().isBlank(); }
}
