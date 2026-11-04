package com.llm_thread_analyzer.projetos;

import com.llm_thread_analyzer.model.SourceCode;
import com.llm_thread_analyzer.service.CodeAnalysisService;
import com.llm_thread_analyzer.service.CodeAnalysisService.ProjetoCompilado;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Executa o projeto do aluno (botão "Executar" do editor).
 *
 * O código roda num processo Java separado, nunca dentro do servidor:
 *  - Security Manager com uma política própria: o programa pode criar e controlar threads,
 *    mas não pode ler nem gravar arquivos, abrir conexões, ler variáveis de ambiente nem
 *    iniciar outros programas (o servidor guarda segredos como GEMINI_API_KEY e JWT_SECRET);
 *  - ambiente vazio (as variáveis do servidor não passam para o processo);
 *  - limites de tempo, memória e tamanho da saída; teclado (System.in) fechado;
 *  - número máximo de execuções ao mesmo tempo.
 *
 * O Security Manager existe até o Java 23 (foi removido no 24). Por isso o serviço precisa rodar
 * com Java 21 a 23 para executar código de aluno; em Java 24+ a execução responde 503, a não ser
 * que java.execucao.isolamento=nenhum (apenas para desenvolvimento na máquina do próprio autor).
 */
@Component
public class ExecutorJava {

    /** Permissões do código do aluno. Tudo que não está aqui é negado. */
    static final String POLITICA = """
            grant {
              permission java.lang.RuntimePermission "modifyThread";
              permission java.lang.RuntimePermission "modifyThreadGroup";
              permission java.lang.RuntimePermission "stopThread";
              permission java.lang.RuntimePermission "exitVM.*";
              permission java.lang.RuntimePermission "getStackTrace";
              permission java.lang.RuntimePermission "setDefaultUncaughtExceptionHandler";
              permission java.util.PropertyPermission "*", "read";
            };
            """;

    /**
     * Linhas que a própria JVM escreve no início quando o Security Manager está ligado
     * ("WARNING: A command line option has enabled the Security Manager" etc.); o aluno não precisa vê-las.
     */
    private static boolean avisoDaJvm(String linha) {
        String l = linha.strip();
        return l.startsWith("WARNING:") && l.contains("Security Manager");
    }

    public enum Interrupcao { TEMPO, SAIDA }

    /** Máximo de execuções por clique ("Executar 5 vezes"). */
    public static final int MAX_REPETICOES = 5;

    /** Uma execução do programa. */
    public record Rodada(int ordem, String saida, boolean saidaCortada, Integer codigoSaida, long duracaoMs,
                         Interrupcao interrupcao, String aviso) {}

    /**
     * Resultado bruto de um clique (sem o registro no banco): uma compilação e de 1 a 5 rodadas.
     * As rodadas param na primeira interrompida por tempo (um deadlock não faz o aluno esperar 5 × 10 s),
     * então rodadas.size() pode ser menor que repeticoesPedidas.
     */
    public record Resultado(boolean compilou, String erroCompilacao, String arquivoExecutado, String classeExecutada,
                            int limiteSegundos, int repeticoesPedidas, List<Rodada> rodadas) {}

    private final CodeAnalysisService analise;
    private final int limiteSegundos;
    private final int memoriaMb;
    private final int maxCaracteresSaida;
    private final boolean isolar;
    private final Semaphore vagas;

    public ExecutorJava(CodeAnalysisService analise,
                        @Value("${java.execucao.limite-segundos:10}") int limiteSegundos,
                        @Value("${java.execucao.memoria-mb:128}") int memoriaMb,
                        @Value("${java.execucao.max-caracteres-saida:50000}") int maxCaracteresSaida,
                        @Value("${java.execucao.max-simultaneas:4}") int maxSimultaneas,
                        @Value("${java.execucao.isolamento:security-manager}") String isolamento) {
        this.analise = analise;
        this.limiteSegundos = Math.max(1, limiteSegundos);
        this.memoriaMb = Math.max(32, memoriaMb);
        this.maxCaracteresSaida = Math.max(1000, maxCaracteresSaida);
        this.vagas = new Semaphore(Math.max(1, maxSimultaneas), true);
        this.isolar = !"nenhum".equalsIgnoreCase(isolamento.strip());
        if (!isolar) {
            System.err.println("[Executar] ATENÇÃO: java.execucao.isolamento=nenhum. O código dos alunos roda sem "
                    + "Security Manager. Use apenas em desenvolvimento, nunca com alunos.");
        }
    }

    /**
     * Compila o projeto e executa a classe com main() {@code repeticoes} vezes seguidas.
     *
     * @param arquivoAtivo arquivo aberto no editor; se tiver main(), é ele que roda
     * @param repeticoes   1 (Executar) ou até MAX_REPETICOES ("Executar 5 vezes")
     */
    public Resultado executar(List<SourceCode> arquivos, String arquivoAtivo, int repeticoes) {
        if (repeticoes < 1 || repeticoes > MAX_REPETICOES) {
            throw new ProjetoException(HttpStatus.BAD_REQUEST,
                    "O programa pode ser executado de 1 a " + MAX_REPETICOES + " vezes por clique.");
        }
        if (isolar && Runtime.version().feature() >= 24) {
            throw new ProjetoException(HttpStatus.SERVICE_UNAVAILABLE,
                    "A execução de programas não está disponível neste servidor: ele precisa rodar com Java 21 a 23.");
        }
        boolean conseguiuVaga;
        try {
            conseguiuVaga = vagas.tryAcquire(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProjetoException(HttpStatus.SERVICE_UNAVAILABLE, "Execução cancelada. Tente novamente.");
        }
        if (!conseguiuVaga) {
            throw new ProjetoException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Muitos programas estão sendo executados agora. Tente de novo em alguns segundos.");
        }

        Path pasta = null;
        try {
            pasta = Files.createTempDirectory("thread-analyzer-execucao-");
            ProjetoCompilado compilado;
            try {
                compilado = analise.compilarProjeto(arquivos, pasta);
            } catch (RuntimeException e) {
                String msg = e.getMessage() == null ? "" : e.getMessage();
                if (msg.startsWith("ERRO_COMPILACAO:")) {
                    return new Resultado(false, msg.substring(msg.indexOf(':') + 1).trim(), null, null,
                            limiteSegundos, repeticoes, List.of());
                }
                throw new ProjetoException(HttpStatus.BAD_REQUEST, msg);
            }

            ClasseMain alvo = escolherMain(compilado, arquivoAtivo);
            List<Rodada> rodadas = new ArrayList<>();
            for (int i = 1; i <= repeticoes; i++) {
                Rodada r = rodar(i, pasta, compilado.classes(), alvo, arquivos);
                rodadas.add(r);
                if (r.interrupcao() == Interrupcao.TEMPO) break;
            }
            return new Resultado(true, null, alvo.arquivo(), alvo.nomeCompleto(), limiteSegundos, repeticoes,
                    List.copyOf(rodadas));
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao preparar a execução: " + e.getMessage(), e);
        } finally {
            vagas.release();
            if (pasta != null) analise.deletarDiretorioTemporario(pasta);
        }
    }

    // ===================== Qual classe executar =====================

    record ClasseMain(String arquivo, String nomeCompleto, String nomeSimples) {}

    /**
     * A classe do arquivo aberto, se ela tiver main(); senão, a única classe do projeto com main().
     * Com mais de uma (e o arquivo aberto sem main), pede para o aluno abrir a que quer executar.
     */
    ClasseMain escolherMain(ProjetoCompilado compilado, String arquivoAtivo) throws IOException {
        List<ClasseMain> todas = procurarMains(compilado);
        if (todas.isEmpty()) {
            throw new ProjetoException(HttpStatus.BAD_REQUEST,
                    "Nenhuma classe do projeto tem o método main. Para executar, uma classe precisa ter "
                            + "\"public static void main(String[] args)\".");
        }
        List<ClasseMain> doAtivo = todas.stream().filter(c -> c.arquivo().equals(arquivoAtivo)).toList();
        if (!doAtivo.isEmpty()) {
            String nomeDoArquivo = nomeDaClasseDoArquivo(arquivoAtivo);
            return doAtivo.stream().filter(c -> c.nomeSimples().equals(nomeDoArquivo)).findFirst().orElse(doAtivo.get(0));
        }
        if (todas.size() == 1) return todas.get(0);
        List<String> nomes = todas.stream().map(ClasseMain::arquivo).distinct().toList();
        throw new ProjetoException(HttpStatus.BAD_REQUEST,
                "Mais de um arquivo tem o método main (" + String.join(", ", nomes)
                        + "). Abra no editor o arquivo que você quer executar e clique em Executar de novo.");
    }

    private static String nomeDaClasseDoArquivo(String caminho) {
        if (caminho == null) return "";
        String nome = caminho.substring(caminho.lastIndexOf('/') + 1);
        return nome.endsWith(".java") ? nome.substring(0, nome.length() - 5) : nome;
    }

    /** Procura "public static void main(String[] args)" nas classes de nível superior, lendo só o código-fonte. */
    private List<ClasseMain> procurarMains(ProjetoCompilado compilado) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        List<ClasseMain> encontradas = new ArrayList<>();
        try (StandardJavaFileManager arquivos = compiler.getStandardFileManager(null, Locale.ROOT, StandardCharsets.UTF_8)) {
            for (String caminho : compilado.caminhos()) {
                Path fonte = compilado.fontes().resolve(caminho);
                JavacTask tarefa = (JavacTask) compiler.getTask(null, arquivos, d -> { }, List.of("-proc:none"), null,
                        arquivos.getJavaFileObjects(fonte));
                for (CompilationUnitTree unidade : tarefa.parse()) {
                    String pacote = unidade.getPackageName() == null ? "" : unidade.getPackageName().toString();
                    for (Tree tipo : unidade.getTypeDecls()) {
                        if (tipo instanceof ClassTree classe && temMain(classe)) {
                            String simples = classe.getSimpleName().toString();
                            encontradas.add(new ClasseMain(caminho, pacote.isEmpty() ? simples : pacote + "." + simples, simples));
                        }
                    }
                }
            }
        }
        return encontradas;
    }

    private static boolean temMain(ClassTree classe) {
        boolean interfaceOuAnotacao = classe.getKind() == Tree.Kind.INTERFACE || classe.getKind() == Tree.Kind.ANNOTATION_TYPE;
        for (Tree membro : classe.getMembers()) {
            if (!(membro instanceof MethodTree m) || !m.getName().contentEquals("main")) continue;
            Set<Modifier> mods = m.getModifiers().getFlags();
            if (!mods.contains(Modifier.STATIC) || (!interfaceOuAnotacao && !mods.contains(Modifier.PUBLIC))) continue;
            if (!(m.getReturnType() instanceof PrimitiveTypeTree r) || r.getPrimitiveTypeKind() != TypeKind.VOID) continue;
            if (m.getParameters().size() != 1) continue;
            VariableTree p = m.getParameters().get(0);
            if (p.getType() instanceof ArrayTypeTree arr) {
                String elem = arr.getType().toString();
                if (elem.equals("String") || elem.equals("java.lang.String")) return true;
            }
        }
        return false;
    }

    // ===================== Processo =====================

    private Rodada rodar(int ordem, Path pasta, Path classes, ClasseMain alvo, List<SourceCode> fontes) throws IOException {
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        Path java = Path.of(System.getProperty("java.home"), "bin", windows ? "java.exe" : "java");
        Path temporaria = Files.createDirectories(pasta.resolve("tmp"));

        List<String> comando = new ArrayList<>(List.of(java.toString(),
                "-Xmx" + memoriaMb + "m",
                "-XX:+UseSerialGC",
                "-XX:TieredStopAtLevel=1",
                "-Dfile.encoding=UTF-8",
                "-Dstdout.encoding=UTF-8",
                "-Dstderr.encoding=UTF-8",
                "-Djava.io.tmpdir=" + temporaria));
        if (isolar) {
            Path politica = pasta.resolve("aluno.policy");
            Files.writeString(politica, POLITICA, StandardCharsets.UTF_8);
            comando.add("-Djava.security.manager");
            // "==" usa só esta política (ignora a java.policy padrão do JDK).
            comando.add("-Djava.security.policy==" + politica.toUri());
        }
        comando.addAll(List.of("-cp", classes.toString(), alvo.nomeCompleto()));

        ProcessBuilder pb = new ProcessBuilder(comando)
                .directory(temporaria.toFile())
                .redirectErrorStream(true)
                .redirectInput(ProcessBuilder.Redirect.from(windows ? Path.of("NUL").toFile() : Path.of("/dev/null").toFile()));
        // Nada do ambiente do servidor passa para o programa do aluno.
        var ambiente = pb.environment();
        String systemRoot = ambiente.get("SystemRoot");
        ambiente.clear();
        if (windows && systemRoot != null) ambiente.put("SystemRoot", systemRoot);
        ambiente.put("TMP", temporaria.toString());
        ambiente.put("TEMP", temporaria.toString());

        long inicio = System.nanoTime();
        Process processo = pb.start();
        LeitorSaida leitor = new LeitorSaida(processo, maxCaracteresSaida);
        Thread threadLeitor = new Thread(leitor, "executar-saida");
        threadLeitor.setDaemon(true);
        threadLeitor.start();

        Interrupcao interrupcao = null;
        try {
            long limiteNanos = TimeUnit.SECONDS.toNanos(limiteSegundos);
            while (true) {
                long restante = limiteNanos - (System.nanoTime() - inicio);
                if (processo.waitFor(Math.max(0, Math.min(restante, TimeUnit.MILLISECONDS.toNanos(100))), TimeUnit.NANOSECONDS)) break;
                if (leitor.estourou()) { interrupcao = Interrupcao.SAIDA; break; }
                if (System.nanoTime() - inicio >= limiteNanos) { interrupcao = Interrupcao.TEMPO; break; }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            interrupcao = Interrupcao.TEMPO;
        } finally {
            if (processo.isAlive()) {
                processo.descendants().forEach(ProcessHandle::destroyForcibly);
                processo.destroyForcibly();
            }
        }
        long duracaoMs = (System.nanoTime() - inicio) / 1_000_000L;
        try {
            processo.waitFor(2, TimeUnit.SECONDS);
            threadLeitor.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Integer codigo = interrupcao == null && !processo.isAlive() ? processo.exitValue() : null;
        String saida = limparAvisosDaJvm(leitor.texto());
        return new Rodada(ordem, saida, leitor.estourou(), codigo, duracaoMs, interrupcao, aviso(saida, fontes));
    }

    static String limparAvisosDaJvm(String saida) {
        StringBuilder sb = new StringBuilder(saida.length());
        boolean inicio = true;
        for (String linha : saida.split("\n", -1)) {
            // só no começo da saída: depois disso, o texto é do programa do aluno
            if (inicio && avisoDaJvm(linha)) continue;
            inicio = false;
            sb.append(linha).append('\n');
        }
        if (sb.length() > 0) sb.setLength(sb.length() - 1); // tira o '\n' extra do fim
        return sb.toString();
    }

    /** Explica erros que vêm das restrições da execução, e não do código em si. */
    static String aviso(String saida, List<SourceCode> fontes) {
        if (saida.contains("java.security.AccessControlException") || saida.contains("java.lang.SecurityException")) {
            return "Por segurança, os programas executados aqui não podem acessar arquivos, rede, variáveis de "
                    + "ambiente nem outros programas. Isso não é um erro de concorrência.";
        }
        boolean usaTeclado = fontes.stream().anyMatch(f -> f.getContent() != null && f.getContent().contains("System.in"));
        if (usaTeclado && saida.contains("java.util.NoSuchElementException")) {
            return "Os programas executados aqui não recebem entrada pelo teclado (System.in). "
                    + "Coloque os valores direto no código.";
        }
        return null;
    }

    /** Lê a saída do processo até o limite de caracteres. */
    private static final class LeitorSaida implements Runnable {
        private final Process processo;
        private final int limite;
        private final StringBuilder texto = new StringBuilder();
        private volatile boolean estourou;

        LeitorSaida(Process processo, int limite) {
            this.processo = processo;
            this.limite = limite;
        }

        @Override
        public void run() {
            char[] buf = new char[8192];
            try (Reader r = new InputStreamReader(processo.getInputStream(), StandardCharsets.UTF_8)) {
                int n;
                while ((n = r.read(buf)) != -1) {
                    // Sem o '\r' do Windows: o limite de caracteres vale igual em qualquer sistema.
                    int m = 0;
                    for (int i = 0; i < n; i++) if (buf[i] != '\r') buf[m++] = buf[i];
                    n = m;
                    synchronized (texto) {
                        int cabe = limite - texto.length();
                        if (n > cabe) {
                            texto.append(buf, 0, Math.max(0, cabe));
                            estourou = true;
                            return;
                        }
                        texto.append(buf, 0, n);
                    }
                }
            } catch (IOException e) {
                // processo encerrado à força: fica o que foi lido
            }
        }

        boolean estourou() { return estourou; }

        String texto() {
            synchronized (texto) {
                return texto.toString().replace("\r\n", "\n");
            }
        }
    }
}
