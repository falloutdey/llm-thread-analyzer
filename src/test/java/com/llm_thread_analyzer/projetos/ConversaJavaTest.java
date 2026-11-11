package com.llm_thread_analyzer.projetos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.llm_thread_analyzer.llm.LlmRouter;
import com.llm_thread_analyzer.llm.LlmTipos.Requisicao;
import com.llm_thread_analyzer.llm.LlmTipos.Resposta;
import com.llm_thread_analyzer.llm.LlmTipos.Resultado;
import com.llm_thread_analyzer.llm.LlmTipos.Tentativa;
import com.llm_thread_analyzer.tutor.repository.EventoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Chat do tutor no editor Java (/api/java/conversas): aluno vindo do token, conversa sobre um alerta
 * da análise do projeto, e a conversa acompanhando uma nova análise. O modelo de IA é simulado.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:conversa-java-teste;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "auth.jwt-secret=" + ProjetoJavaControllerTest.SEGREDO,
        "gemini.api.key=teste"
})
@AutoConfigureMockMvc
class ConversaJavaTest {

    private static final String PROJETOS = "/api/java/projetos";
    private static final String CONVERSAS = "/api/java/conversas";

    private static final String FALA = """
            {"diagnostico":"x","avaliacao":"SEM_RESPOSTA","nivel":0,"aluno_descreveu_correcao":false,
             "mensagem":"O que você espera que o programa imprima? Execute algumas vezes e compare."}""";

    private static final String MAIN = """
            import contas.Conta;

            public class Main {
                public static void main(String[] args) throws InterruptedException {
                    Conta conta = new Conta();
                    Thread t1 = new Thread(() -> { for (int i = 0; i < 1000; i++) conta.depositar(1); });
                    Thread t2 = new Thread(() -> { for (int i = 0; i < 1000; i++) conta.depositar(1); });
                    t1.start(); t2.start();
                    t1.join(); t2.join();
                    System.out.println(conta.getSaldo());
                }
            }
            """;

    private static final String CONTA = """
            package contas;

            public class Conta {
                private int saldo = 0;

                public void depositar(int valor) {
                    saldo = saldo + valor;
                }

                public int getSaldo() {
                    return saldo;
                }
            }
            """;

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private EventoRepository eventos;
    @MockitoBean private LlmRouter llm;

    @BeforeEach
    void modeloSimulado() {
        when(llm.gerar(any(), anyBoolean())).thenReturn(new Resultado(
                new Resposta(FALA, "gemini", "simulado", 10, 10, "STOP", FALA),
                List.of(new Tentativa(1, "simulado", "gemini", "simulado", true, null, null, 5, 10, 10)), 5));
    }

    private ResultActions chamar(MockHttpServletRequestBuilder req, long aluno, Object corpo) throws Exception {
        req.header("Authorization", "Bearer " + ProjetoJavaControllerTest.token(aluno));
        if (corpo != null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo));
        return mvc.perform(req);
    }

    private JsonNode ler(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    /** Projeto Main.java + contas/Conta.java; devolve {projeto, idConta}. */
    private long[] projeto(long aluno) throws Exception {
        JsonNode p = ler(chamar(post(PROJETOS), aluno, Map.of("nome", "Banco")).andExpect(status().isCreated()));
        long id = p.get("id").asLong();
        chamar(put(PROJETOS + "/" + id + "/arquivos/" + p.get("arquivos").get(0).get("id").asLong()), aluno,
                Map.of("conteudo", MAIN)).andExpect(status().isOk());
        JsonNode conta = ler(chamar(post(PROJETOS + "/" + id + "/arquivos"), aluno,
                Map.of("nome", "contas/Conta.java", "conteudo", CONTA)).andExpect(status().isCreated()));
        return new long[] { id, conta.get("id").asLong() };
    }

    private JsonNode analisar(long aluno, long projeto) throws Exception {
        return ler(chamar(post(PROJETOS + "/" + projeto + "/analises"), aluno, Map.of("sessaoId", "s1"))
                .andExpect(status().isOk()));
    }

    private JsonNode alertaNaConta(JsonNode analise) {
        for (JsonNode a : analise.get("alertas")) {
            if ("contas/Conta.java".equals(a.get("arquivo").asText())) return a;
        }
        fail("sem alerta em contas/Conta.java: " + analise);
        return null;
    }

    private JsonNode abrir(long aluno, JsonNode analise, JsonNode alerta) throws Exception {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("submissaoId", analise.get("submissaoId").asLong());
        corpo.put("alertaId", alerta.get("id").asLong());
        return ler(chamar(post(CONVERSAS), aluno, corpo).andExpect(status().isOk()));
    }

    private String ultimoPrompt() {
        ArgumentCaptor<Requisicao> req = ArgumentCaptor.forClass(Requisicao.class);
        verify(llm, atLeastOnce()).gerar(req.capture(), anyBoolean());
        return req.getValue().instrucaoSistema();
    }

    @Test
    void abreConversaSobreOAlertaComAlunoDoToken() throws Exception {
        long[] p = projeto(3001);
        JsonNode analise = analisar(3001, p[0]);
        JsonNode alerta = alertaNaConta(analise);
        JsonNode c = abrir(3001, analise, alerta);

        assertEquals("ABERTA", c.get("estado").asText());
        assertEquals("TUTOR", c.get("mensagens").get(0).get("autor").asText());
        assertTrue(c.get("mensagens").get(0).get("texto").asText().startsWith("O que você espera"));
        assertEquals("contas/Conta.java", c.get("alerta").get("arquivo").asText());
        assertEquals(alerta.get("linha").asInt(), c.get("alerta").get("linha").asInt());

        String prompt = ultimoPrompt();
        assertTrue(prompt.contains("- Arquivo: contas/Conta.java"), "o prompt diz o arquivo do alerta");
        assertTrue(prompt.contains("// ===== Arquivo: contas/Conta.java ====="), "o prompt tem o projeto inteiro");
        assertTrue(prompt.contains("Executar 5 vezes"), "o prompt permite sugerir a execução");
        assertTrue(prompt.contains("acentuação correta"));

        // abrir de novo o mesmo alerta retoma a conversa
        assertEquals(c.get("conversaId").asLong(), abrir(3001, analise, alerta).get("conversaId").asLong());
    }

    @Test
    void mensagemDicaEEncerrar() throws Exception {
        long[] p = projeto(3002);
        JsonNode analise = analisar(3002, p[0]);
        long id = abrir(3002, analise, alertaNaConta(analise)).get("conversaId").asLong();

        JsonNode r = ler(chamar(post(CONVERSAS + "/" + id + "/mensagens"), 3002, Map.of("texto", "Acho que imprime 2000"))
                .andExpect(status().isOk()));
        assertEquals(3, r.get("mensagens").size());
        assertEquals("ALUNO", r.get("mensagens").get(1).get("autor").asText());

        r = ler(chamar(post(CONVERSAS + "/" + id + "/mensagens"), 3002, Map.of("tipo", "PEDIDO_DICA"))
                .andExpect(status().isOk()));
        assertEquals("PEDIDO_DICA", r.get("mensagens").get(3).get("tipo").asText());

        r = ler(chamar(post(CONVERSAS + "/" + id + "/encerrar"), 3002, null).andExpect(status().isOk()));
        assertEquals("ENCERRADA_PELO_ALUNO", r.get("estado").asText());
        chamar(post(CONVERSAS + "/" + id + "/mensagens"), 3002, Map.of("texto", "oi")).andExpect(status().isConflict());
    }

    @Test
    void conversaDeOutroAlunoResponde404() throws Exception {
        long[] p = projeto(3003);
        JsonNode analise = analisar(3003, p[0]);
        long id = abrir(3003, analise, alertaNaConta(analise)).get("conversaId").asLong();

        chamar(get(CONVERSAS + "/" + id), 3004, null).andExpect(status().isNotFound());
        chamar(post(CONVERSAS + "/" + id + "/mensagens"), 3004, Map.of("texto", "oi")).andExpect(status().isNotFound());
        Map<String, Object> corpo = Map.of("submissaoId", analise.get("submissaoId").asLong(),
                "alertaId", alertaNaConta(analise).get("id").asLong());
        chamar(post(CONVERSAS), 3004, corpo).andExpect(status().isNotFound());
    }

    @Test
    void novaAnaliseComOAlertaAindaLaLevaAConversaParaOCodigoNovo() throws Exception {
        long[] p = projeto(3005);
        JsonNode primeira = analisar(3005, p[0]);
        long id = abrir(3005, primeira, alertaNaConta(primeira)).get("conversaId").asLong();

        // muda o código sem corrigir (um comentário) e analisa de novo
        chamar(put(PROJETOS + "/" + p[0] + "/arquivos/" + p[1]), 3005,
                Map.of("conteudo", CONTA.replace("private int saldo = 0;", "private int saldo = 0; // tentativa 2")))
                .andExpect(status().isOk());
        JsonNode segunda = analisar(3005, p[0]);

        JsonNode c = ler(chamar(get(CONVERSAS + "/" + id), 3005, null).andExpect(status().isOk()));
        assertEquals("ABERTA", c.get("estado").asText());
        assertEquals(segunda.get("submissaoId").asLong(), c.get("submissaoId").asLong());
        assertEquals(alertaNaConta(segunda).get("id").asLong(), c.get("alertaId").asLong());
        JsonNode ultima = c.get("mensagens").get(c.get("mensagens").size() - 1);
        assertEquals("SISTEMA", ultima.get("autor").asText());
        assertTrue(ultima.get("texto").asText().contains("continua aparecendo"));

        chamar(post(CONVERSAS + "/" + id + "/mensagens"), 3005, Map.of("texto", "Mudei uma coisa")).andExpect(status().isOk());
        assertTrue(ultimoPrompt().contains("// tentativa 2"), "o tutor passa a ver o código novo");
        assertTrue(eventos.findAll().stream().anyMatch(e -> "CONVERSA_ATUALIZADA".equals(e.getTipo())
                && e.getConversaId() == id && "3005".equals(e.getAlunoId())));

        // reabrir pelo alerta novo retoma a mesma conversa
        assertEquals(id, abrir(3005, segunda, alertaNaConta(segunda)).get("conversaId").asLong());
    }

    @Test
    void novaAnaliseSemOAlertaEncerraComoResolvida() throws Exception {
        long[] p = projeto(3006);
        JsonNode primeira = analisar(3006, p[0]);
        long id = abrir(3006, primeira, alertaNaConta(primeira)).get("conversaId").asLong();

        chamar(put(PROJETOS + "/" + p[0] + "/arquivos/" + p[1]), 3006, Map.of("conteudo",
                CONTA.replace("public void depositar", "public synchronized void depositar")
                     .replace("public int getSaldo", "public synchronized int getSaldo"))).andExpect(status().isOk());
        analisar(3006, p[0]);

        JsonNode c = ler(chamar(get(CONVERSAS + "/" + id), 3006, null).andExpect(status().isOk()));
        assertEquals("RESOLVIDA", c.get("estado").asText());

        // explicação final: uma por conversa, sem chamar o modelo
        reset(llm);
        chamar(post(CONVERSAS + "/" + id + "/reflexao"), 3006, Map.of("texto", " ")).andExpect(status().isBadRequest());
        JsonNode r = ler(chamar(post(CONVERSAS + "/" + id + "/reflexao"), 3006,
                Map.of("texto", "As duas threads liam o mesmo saldo e uma soma se perdia; o synchronized deixa uma por vez."))
                .andExpect(status().isOk()));
        JsonNode ultima = r.get("mensagens").get(r.get("mensagens").size() - 1);
        assertEquals("REFLEXAO", ultima.get("tipo").asText());
        assertEquals("ALUNO", ultima.get("autor").asText());
        chamar(post(CONVERSAS + "/" + id + "/reflexao"), 3006, Map.of("texto", "de novo")).andExpect(status().isConflict());
        chamar(post(CONVERSAS + "/" + id + "/reflexao"), 3007, Map.of("texto", "outro aluno")).andExpect(status().isNotFound());
        verify(llm, never()).gerar(any(), anyBoolean());
    }

    @Test
    void reflexaoSoDepoisDeResolver() throws Exception {
        long[] p = projeto(3008);
        JsonNode analise = analisar(3008, p[0]);
        long id = abrir(3008, analise, alertaNaConta(analise)).get("conversaId").asLong();
        chamar(post(CONVERSAS + "/" + id + "/reflexao"), 3008, Map.of("texto", "cedo demais")).andExpect(status().isConflict());
    }

    @Test
    void promptTemAsRegrasNovas() throws Exception {
        long[] p = projeto(3009);
        JsonNode analise = analisar(3009, p[0]);
        abrir(3009, analise, alertaNaConta(analise));
        String prompt = ultimoPrompt();
        assertTrue(prompt.contains("Não cite as correções na mesma mensagem da explicação"), "B: nível 3 em dois tempos");
        assertTrue(prompt.contains("desenvolva a própria ideia"), "A: desenvolver a resposta parcial");
        assertTrue(prompt.contains("`synchronized (objeto) { ... }`"), "forma geral da sintaxe");
        assertTrue(prompt.contains("Só nomear um recurso"), "onde e por quê");
        // Revisão 4
        assertTrue(prompt.contains("no máximo uma vez na conversa"), "executar só uma vez");
        assertTrue(prompt.contains("nunca é FORA_DO_TEMA nem PEDIU_SOLUCAO"), "dúvida conceitual");
        assertTrue(prompt.contains("Nunca repita uma mensagem"), "sem mensagens repetidas");
        assertTrue(prompt.contains("garante a ordem em que as threads"), "equívocos comuns");
        assertTrue(prompt.contains("não peça que ele execute o programa antes"), "correção aceita sem executar antes");
        // Revisão 5
        assertTrue(prompt.contains("Nunca copie linhas do código"), "linhas numeradas, sem copiar");
        assertTrue(prompt.contains("essa parte é o aluno quem descobre"), "nível 3 só com a estratégia");
        assertTrue(prompt.contains("sem asteriscos"), "texto simples");
    }

    @Test
    void eventoUsaOAlunoDoToken() throws Exception {
        chamar(post("/api/java/eventos"), 3007, Map.of("sessaoId", "s9", "tipo", "ABRIU_CHAT", "detalhe", "teste"))
                .andExpect(status().isNoContent());
        assertTrue(eventos.findAll().stream().anyMatch(e -> "ABRIU_CHAT".equals(e.getTipo()) && "3007".equals(e.getAlunoId())));
    }

    @Test
    void semLoginResponde401() throws Exception {
        mvc.perform(post(CONVERSAS).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
