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

    // CORREÇÃO: Intervalo entre chamadas ao Gemini para respeitar o rate limit do tier gratuito (~15 RPM)
    // 4500ms = ~13 requisições/minuto, com margem de segurança
    private static final long GEMINI_RATE_LIMIT_DELAY_MS = 4500;

    // CORREÇÃO: Semaphore global para controlo de concorrência nas chamadas ao Gemini
    // Thread.sleep por si só não é suficiente — em requisições paralelas, múltiplas threads
    // aguardam 4500ms em paralelo e depois disparam todas ao mesmo tempo, esgotando o rate limit.
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

        // PROTEÇÃO: Limite de tamanho do payload — previne "Bombas de Compilação" (DoS)
        // Um utilizador pode enviar código gerado artificialmente com milhares de linhas,
        // esgotando a RAM do servidor ao tentar compilar (OutOfMemoryError).
        // 50.000 caracteres (~1250 linhas de código) é um limite generoso para código académico
        // e rejeita payloads abusivos antes de qualquer operação em disco ou memória.
        final int TAMANHO_MAXIMO_CODIGO = 50_000;
        if (sourceCode.getContent().length() > TAMANHO_MAXIMO_CODIGO) {
            throw new RuntimeException(
                "Código demasiado longo para análise. Máximo permitido: " + TAMANHO_MAXIMO_CODIGO +
                " caracteres. Enviado: " + sourceCode.getContent().length() + " caracteres."
            );
        }

        // CORREÇÃO: Defesa contra "Bombas de Compilação" (Denial of Service)
        // Um payload gigantesco (código gerado por IA, classes artificialmente complexas, etc.)
        // pode esgotar a RAM do servidor ao ser compilado, causando OutOfMemoryError.
        // O limite de 50.000 caracteres (~1250 linhas) é generoso para qualquer exercício académico
        // real, mas rejeita payloads maliciosos ou acidentalmente excessivos antes de qualquer
        // operação de I/O (criação de pasta temporária, escrita em disco, invocação do compilador).
        final int LIMITE_CARACTERES = 50_000;
        if (sourceCode.getContent().length() > LIMITE_CARACTERES) {
            throw new RuntimeException(
                "Código demasiado longo para análise. Máximo permitido: " + LIMITE_CARACTERES +
                " caracteres. Recebido: " + sourceCode.getContent().length() + " caracteres."
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
                    // CORREÇÃO: Adquirir o Semaphore antes de chamar o Gemini
                    // Garante exclusividade global mesmo com múltiplas threads simultâneas
                    GEMINI_SEMAPHORE.acquire();
                    try {
                        String explicacao = llmFeedbackService.gerarFeedbackDidatico(
                                issue.getMessage(),
                                sourceCode.getContent()
                        );
                        issue.setInterpretation(explicacao);
                    } finally {
                        // CORREÇÃO: Sleep dentro do bloco finally — o delay é aplicado ANTES
                        // de libertar o semáforo, garantindo que a próxima thread só avança
                        // após o intervalo de rate limit, não apenas após a chamada terminar
                        if (i < issues.size() - 1) {
                            System.out.println("[CodeAnalysis] Aguardando " + GEMINI_RATE_LIMIT_DELAY_MS + "ms (rate limit)...");
                            Thread.sleep(GEMINI_RATE_LIMIT_DELAY_MS);
                        }
                        GEMINI_SEMAPHORE.release();
                    }
                } catch (LlmApiException e) {
                    // Falha do LLM marcada estruturalmente — não interrompe os outros issues.
                    // NOTA: NÃO chamamos GEMINI_SEMAPHORE.release() aqui.
                    // O bloco finally interno já o faz de forma infalível — um release() adicional
                    // aqui causaria "Double Release": o semáforo passaria de 1 para 2 permissões,
                    // depois 3, colapsando o controlo de concorrência silenciosamente (bug irónico
                    // numa ferramenta de deteção de bugs de concorrência).
                    System.err.println("[CodeAnalysis] Falha do LLM para issue #" + i + ": " + e.getMessage());
                    issue.setLlmError(true);
                    issue.setLlmErrorMessage(e.getMessage());
                } catch (InterruptedException ie) {
                    // Repõe o estado de interrupção da thread conforme boas práticas.
                    // NOTA: NÃO chamamos GEMINI_SEMAPHORE.release() aqui pelo mesmo motivo acima.
                    // O break é obrigatório — sem ele, na iteração seguinte o acquire() lançaria
                    // imediatamente outra InterruptedException (a thread continua marcada como
                    // interrompida), criando um loop infinito de erros para todos os issues restantes.
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
            // CORREÇÃO: Limpeza garantida do diretório temporário após o SpotBugs terminar
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
            // CORREÇÃO: Sanitização contra Path Traversal
            // Um atacante poderia enviar "../../../../etc/passwd.java" no payload JSON
            // e o código escreveria fora do diretório temporário pretendido.
            // Paths.get().getFileName() extrai apenas o nome final, descartando qualquer prefixo de diretório.
            nomeFicheiro = Paths.get(nomeFicheiro).getFileName().toString();

            if (!nomeFicheiro.endsWith(".java")) {
                nomeFicheiro += ".java";
            }
        }

        // CORREÇÃO: Remoção da declaração package antes de salvar o ficheiro.
        // Ficheiros do benchmark JCB frequentemente contêm "package br.com.xyz;" retirados
        // de projetos reais. O javac compila mas o SpotBugs fica confuso porque espera que
        // Main.class esteja dentro de br/com/xyz/ — causando falsos negativos silenciosos.
        // A regex remove qualquer "package ...;" forçando o código ao pacote padrão (flat).
        String conteudoSanitizado = sourceCode.getContent()
                .replaceAll("(?m)^\\s*package\\s+[\\w.]+;\\s*\\n?", "");

        File sourceFile = new File(tempDir.toFile(), nomeFicheiro);

        // CORREÇÃO: Escrita do ficheiro forçando UTF-8 via NIO.
        // FileWriter usa o encoding padrão do SO (Cp1252 no Windows, UTF-8 no Linux).
        // Em servidores Windows ou ao avaliar numa máquina diferente, caracteres acentuados
        // em comentários/strings do aluno (ex: "// secção crítica") ficavam corrompidos,
        // causando erro "unmappable character" no compilador antes de qualquer análise.
        // Files.writeString com StandardCharsets.UTF_8 garante consistência em qualquer SO.
        Files.writeString(sourceFile.toPath(), conteudoSanitizado, java.nio.charset.StandardCharsets.UTF_8);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new RuntimeException("Compilador Java não encontrado. Certifique-se de estar a usar um JDK e não apenas um JRE.");
        }

        // CORREÇÃO: errorStream também decodificado em UTF-8 para preservar mensagens acentuadas
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

        // SEGURANÇA: Flag -proc:none desativa Annotation Processors durante a compilação.
        // Sem esta flag, um utilizador malicioso poderia submeter código com anotações que
        // acionam processadores automáticos do javac, executando código arbitrário no servidor
        // durante a fase de compilação — mesmo sem invocar a classe compilada.
        // Flag -encoding UTF-8 garante que o compilador lê o ficheiro fonte no mesmo encoding
        // com que foi escrito, evitando erros com caracteres especiais em qualquer plataforma.
        int result = compiler.run(null, null, errorStream,
                "-g", "-proc:none", "-encoding", "UTF-8",
                sourceFile.getAbsolutePath());

        if (result != 0) {
            // Decodifica os erros do compilador em UTF-8 para preservar mensagens acentuadas
            String errosCompilacao = errorStream.toString(java.nio.charset.StandardCharsets.UTF_8);
            throw new RuntimeException("ERRO_COMPILACAO:" + errosCompilacao);
        }

        // CORREÇÃO: Verificação de estrutura válida após compilação bem-sucedida.
        // Um código vazio ou que declare apenas uma interface compila sem erros (retorna 0)
        // mas não gera nenhum .class utilizável. Sem esta verificação, o SpotBugs receberia
        // um diretório vazio e retornaria silenciosamente zero issues — falso negativo.
        // Prefixo ERRO_ESTRUTURA: permite ao Controller devolver HTTP 400 (culpa do aluno)
        // em vez de HTTP 500 (culpa do servidor), seguindo boas práticas REST.
        File[] classesGeradas = tempDir.toFile().listFiles((dir, name) -> name.endsWith(".class"));
        if (classesGeradas == null || classesGeradas.length == 0) {
            throw new RuntimeException("ERRO_ESTRUTURA: O teu código compilou mas não gerou nenhuma classe analisável. " +
                    "Certifica-te de que o código contém pelo menos uma declaração 'class' concreta.");
        }

        // Como o ConcurrencyIssuesDetector já itera TODOS os .class do diretório,
        // retornamos o diretório — não um ficheiro .class específico.
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