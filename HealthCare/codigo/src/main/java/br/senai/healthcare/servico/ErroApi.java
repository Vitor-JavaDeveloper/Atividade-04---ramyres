package br.senai.healthcare.servico;

import org.springframework.http.HttpStatus;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * erro "esperado" do sistema (dado invalido, sem permissao, nao encontrado...).
 *
 * os servicos lancam este erro e o tratadordeerros transforma ele numa
 * resposta json bonitinha, com a mensagem e os campos que estao errados.
 * assim a tela consegue destacar exatamente o campo com problema (criterio do hu01).
 */
public class ErroApi extends RuntimeException {

    private final HttpStatus status;
    private final Map<String, String> campos;

    public ErroApi(HttpStatus status, String mensagem) {
        this(status, mensagem, new LinkedHashMap<>());
    }

    public ErroApi(HttpStatus status, String mensagem, Map<String, String> campos) {
        super(mensagem);
        this.status = status;
        this.campos = campos;
    }

    public HttpStatus getStatus() { return status; }
    public Map<String, String> getCampos() { return campos; }

    /** atalho para "registro nao encontrado". */
    public static ErroApi naoEncontrado(String oque) {
        return new ErroApi(HttpStatus.NOT_FOUND, oque + " não encontrado.");
    }

    /**
     * ajuda a juntar varios erros de campo antes de responder.
     * uso: cria, chama exigir(...) para cada campo e no final lancarsetivererro().
     */
    public static class Validador {
        private final Map<String, String> erros = new LinkedHashMap<>();

        /** se a condicao for falsa, anota a mensagem de erro para aquele campo. */
        public Validador exigir(boolean condicao, String campo, String mensagem) {
            if (!condicao && !erros.containsKey(campo)) erros.put(campo, mensagem);
            return this;
        }

        public void lancarSeTiverErro() {
            if (!erros.isEmpty()) {
                throw new ErroApi(HttpStatus.BAD_REQUEST, "Verifique os campos destacados.", erros);
            }
        }
    }

    /** verdadeiro se o texto existe e nao e so espaco em branco. */
    public static boolean preenchido(String s) { return s != null && !s.isBlank(); }
}
