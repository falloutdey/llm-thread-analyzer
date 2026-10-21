package com.llm_thread_analyzer.service;

import com.llm_thread_analyzer.model.ConcurrencyIssue;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bateria de cobertura dos erros de threads: cada caso é compilado e passa pelo SpotBugs
 * e pelo detector do código-fonte, exatamente como no endpoint /analisar (sem o LLM).
 *
 * com-erro.txt — cada caso tem de gerar o tipo de alerta indicado no cabeçalho.
 * sem-erro.txt — código correto: não pode gerar nenhum alerta (controlo de falsos positivos).
 *
 * Formato dos ficheiros: "### <nome> <Classe> <tipoEsperado>" seguido do código da classe.
 */
class CoberturaThreadsTest {

    private final ConcurrencyIssuesDetector spotBugsDetector = new ConcurrencyIssuesDetector();
    private final SourceConcurrencyDetector sourceDetector = new SourceConcurrencyDetector();

    @TestFactory
    Stream<DynamicTest> casosComErroSaoDetetados() throws IOException {
        return carregarCasos("/casos-threads/com-erro.txt").stream().map(caso -> DynamicTest.dynamicTest(caso.nome(), () -> {
            List<ConcurrencyIssue> issues = analisar(caso);
            assertTrue(issues.stream().anyMatch(i -> i.getType().equals(caso.tipoEsperado())),
                    () -> caso.nome() + ": esperado " + caso.tipoEsperado() + ", obtido " + descrever(issues));
        }));
    }

    @TestFactory
    Stream<DynamicTest> casosCorretosNaoGeramAlertas() throws IOException {
        return carregarCasos("/casos-threads/sem-erro.txt").stream().map(caso -> DynamicTest.dynamicTest(caso.nome(), () -> {
            List<ConcurrencyIssue> issues = analisar(caso);
            assertTrue(issues.isEmpty(), () -> caso.nome() + ": falso positivo " + descrever(issues));
        }));
    }

    private List<ConcurrencyIssue> analisar(Caso caso) throws IOException {
        Path diretorio = Files.createTempDirectory("cobertura-threads-");
        try {
            Path fonte = diretorio.resolve(caso.classe() + ".java");
            Files.writeString(fonte, caso.codigo(), StandardCharsets.UTF_8);

            ByteArrayOutputStream erros = new ByteArrayOutputStream();
            int resultado = ToolProvider.getSystemJavaCompiler().run(null, null, erros,
                    "-g", "-proc:none", "-encoding", "UTF-8", fonte.toString());
            assertEquals(0, resultado, () -> caso.nome() + " não compilou:\n" + erros.toString(StandardCharsets.UTF_8));

            return SourceConcurrencyDetector.mesclar(
                    spotBugsDetector.detectConcurrencyIssues(diretorio.toString()),
                    sourceDetector.detectConcurrencyIssues(diretorio.toString()));
        } finally {
            try (Stream<Path> arquivos = Files.walk(diretorio)) {
                arquivos.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
    }

    private static String descrever(List<ConcurrencyIssue> issues) {
        return issues.stream().map(i -> i.getType() + "@" + i.getLineNumber()).collect(Collectors.joining(", ", "[", "]"));
    }

    private record Caso(String nome, String classe, String tipoEsperado, String codigo) {}

    private static List<Caso> carregarCasos(String recurso) throws IOException {
        String conteudo;
        try (InputStream entrada = CoberturaThreadsTest.class.getResourceAsStream(recurso)) {
            if (entrada == null) throw new IOException("Recurso não encontrado: " + recurso);
            conteudo = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        }

        List<Caso> casos = new ArrayList<>();
        String[] cabecalho = null;
        StringBuilder codigo = new StringBuilder();
        for (String linha : conteudo.split("\\R")) {
            if (linha.startsWith("### ")) {
                if (cabecalho != null) casos.add(new Caso(cabecalho[1], cabecalho[2], cabecalho[3], codigo.toString()));
                cabecalho = linha.split("\\s+");
                codigo.setLength(0);
            } else {
                codigo.append(linha).append('\n');
            }
        }
        if (cabecalho != null) casos.add(new Caso(cabecalho[1], cabecalho[2], cabecalho[3], codigo.toString()));
        return casos;
    }
}
