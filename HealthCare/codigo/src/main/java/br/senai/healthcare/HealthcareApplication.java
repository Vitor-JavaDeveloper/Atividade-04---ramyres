package br.senai.healthcare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ponto de partida do sistema healthcare.
 *
 * quando rodamos "java -jar healthcare.jar" e este main que e executado.
 * ele liga o servidor web embutido (tomcat), conecta no postgresql,
 * cria as tabelas (schema.sql), grava os dados de demonstracao se o banco
 * estiver vazio, carrega a api e tambem entrega as telas do front-end
 * (pasta resources/static).
 */
@SpringBootApplication
public class HealthcareApplication {

    public static void main(String[] args) {
        SpringApplication.run(HealthcareApplication.class, args);
    }
}
