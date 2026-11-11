package com.llm_thread_analyzer.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.llm_thread_analyzer.llm.LlmTipos.LlmException;
import com.llm_thread_analyzer.llm.LlmTipos.MotivoFalha;
import com.llm_thread_analyzer.llm.LlmTipos.Papel;
import com.llm_thread_analyzer.llm.LlmTipos.Requisicao;
import com.llm_thread_analyzer.llm.LlmTipos.Resposta;
import com.llm_thread_analyzer.llm.LlmTipos.Turno;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** Claude pela Messages API, contra um servidor local que imita a API da Anthropic. */
class AnthropicProviderTest {

    private final ObjectMapper json = new ObjectMapper();
    private HttpServer servidor;
    private final AtomicReference<JsonNode> pedido = new AtomicReference<>();
    private final AtomicReference<String> chaveRecebida = new AtomicReference<>();
    private final AtomicReference<String> versaoRecebida = new AtomicReference<>();

    private String subir(int status, String resposta) throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/v1/messages", troca -> {
            pedido.set(json.readTree(troca.getRequestBody().readAllBytes()));
            chaveRecebida.set(troca.getRequestHeaders().getFirst("x-api-key"));
            versaoRecebida.set(troca.getRequestHeaders().getFirst("anthropic-version"));
            byte[] bytes = resposta.getBytes(StandardCharsets.UTF_8);
            troca.getResponseHeaders().add("Content-Type", "application/json");
            troca.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = troca.getResponseBody()) { os.write(bytes); }
        });
        servidor.start();
        return "http://127.0.0.1:" + servidor.getAddress().getPort() + "/v1/messages";
    }

    @AfterEach
    void parar() {
        if (servidor != null) servidor.stop(0);
    }

    private AnthropicProvider provedor(String url) {
        LlmProperties.Modelo m = new LlmProperties.Modelo();
        m.setTipo("anthropic");
        m.setModelo("claude-haiku-5-5");
        m.setUrl(url);
        m.setChave("chave-teste");
        m.setMaxTokens(2048);
        m.setPensar(false);
        return new AnthropicProvider("claude-haiku", m, HttpClient.newHttpClient(), json);
    }

    private static Requisicao pedidoComEsquema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("nivel", Map.of("type", "integer", "minimum", 0, "maximum", 3));
        props.put("mensagem", Map.of("type", "string"));
        Map<String, Object> esquema = new LinkedHashMap<>();
        esquema.put("type", "object");
        esquema.put("properties", props);
        esquema.put("required", List.of("nivel", "mensagem"));
        return new Requisicao("instruções do tutor",
                List.of(new Turno(Papel.USUARIO, "Início"), new Turno(Papel.MODELO, "Olá"),
                        new Turno(Papel.USUARIO, "parte 1"), new Turno(Papel.USUARIO, "parte 2")),
                esquema, null);
    }

    @Test
    void montaOPedidoNoFormatoDaAnthropicELeAResposta() throws Exception {
        String url = subir(200, """
                {"content":[{"type":"text","text":"{\\"nivel\\":0,\\"mensagem\\":\\"Oi\\"}"}],
                 "stop_reason":"end_turn","usage":{"input_tokens":120,"output_tokens":15}}""");
        Resposta r = provedor(url).gerar(pedidoComEsquema(), 5000);

        assertEquals("{\"nivel\":0,\"mensagem\":\"Oi\"}", r.texto());
        assertEquals("anthropic", r.provedor());
        assertEquals(120, r.tokensEntrada());
        assertEquals(15, r.tokensSaida());

        assertEquals("chave-teste", chaveRecebida.get());
        assertEquals("2023-06-01", versaoRecebida.get());
        JsonNode p = pedido.get();
        assertEquals("claude-haiku-5-5", p.path("model").asText());
        assertEquals("instruções do tutor", p.path("system").asText());
        assertFalse(p.has("temperature"), "modelos 5.5 não aceitam temperatura");
        assertEquals("disabled", p.path("thinking").path("type").asText());
        JsonNode msgs = p.path("messages");
        assertEquals(3, msgs.size(), "turnos seguidos do mesmo papel são juntados");
        assertEquals("parte 1\n\nparte 2", msgs.get(2).path("content").asText());
        JsonNode esquema = p.path("output_config").path("format").path("schema");
        assertEquals("json_schema", p.path("output_config").path("format").path("type").asText());
        assertFalse(esquema.path("additionalProperties").asBoolean(true), "additionalProperties=false é obrigatório");
        assertFalse(esquema.path("properties").path("nivel").has("minimum"), "minimum não é aceito pela API");
        assertFalse(esquema.path("properties").path("nivel").has("maximum"));
    }

    @Test
    void recusaDoModeloViraBloqueado() throws Exception {
        String url = subir(200, "{\"content\":[],\"stop_reason\":\"refusal\"}");
        LlmException e = assertThrows(LlmException.class, () -> provedor(url).gerar(pedidoComEsquema(), 5000));
        assertEquals(MotivoFalha.BLOQUEADO, e.getMotivo());
    }

    @Test
    void sobrecarga529ETemporaria() throws Exception {
        String url = subir(529, "{\"type\":\"error\",\"error\":{\"type\":\"overloaded_error\"}}");
        LlmException e = assertThrows(LlmException.class, () -> provedor(url).gerar(pedidoComEsquema(), 5000));
        assertEquals(MotivoFalha.SERVIDOR, e.getMotivo());
        assertTrue(e.temporaria());
    }

    @Test
    void chaveInvalidaNaoETemporaria() throws Exception {
        String url = subir(401, "{\"type\":\"error\",\"error\":{\"type\":\"authentication_error\"}}");
        LlmException e = assertThrows(LlmException.class, () -> provedor(url).gerar(pedidoComEsquema(), 5000));
        assertFalse(e.temporaria());
    }
}
