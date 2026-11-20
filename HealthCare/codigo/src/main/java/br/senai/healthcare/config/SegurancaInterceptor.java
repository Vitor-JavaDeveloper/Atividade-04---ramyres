package br.senai.healthcare.config;

import br.senai.healthcare.modelo.Usuario;
import br.senai.healthcare.servico.AuthServico;
import br.senai.healthcare.servico.ErroApi;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * "porteiro" da api.
 *
 * roda antes de cada requisicao para /api/**. ele:
 * 1. le o token do cabecalho "authorization: bearer <token>";
 * 2. descobre qual usuario e o dono do token (se nao achar, responde 401);
 * 3. confere se o perfil do usuario esta na anotacao @perfis do metodo (se nao, responde 403);
 * 4. guarda o usuario na requisicao para o controller usar.
 */
@Component
public class SegurancaInterceptor implements HandlerInterceptor {

    public static final String USUARIO = "usuarioLogado";

    private final AuthServico auth;

    public SegurancaInterceptor(AuthServico auth) {
        this.auth = auth;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
        if (!(handler instanceof HandlerMethod metodo)) return true; // arquivos estaticos etc

        Usuario usuario = auth.usuarioDoToken(token(req))
                .orElseThrow(() -> new ErroApi(HttpStatus.UNAUTHORIZED, "Sessão expirada. Faça login novamente."));
        // se o admin desativou o usuario depois do login, a sessao deixa de valer
        if (!usuario.isAtivo()) {
            auth.logout(token(req));
            throw new ErroApi(HttpStatus.UNAUTHORIZED, "Usuário desativado. Procure o administrador.");
        }

        Perfis permitidos = metodo.getMethodAnnotation(Perfis.class);
        if (permitidos != null && Arrays.stream(permitidos.value()).noneMatch(p -> p == usuario.getPerfil())) {
            throw new ErroApi(HttpStatus.FORBIDDEN, "Seu perfil não tem acesso a esta função.");
        }
        req.setAttribute(USUARIO, usuario);
        return true;
    }

    /** tira o "bearer " da frente e devolve so o token. */
    public static String token(HttpServletRequest req) {
        String cabecalho = req.getHeader("Authorization");
        return cabecalho != null && cabecalho.startsWith("Bearer ") ? cabecalho.substring(7) : null;
    }
}
