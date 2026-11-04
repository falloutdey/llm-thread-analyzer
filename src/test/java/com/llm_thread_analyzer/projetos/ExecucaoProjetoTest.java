package com.llm_thread_analyzer.projetos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.llm_thread_analyzer.projetos.repository.ExecucaoJavaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Botão Executar: o programa do aluno roda num processo separado, isolado e com limites.
 * Limite de tempo reduzido (3 s) para o teste do deadlock não demorar.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:execucao-teste;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "auth.jwt-secret=" + ProjetoJavaControllerTest.SEGREDO,
        "gemini.api.key=teste",
        "java.execucao.limite-segundos=3",
        "java.execucao.max-caracteres-saida=5000"
})
@AutoConfigureMockMvc
class ExecucaoProjetoTest {

    private static final String URL = "/api/java/projetos";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private ExecucaoJavaRepository execucoes;

    private ResultActions chamar(MockHttpServletRequestBuilder req, long aluno, Object corpo) throws Exception {
        req.header("Authorization", "Bearer " + ProjetoJavaControllerTest.token(aluno));
        if (corpo != null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo));
        return mvc.perform(req);
    }

    private JsonNode ler(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    /** Projeto com os arquivos pedidos; devolve {idProjeto, idArquivo de cada nome}. */
    private Map<String, Long> projeto(long aluno, Map<String, String> arquivos) throws Exception {
        JsonNode p = ler(chamar(post(URL), aluno, Map.of("nome", "Projeto")).andExpect(status().isCreated()));
        Map<String, Long> ids = new LinkedHashMap<>();
        ids.put("#projeto", p.get("id").asLong());
        long main = p.get("arquivos").get(0).get("id").asLong();
        ids.put("Main.java", main);
        for (var e : arquivos.entrySet()) {
            if (e.getKey().equals("Main.java")) {
                chamar(put(URL + "/" + ids.get("#projeto") + "/arquivos/" + main), aluno, Map.of("conteudo", e.getValue()))
                        .andExpect(status().isOk());
            } else {
                JsonNode a = ler(chamar(post(URL + "/" + ids.get("#projeto") + "/arquivos"), aluno,
                        Map.of("nome", e.getKey(), "conteudo", e.getValue())).andExpect(status().isCreated()));
                ids.put(e.getKey(), a.get("id").asLong());
            }
        }
        return ids;
    }

    private ResultActions executar(long aluno, long projeto, Long arquivoAtivo) throws Exception {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("sessaoId", "teste");
        if (arquivoAtivo != null) corpo.put("arquivoAtivoId", arquivoAtivo);
        return chamar(post(URL + "/" + projeto + "/execucoes"), aluno, corpo);
    }

    /** Resposta com os campos da 1ª rodada copiados para o topo (a maioria dos testes executa 1 vez). */
    private JsonNode lerExec(ResultActions r) throws Exception {
        ObjectNode resposta = (ObjectNode) ler(r);
        JsonNode rodadas = resposta.get("rodadas");
        if (rodadas != null && rodadas.size() > 0) {
            rodadas.get(0).fields().forEachRemaining(c -> { if (!c.getKey().equals("ordem")) resposta.set(c.getKey(), c.getValue()); });
        } else {
            resposta.putNull("codigoSaida");
        }
        return resposta;
    }

    private ResultActions executarVezes(long aluno, long projeto, Long arquivoAtivo, int vezes) throws Exception {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("sessaoId", "teste");
        if (arquivoAtivo != null) corpo.put("arquivoAtivoId", arquivoAtivo);
        corpo.put("repeticoes", vezes);
        return chamar(post(URL + "/" + projeto + "/execucoes"), aluno, corpo);
    }

    private JsonNode executarOk(long aluno, Map<String, String> arquivos) throws Exception {
        Map<String, Long> ids = projeto(aluno, arquivos);
        return lerExec(executar(aluno, ids.get("#projeto"), ids.get("Main.java")).andExpect(status().isOk()));
    }

    private static String main(String corpo) {
        return "public class Main {\n    public static void main(String[] args) throws Exception {\n" + corpo + "\n    }\n}\n";
    }

    @Test
    void executaEMostraASaida() throws Exception {
        JsonNode r = executarOk(2001, Map.of("Main.java", main("System.out.println(\"Olá, threads!\");")));
        assertTrue(r.get("compilou").asBoolean());
        assertEquals("Olá, threads!\n", r.get("saida").asText());
        assertEquals(0, r.get("codigoSaida").asInt());
        assertTrue(r.get("interrupcao").isNull());
        assertFalse(r.get("saidaCortada").asBoolean());
        assertTrue(r.get("duracaoMs").asLong() > 0);
        assertEquals(3, r.get("limiteSegundos").asInt());
        assertEquals("Main.java", r.get("arquivoExecutado").asText());
        assertFalse(r.get("saida").asText().contains("WARNING"), "os avisos da JVM sobre o Security Manager não aparecem");
    }

    @Test
    void threadsEExecutorServiceFuncionam() throws Exception {
        JsonNode r = executarOk(2002, Map.of("Main.java", """
                import java.util.concurrent.*;
                import java.util.concurrent.atomic.AtomicInteger;

                public class Main {
                    public static void main(String[] args) throws Exception {
                        AtomicInteger total = new AtomicInteger();
                        ExecutorService pool = Executors.newFixedThreadPool(4);
                        for (int i = 0; i < 100; i++) pool.submit(total::incrementAndGet);
                        pool.shutdown();
                        pool.awaitTermination(2, TimeUnit.SECONDS);
                        Thread t = new Thread(() -> System.out.println("na thread " + Thread.currentThread().getName()), "T1");
                        t.start();
                        t.join();
                        System.out.println("total=" + total.get());
                    }
                }
                """));
        String saida = r.get("saida").asText();
        assertEquals(0, r.get("codigoSaida").asInt(), saida);
        assertTrue(saida.contains("na thread T1"), saida);
        assertTrue(saida.contains("total=100"), saida);
    }

    @Test
    void deadlockEInterrompidoPeloLimiteDeTempoEMostraASaidaParcial() throws Exception {
        JsonNode r = executarOk(2003, Map.of("Main.java", main("""
                        Object a = new Object(), b = new Object();
                        System.out.println("antes do deadlock");
                        Thread t1 = new Thread(() -> { synchronized (a) { dormir(); synchronized (b) { } } });
                        Thread t2 = new Thread(() -> { synchronized (b) { dormir(); synchronized (a) { } } });
                        t1.start(); t2.start();
                        t1.join(); t2.join();
                        System.out.println("nunca chega aqui");
                    }
                    static void dormir() {
                        try { Thread.sleep(200); } catch (InterruptedException e) { }""")));
        assertEquals("TEMPO", r.get("interrupcao").asText());
        assertTrue(r.get("codigoSaida").isNull());
        assertEquals("antes do deadlock\n", r.get("saida").asText());
        assertTrue(r.get("duracaoMs").asLong() >= 3000 && r.get("duracaoMs").asLong() < 6000, r.toString());
    }

    @Test
    void excecaoNaoTratadaTerminaComErro() throws Exception {
        JsonNode r = executarOk(2004, Map.of("Main.java", main("int[] v = new int[2];\nSystem.out.println(v[5]);")));
        assertNotEquals(0, r.get("codigoSaida").asInt());
        assertTrue(r.get("saida").asText().contains("ArrayIndexOutOfBoundsException"), r.get("saida").asText());
        assertTrue(r.get("interrupcao").isNull());
    }

    @Test
    void systemExitDevolveOCodigo() throws Exception {
        JsonNode r = executarOk(2005, Map.of("Main.java", main("System.out.println(\"saindo\");\nSystem.exit(3);")));
        assertEquals(3, r.get("codigoSaida").asInt());
        assertEquals("saindo\n", r.get("saida").asText());
    }

    @Test
    void mainDentroDeUmPackage() throws Exception {
        Map<String, String> arquivos = new LinkedHashMap<>();
        arquivos.put("Main.java", "public class Main { }\n");
        arquivos.put("banco/App.java", """
                package banco;

                public class App {
                    public static void main(String... args) {
                        System.out.println("rodou " + App.class.getName());
                    }
                }
                """);
        Map<String, Long> ids = projeto(2006, arquivos);
        // o arquivo aberto (Main.java) não tem main: roda o único main do projeto
        JsonNode r = lerExec(executar(2006, ids.get("#projeto"), ids.get("Main.java")).andExpect(status().isOk()));
        assertEquals("banco/App.java", r.get("arquivoExecutado").asText());
        assertEquals("banco.App", r.get("classeExecutada").asText());
        assertEquals("rodou banco.App\n", r.get("saida").asText());
    }

    @Test
    void comDoisMainsRodaODoArquivoAberto() throws Exception {
        Map<String, String> arquivos = new LinkedHashMap<>();
        arquivos.put("Main.java", main("System.out.println(\"Main\");"));
        arquivos.put("Outro.java", "public class Outro { public static void main(String[] a) { System.out.println(\"Outro\"); } }\n");
        arquivos.put("util/Ajuda.java", "package util;\n\npublic class Ajuda { }\n");
        Map<String, Long> ids = projeto(2007, arquivos);
        long p = ids.get("#projeto");
        assertEquals("Outro\n", lerExec(executar(2007, p, ids.get("Outro.java"))).get("saida").asText());
        assertEquals("Main\n", lerExec(executar(2007, p, ids.get("Main.java"))).get("saida").asText());
        // aberto um arquivo sem main e há dois mains: pede para abrir o que quer executar
        executar(2007, p, ids.get("util/Ajuda.java"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.containsString("Abra no editor")));
    }

    @Test
    void semMainRespondeComExplicacao() throws Exception {
        Map<String, Long> ids = projeto(2008, Map.of("Main.java", "public class Main { void rodar() { } }\n"));
        executar(2008, ids.get("#projeto"), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.containsString("public static void main")));
    }

    @Test
    void erroDeCompilacaoNaoExecuta() throws Exception {
        JsonNode r = executarOk(2009, Map.of("Main.java", "public class Main {\n    int x = \n}\n"));
        assertFalse(r.get("compilou").asBoolean());
        assertTrue(r.get("erroCompilacao").asText().contains("Main.java:"), r.get("erroCompilacao").asText());
        assertTrue(r.get("codigoSaida").isNull());
    }

    @Test
    void programaNaoLeVariaveisDeAmbienteNemArquivos() throws Exception {
        JsonNode env = executarOk(2010, Map.of("Main.java", main("System.out.println(System.getenv(\"GEMINI_API_KEY\"));")));
        assertNotEquals(0, env.get("codigoSaida").asInt());
        assertTrue(env.get("saida").asText().contains("AccessControlException"), env.get("saida").asText());
        assertTrue(env.get("aviso").asText().contains("Por segurança"));

        JsonNode arq = executarOk(2011, Map.of("Main.java",
                main("System.out.println(java.nio.file.Files.readString(java.nio.file.Path.of(\"application.properties\")));")));
        assertNotEquals(0, arq.get("codigoSaida").asInt());
        assertTrue(arq.get("saida").asText().contains("AccessControlException"), arq.get("saida").asText());

        JsonNode exec = executarOk(2012, Map.of("Main.java", main("new ProcessBuilder(\"ls\").start();")));
        assertTrue(exec.get("saida").asText().contains("AccessControlException"), exec.get("saida").asText());
    }

    @Test
    void saidaGrandeDemaisECortada() throws Exception {
        JsonNode r = executarOk(2013, Map.of("Main.java", main("while (true) System.out.println(\"repetindo...\");")));
        assertEquals("SAIDA", r.get("interrupcao").asText());
        assertTrue(r.get("saidaCortada").asBoolean());
        int tamanho = r.get("saida").asText().length();
        assertTrue(tamanho > 4500 && tamanho <= 5000, "saída cortada perto do limite: " + tamanho);
        assertTrue(r.get("duracaoMs").asLong() < 3000, "deve parar antes do limite de tempo: " + r.get("duracaoMs"));
    }

    @Test
    void tecladoFechadoTemAviso() throws Exception {
        JsonNode r = executarOk(2014, Map.of("Main.java",
                main("java.util.Scanner s = new java.util.Scanner(System.in);\nSystem.out.println(s.nextInt());")));
        assertNotEquals(0, r.get("codigoSaida").asInt());
        assertTrue(r.get("aviso").asText().contains("teclado"), r.toString());
    }

    @Test
    void cadaExecucaoFicaRegistradaENumerada() throws Exception {
        Map<String, Long> ids = projeto(2015, Map.of("Main.java", main("System.out.println(1);")));
        long p = ids.get("#projeto");
        assertEquals(1, lerExec(executar(2015, p, null)).get("numeroExecucao").asInt());
        assertEquals(2, lerExec(executar(2015, p, null)).get("numeroExecucao").asInt());
        var registros = execucoes.findAll().stream().filter(e -> e.getIdEstudante() == 2015L).toList();
        assertEquals(2, registros.size());
        assertTrue(registros.get(0).getCodigo().contains("System.out.println(1);"));
        assertEquals("1\n", registros.get(0).getSaida());
        assertEquals("teste", registros.get(0).getSessaoId());
    }

    @Test
    void projetoDeOutroAlunoResponde404() throws Exception {
        Map<String, Long> ids = projeto(2016, Map.of());
        executar(2017, ids.get("#projeto"), null).andExpect(status().isNotFound());
    }

    @Test
    void executarCincoVezesDevolveCincoRodadasRegistradasNoMesmoGrupo() throws Exception {
        Map<String, Long> ids = projeto(2018, Map.of("Main.java", main("System.out.println(\"oi\");")));
        long p = ids.get("#projeto");
        JsonNode r = ler(executarVezes(2018, p, null, 5).andExpect(status().isOk()));
        assertEquals(5, r.get("repeticoesPedidas").asInt());
        assertEquals(5, r.get("rodadas").size());
        for (int i = 0; i < 5; i++) {
            JsonNode rodada = r.get("rodadas").get(i);
            assertEquals(i + 1, rodada.get("ordem").asInt());
            assertEquals("oi\n", rodada.get("saida").asText());
            assertEquals(0, rodada.get("codigoSaida").asInt());
        }
        assertEquals(1, r.get("numeroExecucao").asInt());
        // o clique seguinte é a execução 2, mesmo depois de 5 rodadas
        assertEquals(2, lerExec(executar(2018, p, null)).get("numeroExecucao").asInt());

        var registros = execucoes.findAll().stream().filter(e -> e.getIdEstudante() == 2018L).toList();
        assertEquals(6, registros.size());
        var doGrupo = registros.stream().filter(e -> e.getGrupo().equals(r.get("grupo").asText())).toList();
        assertEquals(5, doGrupo.size());
        assertTrue(doGrupo.stream().allMatch(e -> e.getRepeticoesPedidas() == 5 && e.getNumeroExecucao() == 1));
        assertEquals(java.util.Set.of(1, 2, 3, 4, 5),
                doGrupo.stream().map(e -> e.getOrdemNoGrupo()).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void cincoVezesParaNaPrimeiraInterrompidaPorTempo() throws Exception {
        Map<String, Long> ids = projeto(2019, Map.of("Main.java", main("System.out.println(\"parado\");\nwhile (true) Thread.sleep(50);")));
        JsonNode r = ler(executarVezes(2019, ids.get("#projeto"), null, 5).andExpect(status().isOk()));
        assertEquals(5, r.get("repeticoesPedidas").asInt());
        assertEquals(1, r.get("rodadas").size(), "não deve rodar as outras 4 depois de um limite de tempo");
        assertEquals("TEMPO", r.get("rodadas").get(0).get("interrupcao").asText());
        assertEquals("parado\n", r.get("rodadas").get(0).get("saida").asText());
    }

    @Test
    void repeticoesForaDoLimiteRespondem400() throws Exception {
        Map<String, Long> ids = projeto(2020, Map.of("Main.java", main("System.out.println(1);")));
        executarVezes(2020, ids.get("#projeto"), null, 6).andExpect(status().isBadRequest());
        executarVezes(2020, ids.get("#projeto"), null, 0).andExpect(status().isBadRequest());
    }

    @Test
    void erroDeCompilacaoNaoTemRodadas() throws Exception {
        Map<String, Long> ids = projeto(2021, Map.of("Main.java", "public class Main {\n    int x = \n}\n"));
        JsonNode r = ler(executarVezes(2021, ids.get("#projeto"), null, 5).andExpect(status().isOk()));
        assertFalse(r.get("compilou").asBoolean());
        assertEquals(0, r.get("rodadas").size());
    }

    @Test
    void semLoginResponde401() throws Exception {
        mvc.perform(post(URL + "/1/execucoes")).andExpect(status().isUnauthorized());
    }
}
