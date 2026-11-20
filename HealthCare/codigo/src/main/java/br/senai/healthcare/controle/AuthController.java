package br.senai.healthcare.controle;

import br.senai.healthcare.config.SegurancaInterceptor;
import br.senai.healthcare.modelo.Usuario;
import br.senai.healthcare.servico.AuthServico;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * endpoints de login e logout (rf06).
 * um "controller" so recebe a requisicao http, chama o servico certo e devolve a resposta.
 * a regra de negocio fica nos servicos.
 */
@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthServico auth;

    public AuthController(AuthServico auth) {
        this.auth = auth;
    }

    /** post /api/auth/login  corpo: {"login": "...", "senha": "..."} */
    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Map<String, String> dados) {
        String token = auth.login(dados.get("login"), dados.get("senha"));
        Usuario u = auth.usuarioDoToken(token).orElseThrow();
        return respostaUsuario(token, u);
    }

    /** get /api/auth/eu  devolve quem esta logado (a tela usa ao recarregar a pagina). */
    @GetMapping("/auth/eu")
    public Map<String, Object> eu(@RequestAttribute(SegurancaInterceptor.USUARIO) Usuario u, HttpServletRequest req) {
        return respostaUsuario(SegurancaInterceptor.token(req), u);
    }

    @PostMapping("/auth/logout")
    public Map<String, String> logout(HttpServletRequest req) {
        auth.logout(SegurancaInterceptor.token(req));
        return Map.of("mensagem", "Até logo!");
    }

    /** get /api/status  usado pelo iniciar.bat para saber se o sistema ja subiu. */
    @GetMapping("/status")
    public Map<String, String> status() {
        return Map.of("status", "online");
    }

    /** monta a resposta com o token, os dados do usuario e a tela inicial do perfil. */
    private Map<String, Object> respostaUsuario(String token, Usuario u) {
        return Map.of(
                "token", token,
                "usuario", u,
                "telaInicial", u.getPerfil().getTelaInicial());
    }
}
