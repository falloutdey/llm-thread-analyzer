package com.llm_thread_analyzer.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llm_thread_analyzer.llm.LlmTipos.MotivoFalha;
import com.llm_thread_analyzer.llm.LlmTipos.Papel;
import com.llm_thread_analyzer.llm.LlmTipos.Requisicao;
import com.llm_thread_analyzer.llm.LlmTipos.Resultado;
import com.llm_thread_analyzer.llm.LlmTipos.Turno;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pico de 503 nos dois modelos ao mesmo tempo (teste de 05/11): com llm.voltas-na-cadeia=2,
 * o roteador percorre a cadeia de novo dentro do prazo em vez de desistir.
 */
class LlmRouterVoltasTest {

    private HttpServer servidor;

    /** Servidor compatível com OpenAI; cada caminho devolve os códigos da fila, na ordem (depois, 200). */
    private String subir(Map<String, Deque<Integer>> respostas) throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        for (var e : respostas.entrySet()) {
            servidor.createContext("/" + e.getKey(), troca -> {
                Integer codigo = e.getValue().poll();
                int status = codigo == null ? 200 : codigo;
                String corpo = status == 200
                        ? "{\"choices\":[{\"message\":{\"content\":\"resposta de " + e.getKey() + "\"},\"finish_reason\":\"stop\"}]}"
                        : "{\"error\":{\"code\":" + status + ",\"message\":\"high demand\"}}";
                byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
                troca.getResponseHeaders().add("Content-Type", "application/json");
                troca.sendResponseHeaders(status, bytes.length);
                try (OutputStream os = troca.getResponseBody()) { os.write(bytes); }
            });
        }
        servidor.start();
        return "http://127.0.0.1:" + servidor.getAddress().getPort() + "/";
    }

    @AfterEach
    void parar() {
        if (servidor != null) servidor.stop(0);
    }

    private LlmRouter roteador(String base, int voltas) {
        LlmProperties props = new LlmProperties();
        Map<String, LlmProperties.Modelo> modelos = new LinkedHashMap<>();
        for (String nome : List.of("principal", "reserva")) {
            LlmProperties.Modelo m = new LlmProperties.Modelo();
            m.setTipo("openai");
            m.setModelo(nome);
            m.setUrl(base + nome);
            m.setChave("x");
            modelos.put(nome, m);
        }
        props.setModelos(modelos);
        props.setCadeia(List.of("principal", "reserva"));
        props.setFallbackHabilitado(true);
        props.setPrazoTotalMs(10_000);
        props.setTimeoutChamadaMs(5_000);
        props.setVoltasNaCadeia(voltas);
        return new LlmRouter(props, new ObjectMapper());
    }

    private static Requisicao pedido() {
        return new Requisicao("sistema", List.of(new Turno(Papel.USUARIO, "oi")));
    }

    private static Deque<Integer> fila(Integer... codigos) {
        return new ArrayDeque<>(List.of(codigos));
    }

    @Test
    void comDuasVoltasRecuperaDoPicoDe503NosDoisModelos() throws Exception {
        String base = subir(Map.of("principal", fila(503), "reserva", fila(503)));
        Resultado r = roteador(base, 2).gerar(pedido(), true);
        assertTrue(r.sucesso());
        assertEquals("resposta de principal", r.resposta().texto());
        assertEquals(3, r.tentativas().size(), "principal 503, reserva 503, principal OK");
    }

    @Test
    void comUmaVoltaDesisteComoAntes() throws Exception {
        String base = subir(Map.of("principal", fila(503), "reserva", fila(503)));
        Resultado r = roteador(base, 1).gerar(pedido(), true);
        assertFalse(r.sucesso());
        assertEquals(2, r.tentativas().size());
    }

    @Test
    void naoVoltaAoModeloComFalhaDefinitiva() throws Exception {
        // 400 é erro do pedido (definitivo): na segunda volta, só o reserva é tentado de novo.
        String base = subir(Map.of("principal", fila(400, 400), "reserva", fila(503)));
        Resultado r = roteador(base, 2).gerar(pedido(), true);
        assertTrue(r.sucesso());
        assertEquals("resposta de reserva", r.resposta().texto());
        assertEquals(List.of("principal", "reserva", "reserva"),
                r.tentativas().stream().map(t -> t.nomeConfig()).toList());
        assertNotEquals(MotivoFalha.SERVIDOR, r.tentativas().get(0).motivoFalha());
    }
}
