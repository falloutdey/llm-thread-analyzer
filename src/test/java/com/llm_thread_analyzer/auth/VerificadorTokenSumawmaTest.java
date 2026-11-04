package com.llm_thread_analyzer.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llm_thread_analyzer.auth.VerificadorTokenSumawma.TokenInvalidoException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Os tokens abaixo foram gerados pela mesma biblioteca que o backend Node do Sumawma usa
 * (jsonwebtoken 9.0.2), com jwt.sign({idestudante, iat: 1793000000}, segredo, {expiresIn: '1h'}).
 * Assim o teste confere a compatibilidade real com o login do Sumawma.
 */
class VerificadorTokenSumawmaTest {

    private static final String SEGREDO = "segredo-de-teste-do-sumawma";

    // idestudante 42, HS256, segredo SEGREDO, exp = 1793003600
    private static final String TOKEN_OK = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZGVzdHVkYW50ZSI6NDIsImlhdCI6MTc5MzAwMDAwMCwiZXhwIjoxNzkzMDAzNjAwfQ.asM4AAElWFdHsdUCD3m86RvgwyoAdjbBZsLIzDbOBa0";
    // mesmo conteúdo, assinado com "outro-segredo"
    private static final String TOKEN_OUTRO_SEGREDO = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZGVzdHVkYW50ZSI6NDIsImlhdCI6MTc5MzAwMDAwMCwiZXhwIjoxNzkzMDAzNjAwfQ.-7rgNyWJAMCS8rmRTM2GdHi45fevszD9qWlyV5QSnn8";
    // {nome: "x"} sem idestudante
    private static final String TOKEN_SEM_ID = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJub21lIjoieCIsImlhdCI6MTc5MzAwMDAwMCwiZXhwIjoxNzkzMDAzNjAwfQ.soPJ_JBWoTwum1reFucLo1CRLjY5sjwQRflWUQKAtdI";
    // idestudante 42 com HS512
    private static final String TOKEN_HS512 = "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJpZGVzdHVkYW50ZSI6NDIsImlhdCI6MTc5MzAwMDAwMCwiZXhwIjoxNzkzMDAzNjAwfQ.NDwd2MtPlbHzuPyLtHrh17RucgaojUAuNKQ3xw6jpMXvJRraupjq2iu_xXG1B4g0CRTihuKhzrz-GU0hkf7LJw";
    // idestudante 7 com o segredo padrão do Node ("chave-secreta")
    private static final String TOKEN_SEGREDO_PADRAO = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZGVzdHVkYW50ZSI6NywiaWF0IjoxNzkzMDAwMDAwLCJleHAiOjE3OTMwMDM2MDB9.z3eUOre3zxofPKddtBEi7CdZ8hZq6YHxVDUHxx6rupg";

    private static final Instant DENTRO_DA_VALIDADE = Instant.ofEpochSecond(1793001000);

    private final VerificadorTokenSumawma verificador = new VerificadorTokenSumawma(SEGREDO, new ObjectMapper());

    private String motivo(VerificadorTokenSumawma v, String token, Instant agora) {
        return assertThrows(TokenInvalidoException.class, () -> v.verificar(token, agora)).getMessage();
    }

    @Test
    void aceitaTokenDoNode() {
        assertEquals(42, verificador.verificar(TOKEN_OK, DENTRO_DA_VALIDADE));
    }

    @Test
    void respeitaValidadeComToleranciaDe30s() {
        assertEquals(42, verificador.verificar(TOKEN_OK, Instant.ofEpochSecond(1793003600 + 29)));
        assertEquals("Token vencido. Faça login novamente",
                motivo(verificador, TOKEN_OK, Instant.ofEpochSecond(1793003600 + 31)));
    }

    @Test
    void recusaOutroSegredo() {
        assertEquals("Assinatura do token inválida", motivo(verificador, TOKEN_OUTRO_SEGREDO, DENTRO_DA_VALIDADE));
    }

    @Test
    void recusaConteudoAdulterado() {
        String[] partes = TOKEN_OK.split("\\.");
        String outroAluno = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"idestudante\":1,\"iat\":1793000000,\"exp\":1793003600}".getBytes(StandardCharsets.UTF_8));
        assertEquals("Assinatura do token inválida",
                motivo(verificador, partes[0] + "." + outroAluno + "." + partes[2], DENTRO_DA_VALIDADE));
    }

    @Test
    void recusaAlgoritmoDiferenteDeHs256() {
        assertEquals("Algoritmo do token não aceito", motivo(verificador, TOKEN_HS512, DENTRO_DA_VALIDADE));
        String[] partes = TOKEN_OK.split("\\.");
        String semAssinatura = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8)) + "." + partes[1] + ".";
        assertThrows(TokenInvalidoException.class, () -> verificador.verificar(semAssinatura, DENTRO_DA_VALIDADE));
    }

    @Test
    void recusaTokenSemIdEstudante() {
        assertEquals("Token sem idestudante válido", motivo(verificador, TOKEN_SEM_ID, DENTRO_DA_VALIDADE));
    }

    @Test
    void recusaLixo() {
        assertEquals("Token mal formado", motivo(verificador, "abc", DENTRO_DA_VALIDADE));
        assertEquals("Token mal formado", motivo(verificador, "a.b.c", DENTRO_DA_VALIDADE));
        assertEquals("Token ausente", motivo(verificador, "  ", DENTRO_DA_VALIDADE));
    }

    @Test
    void funcionaComSegredoCurto() {
        var padrao = new VerificadorTokenSumawma("chave-secreta", new ObjectMapper());
        assertEquals(7, padrao.verificar(TOKEN_SEGREDO_PADRAO, DENTRO_DA_VALIDADE));
    }

    @Test
    void semSegredoNadaEAceito() {
        var semSegredo = new VerificadorTokenSumawma("", new ObjectMapper());
        assertFalse(semSegredo.configurado());
        assertThrows(TokenInvalidoException.class, () -> semSegredo.verificar(TOKEN_OK, DENTRO_DA_VALIDADE));
    }
}
