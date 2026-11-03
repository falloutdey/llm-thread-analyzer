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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CodeAnalysisService {

    // Intervalo mínimo global entre o início de chamadas ao Gemini, inclusive
    // quando os alertas vêm de requisições HTTP diferentes.
    private static final long GEMINI_RATE_LIMIT_DELAY_MS = 4500;

    private static final Pattern PADRAO_PACKAGE = Pattern.compile("(?m)^\\s*package\\s+[\\w.]+;\\s*\\n?");

    // Declaração de package para conferir com a pasta do arquivo, na análise de projeto.
    private static final Pattern DECLARACAO_PACKAGE = Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;");

    // Cada pasta do caminho e o nome da classe: identificadores Java simples.
    private static final Pattern CAMINHO_ARQUIVO = Pattern.compile("([A-Za-z_$][A-Za-z0-9_$]*/)*[A-Za-z_$][A-Za-z0-9_$]*\\.java");

    static final int MAX_CARACTERES_PROJETO = 200_000;

    // O Semaphore(1) garante que apenas UMA thread faz a chamada ao LLM de cada vez,
    // independentemente de quantas requisições simultâneas o Spring Boot esteja a processar.
    private static final Semaphore GEMINI_SEMAPHORE = new Semaphore(1);
    private static long lastGeminiCallStartedAtNanos;
    private static boolean hasGeminiCallStarted;

    @Autowired
    private ConcurrencyIssuesDetector spotBugsDetector;

    @Autowired
    private SourceConcurrencyDetector sourceDetector;

    @Autowired
    private LLMFeedbackService llmFeedbackService;

    /**
     * Analisa um único ficheiro de código fonte.
     * Usado pelo endpoint POST /api/files/analisar
     */
    public CodeAnalysisResults analisarCodigo(SourceCode sourceCode) {
        List<ConcurrencyIssue> issues = detectarProblemas(sourceCode);

        for (int issueIndex = 0; issueIndex < issues.size(); issueIndex++) {
            ConcurrencyIssue issue = issues.get(issueIndex);
            boolean semaphoreAcquired = false;
            try {
                GEMINI_SEMAPHORE.acquire();
                semaphoreAcquired = true;
                aguardarIntervaloMinimoEntreChamadasGemini();

                String explicacao = llmFeedbackService.gerarFeedbackDidatico(
                        issue.getMessage(),
                        sourceCode.getContent(),
                        issue.getLineNumber(),
                        null  // histórico: null por ora — será passado quando o banco estiver conectado
                );
                issue.setInterpretation(explicacao);
            } catch (LlmApiException e) {
                // Falha do LLM marcada estruturalmente e não interrompe os outros issues.
                System.err.println("[CodeAnalysis] Falha do LLM para issue #" + issueIndex + ": " + e.getMessage());
                issue.setLlmError(true);
                issue.setLlmErrorMessage(e.getMessage());
            } catch (InterruptedException ie) {
                // Repõe o estado de interrupção da thread.
                Thread.currentThread().interrupt();
                break;
            } finally {
                // A liberação fica garantida mesmo se o intervalo ou a chamada ao LLM
                // for interrompida, evitando bloquear permanentemente as análises seguintes.
                if (semaphoreAcquired) {
                    GEMINI_SEMAPHORE.release();
                }
            }
        }

        return new CodeAnalysisResults(issues);
    }

    /**
     * Só a parte estática: valida, compila e roda SpotBugs + regras no código-fonte.
     * Não chama o LLM. Usado pelo tutor (POST /api/tutor/submissoes), que chama o LLM
     * só quando o aluno abre a conversa de um alerta.
     *
     * Lança RuntimeException com prefixo ERRO_COMPILACAO: ou ERRO_ESTRUTURA: como antes.
     */
    public List<ConcurrencyIssue> detectarProblemas(SourceCode sourceCode) {
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

            return SourceConcurrencyDetector.mesclar(
                    spotBugsDetector.detectConcurrencyIssues(caminhoFicheiroClass),
                    detetarNoCodigoFonte(caminhoFicheiroClass)
            );

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
     * Análise de um projeto com vários arquivos, que podem estar em pastas (packages).
     *
     * Os arquivos são compilados juntos, mantendo a declaração package (que precisa bater com a
     * pasta), e os alertas vêm com o arquivo de origem em getFileName(). Não chama o LLM.
     * Erros seguem o padrão de detectarProblemas: prefixo ERRO_COMPILACAO: ou ERRO_ESTRUTURA:.
     *
     * @param arquivos nome = caminho no projeto (ex.: "banco/Conta.java"), conteúdo = código
     */
    public List<ConcurrencyIssue> detectarProblemasProjeto(List<SourceCode> arquivos) {
        if (arquivos == null || arquivos.isEmpty()) {
            throw new RuntimeException("O projeto não tem arquivos para analisar.");
        }
        long total = arquivos.stream().mapToLong(a -> a.getContent() == null ? 0 : a.getContent().length()).sum();
        if (total > MAX_CARACTERES_PROJETO) {
            throw new RuntimeException("Projeto demasiado longo para análise. Máximo permitido: " + MAX_CARACTERES_PROJETO
                    + " caracteres somando todos os arquivos. Enviado: " + total + " caracteres.");
        }

        Set<String> caminhos = new LinkedHashSet<>();
        List<String> pacotesErrados = new ArrayList<>();
        for (SourceCode arquivo : arquivos) {
            String caminho = arquivo.getFileName() == null ? "" : arquivo.getFileName().trim();
            if (!CAMINHO_ARQUIVO.matcher(caminho).matches()) {
                throw new RuntimeException("Nome de arquivo inválido no projeto: " + caminho);
            }
            if (!caminhos.add(caminho)) {
                throw new RuntimeException("Arquivo repetido no projeto: " + caminho);
            }
            String esperado = caminho.contains("/") ? caminho.substring(0, caminho.lastIndexOf('/')).replace('/', '.') : "";
            Matcher declarado = DECLARACAO_PACKAGE.matcher(arquivo.getContent() == null ? "" : arquivo.getContent());
            String pacote = declarado.find() ? declarado.group(1) : "";
            if (!pacote.equals(esperado)) {
                pacotesErrados.add(esperado.isEmpty()
                        ? caminho + ": está fora de qualquer pasta, então não deve ter a linha \"package " + pacote + ";\"."
                        : caminho + ": está na pasta " + esperado.replace('.', '/') + ", então a primeira linha precisa ser \"package " + esperado + ";\""
                                + (pacote.isEmpty() ? "." : " (hoje está \"package " + pacote + ";\")."));
            }
        }
        if (!pacotesErrados.isEmpty()) {
            throw new RuntimeException("ERRO_COMPILACAO:O package declarado não corresponde à pasta do arquivo:\n"
                    + String.join("\n", pacotesErrados));
        }

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("thread-analyzer-projeto-");
            Path fontes = tempDir.resolve("src");
            Path classes = tempDir.resolve("classes");
            Files.createDirectories(classes);

            List<String> argumentos = new ArrayList<>(List.of("-g", "-proc:none", "-encoding", "UTF-8",
                    "-d", classes.toString()));
            for (SourceCode arquivo : arquivos) {
                Path destino = fontes.resolve(arquivo.getFileName().trim()).normalize();
                if (!destino.startsWith(fontes)) {
                    throw new RuntimeException("Nome de arquivo inválido no projeto: " + arquivo.getFileName());
                }
                Files.createDirectories(destino.getParent());
                Files.writeString(destino, arquivo.getContent(), java.nio.charset.StandardCharsets.UTF_8);
                argumentos.add(destino.toString());
            }

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                throw new RuntimeException("Compilador Java não encontrado. Certifique-se de estar a usar um JDK e não apenas um JRE.");
            }
            ByteArrayOutputStream erros = new ByteArrayOutputStream();
            int resultado = compiler.run(null, null, erros, argumentos.toArray(String[]::new));
            if (resultado != 0) {
                // Tira o caminho da pasta temporária: o aluno vê "banco/Conta.java:3: error: ...".
                String texto = erros.toString(java.nio.charset.StandardCharsets.UTF_8)
                        .replace(fontes.toString() + File.separator, "");
                if (File.separatorChar != '/') {
                    for (String caminho : caminhos) {
                        texto = texto.replace(caminho.replace('/', File.separatorChar), caminho);
                    }
                }
                throw new RuntimeException("ERRO_COMPILACAO:" + texto);
            }

            try (var geradas = Files.walk(classes)) {
                if (geradas.noneMatch(c -> c.toString().endsWith(".class"))) {
                    throw new RuntimeException("ERRO_ESTRUTURA: O teu código compilou mas não gerou nenhuma classe analisável. "
                            + "Certifica-te de que o código contém pelo menos uma declaração 'class' concreta.");
                }
            }

            return SourceConcurrencyDetector.mesclar(
                    spotBugsDetector.detectConcurrencyIssuesProjeto(classes.toString(), caminhos),
                    detetarNoProjeto(fontes)
            );
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro interno durante a análise do código: " + e.getMessage());
        } finally {
            if (tempDir != null) {
                deletarDiretorioTemporario(tempDir);
            }
        }
    }

    private List<ConcurrencyIssue> detetarNoProjeto(Path fontes) {
        try {
            return sourceDetector.detectarNoProjeto(fontes);
        } catch (RuntimeException e) {
            System.err.println("[CodeAnalysis] Falha na análise do código-fonte do projeto (seguindo só com o SpotBugs): " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Complementa o SpotBugs com a análise do código-fonte.
     * Uma falha aqui não deve impedir que os alertas do SpotBugs cheguem ao aluno.
     */
    private List<ConcurrencyIssue> detetarNoCodigoFonte(String diretorioFontes) {
        try {
            return sourceDetector.detectConcurrencyIssues(diretorioFontes);
        } catch (RuntimeException e) {
            System.err.println("[CodeAnalysis] Falha na análise do código-fonte (seguindo só com o SpotBugs): " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Garante o intervalo mínimo entre chamadas globais ao Gemini.
     * O semáforo deve estar adquirido durante esta chamada.
     */
    private static void aguardarIntervaloMinimoEntreChamadasGemini() throws InterruptedException {
        long intervaloNanos = GEMINI_RATE_LIMIT_DELAY_MS * 1_000_000L;
        long agora = System.nanoTime();

        if (hasGeminiCallStarted) {
            long restanteNanos = intervaloNanos - (agora - lastGeminiCallStartedAtNanos);
            if (restanteNanos > 0L) {
                long millis = restanteNanos / 1_000_000L;
                int nanos = (int) (restanteNanos % 1_000_000L);
                Thread.sleep(millis, nanos);
            }
        }

        lastGeminiCallStartedAtNanos = System.nanoTime();
        hasGeminiCallStarted = true;
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
        // As quebras de linha removidas são repostas para que os números de linha dos alertas
        // continuem a coincidir com o código que o aluno vê no editor.
        String conteudoSanitizado = PADRAO_PACKAGE.matcher(sourceCode.getContent())
                .replaceAll(declaracao -> "\n".repeat((int) declaracao.group().chars().filter(c -> c == '\n').count()));

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
