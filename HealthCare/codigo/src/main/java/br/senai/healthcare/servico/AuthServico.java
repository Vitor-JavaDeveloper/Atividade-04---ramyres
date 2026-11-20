package br.senai.healthcare.servico;

import br.senai.healthcare.modelo.Usuario;
import br.senai.healthcare.repositorio.UsuarioRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * autenticacao dos usuarios (rf06 e hu06).
 *
 * como funciona: o usuario manda login e senha, a gente busca no banco e confere
 * a senha com o hash bcrypt. se estiver certo, gera um "token" (um codigo aleatorio).
 * a tela guarda esse token e manda ele em toda requisicao no cabecalho authorization.
 * assim o servidor sabe quem esta logado sem pedir a senha de novo.
 *
 * os tokens ficam na memoria do servidor: se o sistema reiniciar, e so logar de novo.
 */
@Service
public class AuthServico {

    private final UsuarioRepositorio usuarios;

    // token -> id do usuario logado
    private final Map<String, Long> sessoes = new ConcurrentHashMap<>();

    public AuthServico(UsuarioRepositorio usuarios) {
        this.usuarios = usuarios;
    }

    /** faz o login. devolve o token ou lanca erro 401 com a mensagem pedida no criterio de aceitacao. */
    public String login(String login, String senha) {
        Usuario usuario = usuarios.findByLoginIgnoreCase(login == null ? "" : login.trim())
                .filter(u -> u.autenticar(login.trim(), senha))
                .orElseThrow(() -> new ErroApi(HttpStatus.UNAUTHORIZED, "Usuário ou senha incorretos."));
        // usuario desativado nao entra, mesmo com a senha certa
        if (!usuario.isAtivo()) {
            throw new ErroApi(HttpStatus.UNAUTHORIZED, "Usuário desativado. Procure o administrador.");
        }
        String token = UUID.randomUUID().toString();
        sessoes.put(token, usuario.getId());
        return token;
    }

    /** descobre quem e o dono do token, sempre lendo o usuario atualizado do banco. */
    public Optional<Usuario> usuarioDoToken(String token) {
        Long id = token == null ? null : sessoes.get(token);
        return id == null ? Optional.empty() : usuarios.findById(id);
    }

    public void logout(String token) {
        if (token != null) sessoes.remove(token);
    }

    /** tira todas as sessoes de um usuario (usado quando o admin desativa o acesso). */
    public void encerrarSessoesDo(Long usuarioId) {
        if (usuarioId != null) sessoes.entrySet().removeIf(e -> usuarioId.equals(e.getValue()));
    }
}
