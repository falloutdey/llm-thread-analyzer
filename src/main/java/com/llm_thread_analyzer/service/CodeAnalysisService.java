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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;

@Service
public class CodeAnalysisService {

    // Intervalo entre chamadas ao Gemini para respeitar o rate limit do tier gratuito (~15 RPM)
    // 4500ms = ~13 requisições/minuto, com margem de segurança
    private static final long GEMINI_RATE_LIMIT_DELAY_MS = 4500;

    // O Semaphore(1) garante que apenas UMA thread faz a chamada ao LLM de cada vez,
    // independentemente de quantas requisições simultâneas o Spring Boot esteja a processar.
    private static final Semaphore GEMINI_SEMAPHORE = new Semaphore(1);

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

        final int TAMANHO_MAXIMO_CODIGO = 50_000;
        if (sourceCode.getContent().length() > TAMANHO_MAXIMO_CODIGO) {
            throw new RuntimeException(
                "Código demasiado longo para análise. Máximo permitido: " + TAMANHO_MAXIMO_CODIGO +
                " caracteres. Enviado: " + sourceCode.getContent().length() + " caracteres."
            );
        }

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("thread-analyzer-");
            String caminhoFicheiroClass = compilarCodigo(sourceCode, tempDir);

            List<ConcurrencyIssue> issues = spotBugsDetector.detectConcurrencyIssues(caminhoFicheiroClass);

            for (int i = 0; i < issues.size(); i++) {
                ConcurrencyIssue issue = issues.get(i);
                try {
                    // Adquirir o Semaphore antes de chamar o Gemini
                    // Garante exclusividade global mesmo com múltiplas threads simultâneas
                    GEMINI_SEMAPHORE.acquire();
                    try {
                        String explicacao = llmFeedbackService.gerarFeedbackDidatico(
                                issue.getMessage(),
                                sourceCode.getContent()
                        );
                        issue.setInterpretation(explicacao);
                    } finally {
                        // Sleep dentro do bloco finally: o delay é aplicado ANTES
                        // de libertar o semáforo, garantindo que a próxima thread só avança
                        // após o intervalo de rate limit, não apenas após a chamada terminar
                        if (i < issues.size() - 1) {
                            System.out.println("[CodeAnalysis] Aguardando " + GEMINI_RATE_LIMIT_DELAY_MS + "ms (rate limit)...");
                            Thread.sleep(GEMINI_RATE_LIMIT_DELAY_MS);
                        }
                        GEMINI_SEMAPHORE.release();
                    }
                } catch (LlmApiException e) {
                    // Falha do LLM marcada estruturalmente e não interrompe os outros issues.
                    System.err.println("[CodeAnalysis] Falha do LLM para issue #" + i + ": " + e.getMessage());
                    issue.setLlmError(true);
                    issue.setLlmErrorMessage(e.getMessage());
                } catch (InterruptedException ie) {
                    // Repõe o estado de interrupção da thread.
                    Thread.currentThread().interrupt();
                    break;
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
        } else {
            // Paths.get().getFileName() extrai apenas o nome final, descartando qualquer prefixo de diretório.
            nomeFicheiro = Paths.get(nomeFicheiro).getFileName().toString();

            if (!nomeFicheiro.endsWith(".java")) {
                nomeFicheiro += ".java";
            }
        }

        // Remoção da declaração package antes de salvar o ficheiro.
        String conteudoSanitizado = sourceCode.getContent()
                .replaceAll("(?m)^\\s*package\\s+[\\w.]+;\\s*\\n?", "");

        File sourceFile = new File(tempDir.toFile(), nomeFicheiro);

        //Escrita do ficheiro forçando UTF-8.
        Files.writeString(sourceFile.toPath(), conteudoSanitizado, java.nio.charset.StandardCharsets.UTF_8);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new RuntimeException("Compilador Java não encontrado. Certifique-se de estar a usar um JDK e não apenas um JRE.");
        }

        //errorStream também decodificado em UTF-8 para preservar mensagens acentuadas
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

        int result = compiler.run(null, null, errorStream,
                "-g", "-proc:none", "-encoding", "UTF-8",
                sourceFile.getAbsolutePath());

        if (result != 0) {
            // Decodifica os erros do compilador em UTF-8 para preservar mensagens acentuadas
            String errosCompilacao = errorStream.toString(java.nio.charset.StandardCharsets.UTF_8);
            throw new RuntimeException("ERRO_COMPILACAO:" + errosCompilacao);
        }

        // Verificação de estrutura válida após compilação bem-sucedida.
        File[] classesGeradas = tempDir.toFile().listFiles((dir, name) -> name.endsWith(".class"));
        if (classesGeradas == null || classesGeradas.length == 0) {
            throw new RuntimeException("ERRO_ESTRUTURA: O teu código compilou mas não gerou nenhuma classe analisável. " +
                    "Certifica-te de que o código contém pelo menos uma declaração 'class' concreta.");
        }

        // Como o ConcurrencyIssuesDetector já itera TODOS os .class do diretório,
        // retornamos o diretório, não um ficheiro .class específico.
        return tempDir.toAbsolutePath().toString();
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