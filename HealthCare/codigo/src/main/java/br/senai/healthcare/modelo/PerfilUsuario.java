package br.senai.healthcare.modelo;

/**
 * perfis de acesso do sistema (enumeration perfilusuario do diagrama de classes).
 * cada perfil enxerga um conjunto diferente de telas (rf06).
 * o campo telainicial diz para qual tela o usuario vai logo depois do login.
 */
public enum PerfilUsuario {
    ADMINISTRADOR("relatorios"),
    RECEPCIONISTA("cadastro-paciente"),
    ENFERMEIRO("triagem"),
    MEDICO("fila");

    private final String telaInicial;

    PerfilUsuario(String telaInicial) { this.telaInicial = telaInicial; }

    public String getTelaInicial() { return telaInicial; }
}
