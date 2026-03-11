package com.llm_thread_analyzer.service;

import com.llm_thread_analyzer.model.CodeAnalysisResults;
import com.llm_thread_analyzer.model.ConcurrencyIssue;
import com.llm_thread_analyzer.model.SourceCode;
import com.llm_thread_analyzer.service.LLMFeedbackService.LlmApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CodeAnalysisService {

    // Intervalo entre chamadas ao Gemini para respeitar o rate limit do tier gratuito (~15 RPM)
    // 4500ms = ~13 requisições/minuto, com margem de segurança
    private static final long GEMINI_RATE_LIMIT_DELAY_MS = 4500;

    @Autowired
    private ConcurrencyIssuesDetector spotBugsDetector;

    @Autowired
    private LLMFeedbackService llmFeedbackService;

    /**
     * Analisa um único ficheiro de código fonte.
     * Usado pelo endpoint POST /api/files/analisar
     */
    public CodeAnalysisResults analisarCodigo(SourceCode sourceCode) {
        if (sourceCode == null || sourceCode.getContent() == null || sourceCode.getContent().trim().isEmpty()) {
            throw new RuntimeException("O código fonte enviado está vazio ou num formato inválido.");
        }

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("thread-analyzer-");
            String caminhoFicheiroClass = compilarCodigo(sourceCode, tempDir);

            List<ConcurrencyIssue> issues = spotBugsDetector.detectConcurrencyIssues(caminhoFicheiroClass);

            for (int i = 0; i < issues.size(); i++) {
                ConcurrencyIssue issue = issues.get(i);
                try {
                    String explicacao = llmFeedbackService.gerarFeedbackDidatico(
                            issue.getMessage(),
                            sourceCode.getContent()
                    );
                    issue.setInterpretation(explicacao);
                } catch (LlmApiException e) {
                    // Falha do LLM é marcada estruturalmente na issue
                    // O processamento dos outros issues do mesmo ficheiro continua normalmente
                    System.err.println("[CodeAnalysis] Falha do LLM para issue #" + i + ": " + e.getMessage());
                    issue.setLlmError(true);
                    issue.setLlmErrorMessage(e.getMessage());
                }

                // Aguardar entre chamadas ao Gemini para evitar 429 Too Many Requests
                // Aplicado após cada issue, exceto o último
                if (i < issues.size() - 1) {
                    try {
                        System.out.println("[CodeAnalysis] Aguardando " + GEMINI_RATE_LIMIT_DELAY_MS + "ms antes da próxima chamada ao Gemini...");
                        Thread.sleep(GEMINI_RATE_LIMIT_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            return new CodeAnalysisResults(issues);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro interno durante a análise do código: " + e.getMessage());
        } finally {
            // Limpeza garantida do diretório temporário após o SpotBugs terminar
            // Evita acumulação de centenas de diretórios durante a bateria de testes JCB
            if (tempDir != null) {
                deletarDiretorioTemporario(tempDir);
            }
        }
    }

    /**
     * Analisa uma lista de ficheiros em sequência.
     * Usado pelo endpoint POST /api/files/analisar/batch
     * Permite submeter toda a bateria JCB numa única chamada ao Postman/script.
     *
     * Erros individuais por ficheiro são registados mas não interrompem o processamento dos restantes.
     */
    public List<CodeAnalysisResults> analisarBatch(List<SourceCode> codigos) {
        List<CodeAnalysisResults> resultados = new ArrayList<>();

        for (int i = 0; i < codigos.size(); i++) {
            SourceCode sc = codigos.get(i);
            System.out.println("\n[Batch] A processar ficheiro " + (i + 1) + "/" + codigos.size() + ": " + sc.getFileName());
            try {
                CodeAnalysisResults resultado = analisarCodigo(sc);
                resultados.add(resultado);
            } catch (Exception e) {
                // Ficheiro com erro (ex: erro de compilação) — adiciona resultado vazio com anotação do erro
                System.err.println("[Batch] Erro ao processar ficheiro '" + sc.getFileName() + "': " + e.getMessage());
                CodeAnalysisResults resultadoErro = new CodeAnalysisResults();
                resultadoErro.setBatchError(true);
                resultadoErro.setBatchErrorMessage(e.getMessage());
                resultadoErro.setFileName(sc.getFileName());
                resultados.add(resultadoErro);
            }
        }

        System.out.println("\n[Batch] Processamento concluído. " + resultados.size() + " ficheiros analisados.");
        return resultados;
    }

    private String compilarCodigo(SourceCode sourceCode, Path tempDir) throws IOException {
        String nomeFicheiro = sourceCode.getFileName();
        if (nomeFicheiro == null || nomeFicheiro.isEmpty()) {
            nomeFicheiro = "Main.java";
        } else if (!nomeFicheiro.endsWith(".java")) {
            nomeFicheiro += ".java";
        }

        File sourceFile = new File(tempDir.toFile(), nomeFicheiro);
        try (FileWriter writer = new FileWriter(sourceFile)) {
            writer.write(sourceCode.getContent());
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new RuntimeException("Compilador Java não encontrado. Certifique-se de estar a usar um JDK e não apenas um JRE.");
        }

        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        int result = compiler.run(null, null, errorStream, "-g", sourceFile.getAbsolutePath());

        if (result != 0) {
            String errosCompilacao = new String(errorStream.toByteArray());
            throw new RuntimeException("ERRO_COMPILACAO:" + errosCompilacao);
        }

        String nomeFicheiroClass = nomeFicheiro.replace(".java", ".class");
        File classFile = new File(tempDir.toFile(), nomeFicheiroClass);

        if (!classFile.exists()) {
            throw new RuntimeException("O ficheiro .class não foi gerado após a compilação.");
        }

        return classFile.getAbsolutePath();
    }

    private void deletarDiretorioTemporario(Path tempDir) {
        try {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            System.out.println("[CodeAnalysis] Diretório temporário removido: " + tempDir);
        } catch (IOException e) {
            System.err.println("[CodeAnalysis] Aviso: não foi possível remover o diretório temporário " + tempDir + ": " + e.getMessage());
        }
    }
}