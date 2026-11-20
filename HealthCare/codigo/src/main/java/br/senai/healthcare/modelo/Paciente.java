package br.senai.healthcare.modelo;

import jakarta.persistence.*;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

/** paciente da clinica (rf01). campos iguais aos da tela de cadastro. */
@Entity
@Table(name = "paciente")
public class Paciente extends Entidade {

    private String nome;
    private String cpf;               // guardado so com numeros
    private LocalDate dataNascimento;
    private String sexo;
    private String telefone;
    private String endereco;
    private LocalDateTime cadastradoEm = LocalDateTime.now();

    /** metodo calcularidade() do diagrama de classes: anos completos desde o nascimento. */
    public int calcularIdade() {
        return dataNascimento == null ? 0 : Period.between(dataNascimento, LocalDate.now()).getYears();
    }

    /** o spring transforma este getter no campo "idade" do json enviado para a tela. */
    public int getIdade() { return calcularIdade(); }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
    public LocalDateTime getCadastradoEm() { return cadastradoEm; }
    public void setCadastradoEm(LocalDateTime cadastradoEm) { this.cadastradoEm = cadastradoEm; }
}
