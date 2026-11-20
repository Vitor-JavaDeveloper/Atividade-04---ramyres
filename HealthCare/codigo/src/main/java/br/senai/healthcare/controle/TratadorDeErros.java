package br.senai.healthcare.controle;

import br.senai.healthcare.servico.ErroApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * transforma qualquer erro em uma resposta json no formato:
 * { "erro": "mensagem", "campos": { "cpf": "cpf inválido." } }
 * a tela le esse formato para mostrar a mensagem e marcar o campo errado.
 */
@RestControllerAdvice
public class TratadorDeErros {

    /** erros previstos pelas regras do sistema. */
    @ExceptionHandler(ErroApi.class)
    public ResponseEntity<Map<String, Object>> erroDoSistema(ErroApi e) {
        return resposta(e.getStatus(), e.getMessage(), e.getCampos());
    }

    /** json mal formado ou com tipo errado (ex: letra no lugar de numero, inclusive no endereco como /api/pacientes/abc). */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> jsonInvalido(Exception e) {
        return resposta(HttpStatus.BAD_REQUEST, "Dados enviados em formato inválido.", Map.of());
    }

    /** endereco que nao existe. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> naoExiste(Exception e) {
        return resposta(HttpStatus.NOT_FOUND, "Endereço não encontrado.", Map.of());
    }

    /** qualquer outro erro inesperado: mostra no terminal e responde 500 sem detalhes tecnicos. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> erroInesperado(Exception e) {
        e.printStackTrace();
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor. Veja o log do sistema.", Map.of());
    }

    private ResponseEntity<Map<String, Object>> resposta(HttpStatus status, String msg, Map<String, String> campos) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("erro", msg);
        corpo.put("campos", campos);
        return ResponseEntity.status(status).body(corpo);
    }
}
