package com.llm_thread_analyzer.projetos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API de projetos Java ponta a ponta (banco H2 em memória, separado do ./dados do desenvolvimento).
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:projetos-java-teste;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "auth.jwt-secret=" + ProjetoJavaControllerTest.SEGREDO,
        "gemini.api.key=teste",
        "java.projetos.max-por-aluno=3",
        "java.projetos.max-arquivos=3",
        "java.projetos.max-caracteres-arquivo=50"
})
@AutoConfigureMockMvc
class ProjetoJavaControllerTest {

    static final String SEGREDO = "segredo-de-teste-do-sumawma";
    private static final String URL = "/api/java/projetos";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    // ---------- apoio ----------

    /** Token no mesmo formato do login do Sumawma (jsonwebtoken, HS256). */
    static String token(long idEstudante, long exp, String segredo) {
        try {
            Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
            String cab = b64.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
            String corpo = b64.encodeToString(("{\"idestudante\":" + idEstudante + ",\"iat\":" + (exp - 3600)
                    + ",\"exp\":" + exp + "}").getBytes(StandardCharsets.UTF_8));
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String assinatura = b64.encodeToString(mac.doFinal((cab + "." + corpo).getBytes(StandardCharsets.US_ASCII)));
            return cab + "." + corpo + "." + assinatura;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static String token(long idEstudante) {
        return token(idEstudante, Instant.now().getEpochSecond() + 3600, SEGREDO);
    }

    private ResultActions chamar(MockHttpServletRequestBuilder req, long idEstudante, Object corpo) throws Exception {
        req.header("Authorization", "Bearer " + token(idEstudante));
        if (corpo != null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo));
        return mvc.perform(req);
    }

    private JsonNode criarProjeto(long idEstudante, String nome) throws Exception {
        String resposta = chamar(post(URL), idEstudante, Map.of("nome", nome))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(resposta);
    }

    // ---------- login ----------

    @Test
    void semTokenResponde401() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("Login necessário"));
    }

    @Test
    void tokenComOutroSegredoResponde401() throws Exception {
        mvc.perform(get(URL).header("Authorization", "Bearer " + token(1, Instant.now().getEpochSecond() + 3600, "outro")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("Assinatura do token inválida"));
    }

    @Test
    void tokenVencidoResponde401() throws Exception {
        mvc.perform(get(URL).header("Authorization", "Bearer " + token(1, Instant.now().getEpochSecond() - 120, SEGREDO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("Token vencido. Faça login novamente"));
    }

    @Test
    void rotasAntigasContinuamSemLogin() throws Exception {
        // /api/tutor e /api/files não passam a exigir login (só /api/java/**).
        mvc.perform(get("/api/tutor/conversas/999999"))
                .andExpect(status().is(not(401)));
    }

    // ---------- projetos ----------

    @Test
    void criaProjetoComMainJavaEListaSoOsDoAluno() throws Exception {
        JsonNode projeto = criarProjeto(100, "  Contador com threads  ");
        org.junit.jupiter.api.Assertions.assertEquals("Contador com threads", projeto.get("nome").asText());
        org.junit.jupiter.api.Assertions.assertEquals("Main.java", projeto.get("arquivos").get(0).get("nome").asText());
        org.junit.jupiter.api.Assertions.assertTrue(projeto.get("arquivos").get(0).get("conteudo").asText().contains("public class Main"));

        criarProjeto(200, "Projeto de outro aluno");

        chamar(get(URL), 100, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome").value("Contador com threads"))
                .andExpect(jsonPath("$[0].quantidadeArquivos").value(1));
    }

    @Test
    void projetoDeOutroAlunoResponde404() throws Exception {
        long id = criarProjeto(300, "Meu projeto").get("id").asLong();
        chamar(get(URL + "/" + id), 301, null).andExpect(status().isNotFound());
        chamar(put(URL + "/" + id), 301, Map.of("nome", "invadido")).andExpect(status().isNotFound());
        chamar(delete(URL + "/" + id), 301, null).andExpect(status().isNotFound());
        chamar(post(URL + "/" + id + "/arquivos"), 301, Map.of("nome", "X.java")).andExpect(status().isNotFound());
        // o dono continua vendo o projeto intacto
        chamar(get(URL + "/" + id), 300, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Meu projeto"))
                .andExpect(jsonPath("$.arquivos", hasSize(1)));
    }

    @Test
    void validaNomeDoProjetoELimite() throws Exception {
        chamar(post(URL), 400, Map.of("nome", "   ")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Informe o nome do projeto."));
        chamar(post(URL), 400, null).andExpect(status().isBadRequest());
        criarProjeto(400, "a");
        criarProjeto(400, "b");
        criarProjeto(400, "c");
        chamar(post(URL), 400, Map.of("nome", "d")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", startsWith("Limite de 3 projetos")));
    }

    @Test
    void renomeiaEExcluiProjeto() throws Exception {
        long id = criarProjeto(500, "Antigo").get("id").asLong();
        chamar(put(URL + "/" + id), 500, Map.of("nome", "Novo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Novo"));
        chamar(delete(URL + "/" + id), 500, null).andExpect(status().isNoContent());
        chamar(get(URL + "/" + id), 500, null).andExpect(status().isNotFound());
    }

    // ---------- arquivos ----------

    @Test
    void criaSalvaRenomeiaEExcluiArquivo() throws Exception {
        long id = criarProjeto(600, "Arquivos").get("id").asLong();
        String base = URL + "/" + id + "/arquivos";

        String criado = chamar(post(base), 600, Map.of("nome", "Contador.java"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conteudo", containsString("public class Contador")))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long idArq = json.readTree(criado).get("id").asLong();

        chamar(put(base + "/" + idArq), 600, Map.of("conteudo", "class Contador {}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Contador.java"))
                .andExpect(jsonPath("$.conteudo").value("class Contador {}"));

        chamar(put(base + "/" + idArq), 600, Map.of("nome", "Somador.java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Somador.java"))
                .andExpect(jsonPath("$.conteudo").value("class Contador {}"));

        chamar(delete(base + "/" + idArq), 600, null).andExpect(status().isNoContent());
        chamar(get(URL + "/" + id), 600, null).andExpect(jsonPath("$.arquivos", hasSize(1)));
    }

    @Test
    void validaNomeDeArquivo() throws Exception {
        long id = criarProjeto(700, "Nomes").get("id").asLong();
        String base = URL + "/" + id + "/arquivos";
        for (String invalido : new String[]{"../Main.java", "pasta/Main.java", "Main.c", "Meu Arquivo.java",
                "Ação.java", "1Main.java", "class.java", ".java", ""}) {
            chamar(post(base), 700, Map.of("nome", invalido)).andExpect(status().isBadRequest());
        }
        // nome repetido, mesmo com maiúsculas diferentes (no Windows seriam o mesmo arquivo)
        chamar(post(base), 700, Map.of("nome", "main.java")).andExpect(status().isConflict());
    }

    @Test
    void limitesDeArquivo() throws Exception {
        long id = criarProjeto(800, "Limites").get("id").asLong();
        String base = URL + "/" + id + "/arquivos";
        chamar(post(base), 800, Map.of("nome", "Grande.java", "conteudo", "x".repeat(51)))
                .andExpect(status().isBadRequest());
        chamar(post(base), 800, Map.of("nome", "A.java", "conteudo", "class A {}")).andExpect(status().isCreated());
        chamar(post(base), 800, Map.of("nome", "B.java", "conteudo", "class B {}")).andExpect(status().isCreated());
        chamar(post(base), 800, Map.of("nome", "C.java", "conteudo", "class C {}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", startsWith("Limite de 3 arquivos")));
    }

    @Test
    void arquivoDeOutroProjetoResponde404() throws Exception {
        long meu = criarProjeto(900, "Meu").get("id").asLong();
        JsonNode outro = criarProjeto(900, "Outro");
        long arquivoDoOutro = outro.get("arquivos").get(0).get("id").asLong();
        chamar(put(URL + "/" + meu + "/arquivos/" + arquivoDoOutro), 900, Map.of("conteudo", "x"))
                .andExpect(status().isNotFound());
    }
}
