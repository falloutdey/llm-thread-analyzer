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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Análise do projeto inteiro: vários arquivos, packages e classes que se usam entre arquivos.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:analise-projeto-teste;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "auth.jwt-secret=" + ProjetoJavaControllerTest.SEGREDO,
        "gemini.api.key=teste"
})
@AutoConfigureMockMvc
class AnaliseProjetoTest {

    private static final String URL = "/api/java/projetos";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    private ResultActions chamar(MockHttpServletRequestBuilder req, long aluno, Object corpo) throws Exception {
        req.header("Authorization", "Bearer " + ProjetoJavaControllerTest.token(aluno));
        if (corpo != null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo));
        return mvc.perform(req);
    }

    private JsonNode ler(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    /** Cria o projeto e deixa os arquivos com o conteúdo pedido (o Main.java inicial é sobrescrito). */
    private long projeto(long aluno, Map<String, String> arquivos) throws Exception {
        JsonNode p = ler(chamar(post(URL), aluno, Map.of("nome", "Projeto")).andExpect(status().isCreated()));
        long id = p.get("id").asLong();
        long main = p.get("arquivos").get(0).get("id").asLong();
        for (var e : arquivos.entrySet()) {
            if (e.getKey().equals("Main.java")) {
                chamar(put(URL + "/" + id + "/arquivos/" + main), aluno, Map.of("conteudo", e.getValue()))
                        .andExpect(status().isOk());
            } else {
                chamar(post(URL + "/" + id + "/arquivos"), aluno, Map.of("nome", e.getKey(), "conteudo", e.getValue()))
                        .andExpect(status().isCreated());
            }
        }
        return id;
    }

    private JsonNode analisar(long aluno, long projeto) throws Exception {
        return ler(chamar(post(URL + "/" + projeto + "/analises"), aluno, Map.of("sessaoId", "teste"))
                .andExpect(status().isOk()));
    }

    // Main.java cria as threads; a escrita sem coordenação está em outro arquivo, noutro package.
    private static final String MAIN_USA_CONTA = """
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

    @Test
    void compilaClassesDeArquivosEPackagesDiferentes() throws Exception {
        Map<String, String> arquivos = new HashMap<>();
        arquivos.put("Main.java", MAIN_USA_CONTA);
        arquivos.put("contas/Conta.java", CONTA);
        JsonNode r = analisar(1001, projeto(1001, arquivos));
        assertTrue(r.get("compilou").asBoolean(), "deveria compilar: " + r.get("erroCompilacao"));
    }

    @Test
    void alertaApontaOArquivoDaClasseUsadaPelasThreads() throws Exception {
        Map<String, String> arquivos = new HashMap<>();
        arquivos.put("Main.java", MAIN_USA_CONTA);
        arquivos.put("contas/Conta.java", CONTA);
        JsonNode r = analisar(1002, projeto(1002, arquivos));
        assertTrue(r.get("compilou").asBoolean());
        boolean naConta = false;
        for (JsonNode a : r.get("alertas")) {
            assertNotNull(a.get("arquivo").textValue(), "todo alerta do projeto indica o arquivo: " + a);
            if (a.get("arquivo").asText().equals("contas/Conta.java") && a.get("linha").asInt() == 7) naConta = true;
        }
        assertTrue(naConta, "esperava alerta em contas/Conta.java, linha 7: " + r.get("alertas"));
    }

    @Test
    void packageQueNaoBateComAPastaViraErroDeCompilacao() throws Exception {
        Map<String, String> arquivos = new HashMap<>();
        arquivos.put("Main.java", "public class Main { public static void main(String[] a) {} }\n");
        arquivos.put("contas/Conta.java", "public class Conta {}\n");
        JsonNode r = analisar(1003, projeto(1003, arquivos));
        assertFalse(r.get("compilou").asBoolean());
        assertTrue(r.get("erroCompilacao").asText().contains("package contas;"), r.get("erroCompilacao").asText());
    }

    @Test
    void erroDeCompilacaoMostraOCaminhoNoProjeto() throws Exception {
        Map<String, String> arquivos = new HashMap<>();
        arquivos.put("Main.java", "public class Main { public static void main(String[] a) {} }\n");
        arquivos.put("contas/Conta.java", "package contas;\n\npublic class Conta {\n    int x = \n}\n");
        JsonNode r = analisar(1004, projeto(1004, arquivos));
        assertFalse(r.get("compilou").asBoolean());
        String erro = r.get("erroCompilacao").asText();
        assertTrue(erro.contains("contas/Conta.java:5"), erro);
        assertFalse(erro.contains("thread-analyzer-projeto-"), "não deve mostrar a pasta temporária: " + erro);
    }

    @Test
    void arquivoEmPastaJaNasceComALinhaPackage() throws Exception {
        long id = ler(chamar(post(URL), 1005, Map.of("nome", "Packages")).andExpect(status().isCreated())).get("id").asLong();
        chamar(post(URL + "/" + id + "/arquivos"), 1005, Map.of("nome", "banco/modelo/Cliente.java"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("banco/modelo/Cliente.java"))
                .andExpect(jsonPath("$.conteudo", startsWith("package banco.modelo;")))
                .andExpect(jsonPath("$.conteudo", containsString("public class Cliente")));
        // o projeto com o Main.java padrão e o arquivo no package compila
        assertTrue(analisar(1005, id).get("compilou").asBoolean());
    }

    @Test
    void analiseDeProjetoDeOutroAlunoResponde404() throws Exception {
        long id = projeto(1006, Map.of());
        chamar(post(URL + "/" + id + "/analises"), 1007, Map.of()).andExpect(status().isNotFound());
    }

    @Test
    void segundaAnaliseRegistraAlertaResolvido() throws Exception {
        Map<String, String> arquivos = new HashMap<>();
        arquivos.put("Main.java", MAIN_USA_CONTA);
        arquivos.put("contas/Conta.java", CONTA);
        long id = projeto(1008, arquivos);
        JsonNode primeira = analisar(1008, id);
        assertTrue(primeira.get("alertas").size() > 0);
        assertEquals(1, primeira.get("numeroTentativa").asInt());

        // corrige: método sincronizado
        JsonNode p = ler(chamar(get(URL + "/" + id), 1008, null));
        long conta = 0;
        for (JsonNode a : p.get("arquivos")) if (a.get("nome").asText().equals("contas/Conta.java")) conta = a.get("id").asLong();
        chamar(put(URL + "/" + id + "/arquivos/" + conta), 1008, Map.of("conteudo",
                CONTA.replace("public void depositar", "public synchronized void depositar")
                     .replace("public int getSaldo", "public synchronized int getSaldo")))
                .andExpect(status().isOk());

        JsonNode segunda = analisar(1008, id);
        assertEquals(2, segunda.get("numeroTentativa").asInt());
        assertTrue(segunda.get("resolvidosDesdeAnterior").size() > 0, "esperava alerta resolvido: " + segunda);
    }
}
