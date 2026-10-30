package com.llm_thread_analyzer.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

/**
 * Confere o token de login (JWT) emitido pelo backend Node do Sumawma.
 *
 * O Node assina com HS256 (biblioteca jsonwebtoken) usando JWT_SECRET, e o conteúdo é
 * {"idestudante": <número>, "iat": ..., "exp": ...}. Aqui só se confere o token: quem
 * faz login e emite o token continua sendo o Node, que não muda.
 *
 * A conferência é feita com o próprio JDK (HMAC-SHA256), sem biblioteca extra:
 *  - aceita só o algoritmo HS256 (recusa "none" e qualquer outro);
 *  - compara a assinatura em tempo constante;
 *  - exige "exp" e recusa token vencido (com 30 s de tolerância de relógio entre servidores);
 *  - exige "idestudante" inteiro e positivo.
 */
@Component
public class VerificadorTokenSumawma {

    /** Tolerância para diferença de relógio entre o servidor Node e este. */
    static final long TOLERANCIA_SEGUNDOS = 30;

    private final byte[] segredo;
    private final ObjectMapper json;

    public VerificadorTokenSumawma(@Value("${auth.jwt-secret:}") String segredo, ObjectMapper json) {
        this.segredo = segredo == null ? new byte[0] : segredo.getBytes(StandardCharsets.UTF_8);
        this.json = json;
    }

    /** Falso quando JWT_SECRET não foi configurado: nesse caso nenhum token é aceito. */
    public boolean configurado() {
        return segredo.length > 0;
    }

    /** Devolve o idestudante do token, ou lança TokenInvalidoException. */
    public long verificar(String token) {
        return verificar(token, Instant.now());
    }

    long verificar(String token, Instant agora) {
        if (!configurado()) {
            throw new TokenInvalidoException("JWT_SECRET não configurado no serviço");
        }
        if (token == null || token.isBlank()) {
            throw new TokenInvalidoException("Token ausente");
        }
        String[] partes = token.trim().split("\\.", -1);
        if (partes.length != 3 || partes[0].isEmpty() || partes[1].isEmpty() || partes[2].isEmpty()) {
            throw new TokenInvalidoException("Token mal formado");
        }

        JsonNode cabecalho = lerJson(partes[0]);
        if (!"HS256".equals(cabecalho.path("alg").asText(null))) {
            throw new TokenInvalidoException("Algoritmo do token não aceito");
        }

        byte[] assinaturaRecebida = decodificar(partes[2]);
        byte[] assinaturaEsperada = hmacSha256((partes[0] + "." + partes[1]).getBytes(StandardCharsets.US_ASCII));
        if (!MessageDigest.isEqual(assinaturaEsperada, assinaturaRecebida)) {
            throw new TokenInvalidoException("Assinatura do token inválida");
        }

        JsonNode conteudo = lerJson(partes[1]);
        long agoraSeg = agora.getEpochSecond();

        JsonNode exp = conteudo.get("exp");
        if (exp == null || !exp.isNumber()) {
            throw new TokenInvalidoException("Token sem validade (exp)");
        }
        if (agoraSeg >= exp.asLong() + TOLERANCIA_SEGUNDOS) {
            throw new TokenInvalidoException("Token vencido. Faça login novamente");
        }
        JsonNode nbf = conteudo.get("nbf");
        if (nbf != null && nbf.isNumber() && agoraSeg + TOLERANCIA_SEGUNDOS < nbf.asLong()) {
            throw new TokenInvalidoException("Token ainda não é válido");
        }

        JsonNode id = conteudo.get("idestudante");
        if (id == null || !id.canConvertToLong() || !id.isIntegralNumber() || id.asLong() <= 0) {
            throw new TokenInvalidoException("Token sem idestudante válido");
        }
        return id.asLong();
    }

    private JsonNode lerJson(String parteBase64) {
        try {
            JsonNode no = json.readTree(decodificar(parteBase64));
            if (no == null || !no.isObject()) throw new TokenInvalidoException("Token mal formado");
            return no;
        } catch (java.io.IOException e) {
            throw new TokenInvalidoException("Token mal formado");
        }
    }

    private static byte[] decodificar(String parteBase64) {
        try {
            return Base64.getUrlDecoder().decode(parteBase64);
        } catch (IllegalArgumentException e) {
            throw new TokenInvalidoException("Token mal formado");
        }
    }

    private byte[] hmacSha256(byte[] dados) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo, "HmacSHA256"));
            return mac.doFinal(dados);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 indisponível", e);
        }
    }

    /** Token recusado; a mensagem vai para o aluno (sem detalhes internos). */
    public static class TokenInvalidoException extends RuntimeException {
        public TokenInvalidoException(String mensagem) { super(mensagem); }
    }
}
