package com.llm_thread_analyzer.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Rotas antigas do tutor (/api/tutor/**). Elas confiam no alunoId enviado no corpo, sem login,
 * e o editor do Sumawma não as usa mais (usa /api/java/**). Ficam só para a bateria de robustez.
 *
 * O que acontece em cada requisição para /api/tutor/**, na ordem:
 *   1. Se tutor.rotas-antigas.habilitadas=false (servidor de produção), responde 404.
 *   2. Se o RELATORIO_TOKEN não está definido, responde 403: sem token, ninguém usa as rotas.
 *   3. Se o cabeçalho X-Relatorio-Token não confere com o RELATORIO_TOKEN, responde 401.
 *   4. Caso contrário, segue para o controlador. O script da bateria já manda esse cabeçalho.
 */
@Component
public class FiltroRotasAntigasTutor extends OncePerRequestFilter {

    static final String PREFIXO = "/api/tutor/";

    private final boolean habilitadas;
    private final String token;

    public FiltroRotasAntigasTutor(@Value("${tutor.rotas-antigas.habilitadas:true}") boolean habilitadas,
                                   @Value("${relatorio.token:}") String token) {
        this.habilitadas = habilitadas;
        this.token = token;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String caminho = req.getRequestURI().substring(req.getContextPath().length());
        return !(caminho.startsWith(PREFIXO) || caminho.equals("/api/tutor"))
                || "OPTIONS".equalsIgnoreCase(req.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        if (!habilitadas) {
            responder(resp, HttpServletResponse.SC_NOT_FOUND, "Rota desativada neste servidor.");
            return;
        }
        if (token == null || token.isBlank()) {
            responder(resp, HttpServletResponse.SC_FORBIDDEN,
                    "Rotas antigas do tutor desativadas: defina RELATORIO_TOKEN para usar a bateria de robustez.");
            return;
        }
        String recebido = req.getHeader("X-Relatorio-Token");
        if (recebido == null || !MessageDigest.isEqual(
                recebido.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
            responder(resp, HttpServletResponse.SC_UNAUTHORIZED, "Token inválido para as rotas antigas do tutor.");
            return;
        }
        chain.doFilter(req, resp);
    }

    private static void responder(HttpServletResponse resp, int status, String mensagem) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write("{\"erro\":\"" + mensagem.replace("\"", "'") + "\"}");
    }
}
