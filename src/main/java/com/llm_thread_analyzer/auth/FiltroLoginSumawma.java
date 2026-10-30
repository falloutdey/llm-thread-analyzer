package com.llm_thread_analyzer.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Lê "Authorization: Bearer <token>" nas rotas protegidas (hoje, /api/java/**) e, se o token
 * do Sumawma for válido, marca a requisição como do aluno do token.
 *
 * Não é um @Component de propósito: é criado no SecurityConfig e só roda dentro da cadeia
 * do Spring Security (se fosse bean, o Spring Boot o registraria também fora dela).
 */
public class FiltroLoginSumawma extends OncePerRequestFilter {

    /** Atributo da requisição com o motivo da recusa, usado pela resposta 401. */
    static final String ATRIBUTO_MOTIVO = FiltroLoginSumawma.class.getName() + ".motivo";

    private final VerificadorTokenSumawma verificador;
    private final String prefixoProtegido;

    public FiltroLoginSumawma(VerificadorTokenSumawma verificador, String prefixoProtegido) {
        this.verificador = verificador;
        this.prefixoProtegido = prefixoProtegido;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String caminho = request.getRequestURI().substring(request.getContextPath().length());
        return !caminho.startsWith(prefixoProtegido);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecalho = request.getHeader("Authorization");
        if (cabecalho == null || !cabecalho.regionMatches(true, 0, "Bearer ", 0, 7)) {
            request.setAttribute(ATRIBUTO_MOTIVO, "Login necessário");
        } else {
            try {
                long idEstudante = verificador.verificar(cabecalho.substring(7));
                var autenticacao = new UsernamePasswordAuthenticationToken(
                        new AlunoAutenticado(idEstudante), null, List.of(new SimpleGrantedAuthority("ROLE_ALUNO")));
                SecurityContextHolder.getContext().setAuthentication(autenticacao);
            } catch (VerificadorTokenSumawma.TokenInvalidoException e) {
                request.setAttribute(ATRIBUTO_MOTIVO, e.getMessage());
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * Resposta para requisição sem login válido: 401 com {"erro": motivo}.
     * Se o serviço está sem JWT_SECRET, responde 503 (problema de configuração, não do aluno).
     */
    public static AuthenticationEntryPoint respostaSemLogin(VerificadorTokenSumawma verificador, ObjectMapper json) {
        return (request, response, excecao) -> {
            Object motivo = request.getAttribute(ATRIBUTO_MOTIVO);
            int status = verificador.configurado()
                    ? HttpServletResponse.SC_UNAUTHORIZED
                    : HttpServletResponse.SC_SERVICE_UNAVAILABLE;
            String mensagem = verificador.configurado()
                    ? (motivo != null ? motivo.toString() : "Login necessário")
                    : "Login do módulo Java indisponível: JWT_SECRET não configurado no serviço";
            response.setStatus(status);
            response.setCharacterEncoding("UTF-8");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            json.writeValue(response.getOutputStream(), Map.of("erro", mensagem));
        };
    }
}
