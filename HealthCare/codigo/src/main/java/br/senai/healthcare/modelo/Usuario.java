package br.senai.healthcare.modelo;

import jakarta.persistence.*;


import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * usuario do sistema (classe usuario do diagrama de classes).
 *
 * administrador, recepcionista, enfermeiro e medico herdam desta classe,
 * por isso todos tem nome, login, senha, perfil e o status ativo/inativo.
 */
@Entity
@Table(name = "usuario")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE) // todos os perfis na mesma tabela
@DiscriminatorColumn(name = "tipo")                   // coluna que diz qual subclasse e
public class Usuario extends Entidade {

    // um unico "codificador" bcrypt para o sistema inteiro.
    // bcrypt gera um hash da senha que nao da para desfazer (rnf01 e lgpd).
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    private String nome;
    private String login;

    // @jsonignore faz a senha nunca ser enviada nas respostas da api
    @JsonIgnore
    private String senha;

    @Enumerated(EnumType.STRING) // grava o nome do perfil (ex: medico) e nao um numero
    private PerfilUsuario perfil;

    // funcionario ativo entra no sistema; desativado permanece cadastrado, mas o login e recusado
    private boolean ativo = true;

    protected Usuario() { } // construtor vazio exigido pelo jpa

    public Usuario(String nome, String login, String senhaAberta, PerfilUsuario perfil) {
        this.nome = nome;
        this.login = login;
        definirSenha(senhaAberta);
        this.perfil = perfil;
        this.ativo = true;
    }

    /** guarda so o hash da senha, nunca a senha em texto puro. */
    public void definirSenha(String senhaAberta) {
        this.senha = (senhaAberta == null || senhaAberta.isBlank()) ? null : BCRYPT.encode(senhaAberta);
    }

    /**
     * metodo autenticar(login, senha) do diagrama.
     * confere se o login bate e se a senha digitada corresponde ao hash guardado.
     */
    public boolean autenticar(String login, String senhaDigitada) {
        return this.login != null && this.senha != null
                && this.login.equalsIgnoreCase(login)
                && senhaDigitada != null
                && BCRYPT.matches(senhaDigitada, this.senha);
    }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public PerfilUsuario getPerfil() { return perfil; }
    public void setPerfil(PerfilUsuario perfil) { this.perfil = perfil; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
