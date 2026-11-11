package com.llm_thread_analyzer.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.llm_thread_analyzer.llm.LlmTipos.Papel;
import com.llm_thread_analyzer.llm.LlmTipos.Requisicao;
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

/** API compatível com OpenAI (Groq): esquema estrito e reasoning_effort quando configurados. */
class OpenAiCompatProviderTest {

    private final ObjectMapper json = new ObjectMapper();
    private HttpServer servidor;
    private final AtomicReference<JsonNode> pedido = new AtomicReference<>();

    @AfterEach
    void parar() {
        if (servidor != null) servidor.stop(0);
    }

    private String subir() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/v1/chat/completions", troca -> {
            pedido.set(json.readTree(troca.getRequestBody().readAllBytes()));
            byte[] b = "{\"choices\":[{\"message\":{\"content\":\"{}\"},\"finish_reason\":\"stop\"}]}".getBytes(StandardCharsets.UTF_8);
            troca.sendResponseHeaders(200, b.length);
            try (OutputStream os = troca.getResponseBody()) { os.write(b); }
        });
        servidor.start();
        return "http://127.0.0.1:" + servidor.getAddress().getPort() + "/v1/chat/completions";
    }

    private Requisicao requisicao() {
        Map<String, Object> esquema = new LinkedHashMap<>();
        esquema.put("type", "object");
        esquema.put("properties", Map.of("nivel", Map.of("type", "integer", "minimum", 0, "maximum", 3)));
        esquema.put("required", List.of("nivel"));
        return new Requisicao("sistema", List.of(new Turno(Papel.USUARIO, "oi")), esquema, null);
    }

    private OpenAiCompatProvider provedor(String url, boolean estrito, String esforco) {
        LlmProperties.Modelo m = new LlmProperties.Modelo();
        m.setTipo("openai");
        m.setModelo("openai/gpt-oss-120b");
        m.setUrl(url);
        m.setChave("x");
        m.setEsquemaEstrito(estrito);
        m.setEsforco(esforco);
        return new OpenAiCompatProvider("groq-gpt-oss", m, HttpClient.newHttpClient(), json);
    }

    @Test
    void comEsquemaEstritoMandaJsonSchemaAdaptado() throws Exception {
        provedor(subir(), true, "low").gerar(requisicao(), 5000);
        JsonNode rf = pedido.get().path("response_format");
        assertEquals("json_schema", rf.path("type").asText());
        assertTrue(rf.path("json_schema").path("strict").asBoolean());
        JsonNode esquema = rf.path("json_schema").path("schema");
        assertFalse(esquema.path("additionalProperties").asBoolean(true));
        assertFalse(esquema.path("properties").path("nivel").has("minimum"));
        assertEquals("low", pedido.get().path("reasoning_effort").asText());
    }

    @Test
    void semEsquemaEstritoContinuaJsonObject() throws Exception {
        provedor(subir(), false, null).gerar(requisicao(), 5000);
        assertEquals("json_object", pedido.get().path("response_format").path("type").asText());
        assertFalse(pedido.get().has("reasoning_effort"));
    }
}
