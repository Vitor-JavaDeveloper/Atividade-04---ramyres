package br.senai.healthcare.config;

import br.senai.healthcare.modelo.PerfilUsuario;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * anotacao que colocamos em cima de um metodo da api para dizer
 * quais perfis podem usar aquele recurso.
 * exemplo: @perfis({perfilusuario.medico}) libera so para medicos.
 * sem a anotacao, qualquer usuario logado pode acessar.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Perfis {
    PerfilUsuario[] value();
}
