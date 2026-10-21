package com.llm_thread_analyzer.service;

import com.llm_thread_analyzer.model.ConcurrencyIssue;
import com.sun.source.tree.*;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import org.springframework.stereotype.Service;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.UnionType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Detector complementar ao SpotBugs.
 *
 * O SpotBugs analisa bytecode e reconhece bem erros "de classe" (sincronização inconsistente,
 * wait fora de laço, double-checked locking...), mas não reconhece vários erros clássicos
 * escritos no main com lambdas — o estilo mais comum nos exercícios dos alunos:
 * condição de corrida em campo estático, deadlock por ordem de locks, coleção não thread-safe
 * compartilhada, leitura sem join(), entre outros.
 *
 * Este detector percorre a árvore sintática do javac (já atribuída com tipos e símbolos)
 * para cobrir esses casos. Os alertas usam o prefixo SRC_ para se distinguirem dos do SpotBugs.
 *
 * Escopo: apenas problemas de concorrência. Erros gerais de lógica Java (ex.: divisão por zero)
 * estão fora do escopo e não são reportados.
 *
 * Decisões de escopo pendentes:
 * - Exceção não tratada dentro de uma thread (a thread termina sem aviso à thread principal):
 *   ainda não há regra. É um tema de concorrência próprio, independente da divisão por zero.
 * - Falso positivo conhecido do SpotBugs (AT_NONATOMIC_*) em subclasse de Thread com campo próprio,
 *   lido pelo main depois do join(): o alerta é mantido. Uma supressão só deve voltar com
 *   evidência positiva de que o objeto não escapa (alias, argumento) e de que todo start() é conhecido.
 */
@Service
public class SourceConcurrencyDetector {

    // Tipos SRC_ que o SpotBugs às vezes já reporta na mesma linha com outro nome.
    // Na mesclagem, o alerta do SpotBugs tem prioridade para não duplicar a explicação do LLM.
    private static final Map<String, List<String>> COBERTOS_PELO_SPOTBUGS = Map.of(
            "SRC_UNSYNCHRONIZED_SHARED_WRITE", List.of("IS2_", "IS_", "AT_", "VO_", "DC_", "ML_"),
            "SRC_NON_THREAD_SAFE_COLLECTION", List.of("AT_", "IS2_"),
            "SRC_NON_VOLATILE_FLAG", List.of("SP_", "IS2_", "AT_"),
            "SRC_MONITOR_NOT_HELD", List.of("MWN_")
    );

    private static final Set<String> COLECOES_NAO_THREAD_SAFE = Set.of(
            "java.util.ArrayList", "java.util.LinkedList", "java.util.HashMap", "java.util.HashSet",
            "java.util.LinkedHashMap", "java.util.LinkedHashSet", "java.util.TreeMap", "java.util.TreeSet",
            "java.util.ArrayDeque", "java.util.PriorityQueue", "java.util.WeakHashMap",
            "java.util.IdentityHashMap", "java.util.EnumMap");

    private static final Set<String> METODOS_QUE_MODIFICAM = Set.of(
            "add", "addAll", "addFirst", "addLast", "put", "putAll", "putIfAbsent", "remove", "removeAll",
            "removeIf", "removeFirst", "removeLast", "retainAll", "clear", "set", "offer", "offerFirst",
            "offerLast", "poll", "pollFirst", "pollLast", "push", "pop", "compute", "computeIfAbsent",
            "computeIfPresent", "merge", "replace", "replaceAll", "sort");

    // Métodos do CompletableFuture que executam o lambda recebido noutra thread.
    private static final Set<String> METODOS_ASSINCRONOS = Set.of(
            "supplyAsync", "runAsync", "thenApplyAsync", "thenAcceptAsync", "thenRunAsync", "thenComposeAsync");

    private static final Set<String> METODOS_QUE_ENVIAM_TAREFAS = Set.of(
            "submit", "execute", "invokeAll", "invokeAny", "schedule", "scheduleAtFixedRate", "scheduleWithFixedDelay");

    private static final Set<String> METODOS_QUE_ENCERRAM_EXECUTOR = Set.of("shutdown", "shutdownNow", "close");

    // Chamadas que fazem a thread principal esperar por outra thread (além de join()).
    private static final Set<String> METODOS_DE_ESPERA = Set.of("await", "awaitTermination", "acquire", "invokeAll");

    public List<ConcurrencyIssue> detectConcurrencyIssues(String sourceDirPath) {
        File[] fontes = new File(sourceDirPath).listFiles((dir, name) -> name.endsWith(".java"));
        if (fontes == null || fontes.length == 0) {
            return new ArrayList<>();
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new RuntimeException("[Fonte] Compilador Java não encontrado. Certifique-se de estar a usar um JDK e não apenas um JRE.");
        }

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            // Diagnósticos descartados: o código já foi compilado com sucesso antes desta etapa.
            JavacTask task = (JavacTask) compiler.getTask(null, fileManager, diagnostico -> {},
                    List.of("-proc:none", "-encoding", "UTF-8"), null, fileManager.getJavaFileObjects(fontes));
            Iterable<? extends CompilationUnitTree> unidades = task.parse();
            task.analyze();

            List<ConcurrencyIssue> issues = new ArrayList<>();
            for (CompilationUnitTree unidade : unidades) {
                issues.addAll(new Analise(task, unidade).executar());
            }
            issues.sort(Comparator.comparingInt(ConcurrencyIssue::getLineNumber));

            System.out.println("[Fonte] Issues de concorrência detetados no código-fonte: " + issues.size());
            for (ConcurrencyIssue issue : issues) {
                System.out.println("[Fonte] " + issue.getType() + " (linha " + issue.getLineNumber() + ")");
            }
            return issues;
        } catch (IOException e) {
            throw new RuntimeException("[Fonte] Falha ao ler o código-fonte: " + e.getMessage(), e);
        }
    }

    /**
     * Junta os alertas do SpotBugs com os do código-fonte, descartando os SRC_ que o SpotBugs
     * já reportou na mesma linha, e ordena tudo pela linha.
     */
    public static List<ConcurrencyIssue> mesclar(List<ConcurrencyIssue> spotBugs, List<ConcurrencyIssue> fonte) {
        List<ConcurrencyIssue> resultado = new ArrayList<>(spotBugs);
        for (ConcurrencyIssue issueFonte : fonte) {
            List<String> prefixos = COBERTOS_PELO_SPOTBUGS.getOrDefault(issueFonte.getType(), List.of());
            boolean jaReportado = spotBugs.stream().anyMatch(s ->
                    s.getLineNumber() == issueFonte.getLineNumber() && prefixos.stream().anyMatch(s.getType()::startsWith));
            if (!jaReportado) {
                resultado.add(issueFonte);
            }
        }
        resultado.sort(Comparator.comparingInt(ConcurrencyIssue::getLineNumber));
        return resultado;
    }

    /**
     * Análise de uma unidade de compilação.
     *
     * Conceito central: "raiz" é um trecho de código que executa numa thread que não é a principal —
     * um lambda Runnable/Callable, o run()/call() de uma classe, ou um método chamado a partir deles.
     * Para cada raiz sabe-se se pode estar a executar em mais de uma thread ao mesmo tempo
     * (multiplicidade), o que distingue estado realmente compartilhado de estado confinado a uma thread.
     */
    private static final class Analise {

        private final Trees trees;
        private final Types types;
        private final Elements elements;
        private final CompilationUnitTree unidade;
        private final SourcePositions posicoes;

        private final Map<Element, TreePath> metodos = new LinkedHashMap<>();
        private final Map<VariableElement, TreePath> declaracoes = new LinkedHashMap<>();
        private final Map<VariableElement, List<TreePath>> usos = new LinkedHashMap<>();
        private final Map<VariableElement, List<TreePath>> valoresAtribuidos = new LinkedHashMap<>();
        private final List<TreePath> lambdas = new ArrayList<>();
        private final List<TreePath> referenciasMetodo = new ArrayList<>();
        private final List<TreePath> instanciacoes = new ArrayList<>();
        private final List<TreePath> invocacoes = new ArrayList<>();
        private final List<TreePath> sincronizados = new ArrayList<>();
        private final List<TreePath> capturas = new ArrayList<>();
        private final List<TreePath> lacos = new ArrayList<>();

        private final Map<Tree, Raiz> raizes = new LinkedHashMap<>();
        private final List<Acesso> acessos = new ArrayList<>();
        private final List<ConcurrencyIssue> issues = new ArrayList<>();

        Analise(JavacTask task, CompilationUnitTree unidade) {
            this.trees = Trees.instance(task);
            this.types = task.getTypes();
            this.elements = task.getElements();
            this.unidade = unidade;
            this.posicoes = trees.getSourcePositions();
        }

        List<ConcurrencyIssue> executar() {
            new Coletor().scan(unidade, null);
            identificarRaizes();
            propagarChamadas();
            coletarAcessos();

            detectarEscritaCompartilhada();
            detectarColecaoNaoThreadSafe();
            detectarLeituraSemJoin();
            detectarFlagSemVolatile();
            detectarDeadlockPorOrdemDeLocks();
            detectarMonitorNaoAdquirido();
            detectarInterrupcaoIgnorada();
            detectarExecutorSemShutdown();
            return issues;
        }

        /* ───────────── Estruturas ───────────── */

        private static final class Raiz {
            final TreePath caminho;
            // true = ponto onde a thread começa (lambda, run()/call(), referência de método);
            // false = método chamado a partir de uma raiz
            final boolean entrada;
            // Pode executar em várias threads ao mesmo tempo sobre o MESMO objeto (campos via this)
            boolean multiInstancia;
            // Pode executar em várias threads ao mesmo tempo (campos estáticos e objetos externos)
            boolean multiClasse;
            // Todas as chamadas a este método acontecem com um lock adquirido
            boolean protegidaPeloChamador;
            final Map<Raiz, Chamada> chamadores = new LinkedHashMap<>();
            // Pontos de entrada de thread que alcançam esta raiz
            final Set<Raiz> origens = new LinkedHashSet<>();

            Raiz(TreePath caminho, boolean entrada) {
                this.caminho = caminho;
                this.entrada = entrada;
                if (entrada) origens.add(this);
            }
        }

        private record Chamada(boolean protegida, boolean viaThis) {
            Chamada juntar(Chamada outra) {
                return new Chamada(protegida && outra.protegida, viaThis && outra.viaThis);
            }
        }

        private record Acesso(VariableElement variavel, TreePath caminho, Raiz raiz,
                              boolean escrita, boolean mutacao, boolean protegido, boolean viaThis) {}

        private record ChaveThis(Element classe) {}

        private record ChaveClasse(Element classe) {}

        private record ParDeLocks(Object externo, Object interno, TreePath caminho) {}

        /* ───────────── Coleta ───────────── */

        private final class Coletor extends TreePathScanner<Void, Void> {
            @Override
            public Void visitMethod(MethodTree node, Void p) {
                Element metodo = trees.getElement(getCurrentPath());
                if (metodo != null) metodos.put(metodo, getCurrentPath());
                return super.visitMethod(node, p);
            }

            @Override
            public Void visitVariable(VariableTree node, Void p) {
                if (trees.getElement(getCurrentPath()) instanceof VariableElement variavel) {
                    declaracoes.put(variavel, getCurrentPath());
                    if (node.getInitializer() != null) {
                        registrarValor(variavel, new TreePath(getCurrentPath(), node.getInitializer()));
                    }
                }
                return super.visitVariable(node, p);
            }

            @Override
            public Void visitAssignment(AssignmentTree node, Void p) {
                if (trees.getElement(new TreePath(getCurrentPath(), node.getVariable())) instanceof VariableElement variavel) {
                    registrarValor(variavel, new TreePath(getCurrentPath(), node.getExpression()));
                }
                return super.visitAssignment(node, p);
            }

            @Override
            public Void visitIdentifier(IdentifierTree node, Void p) {
                registrarUso();
                return super.visitIdentifier(node, p);
            }

            @Override
            public Void visitMemberSelect(MemberSelectTree node, Void p) {
                registrarUso();
                return super.visitMemberSelect(node, p);
            }

            @Override
            public Void visitLambdaExpression(LambdaExpressionTree node, Void p) {
                lambdas.add(getCurrentPath());
                return super.visitLambdaExpression(node, p);
            }

            @Override
            public Void visitMemberReference(MemberReferenceTree node, Void p) {
                referenciasMetodo.add(getCurrentPath());
                return super.visitMemberReference(node, p);
            }

            @Override
            public Void visitNewClass(NewClassTree node, Void p) {
                instanciacoes.add(getCurrentPath());
                return super.visitNewClass(node, p);
            }

            @Override
            public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
                invocacoes.add(getCurrentPath());
                return super.visitMethodInvocation(node, p);
            }

            @Override
            public Void visitSynchronized(SynchronizedTree node, Void p) {
                sincronizados.add(getCurrentPath());
                return super.visitSynchronized(node, p);
            }

            @Override
            public Void visitCatch(CatchTree node, Void p) {
                capturas.add(getCurrentPath());
                return super.visitCatch(node, p);
            }

            @Override
            public Void visitWhileLoop(WhileLoopTree node, Void p) {
                lacos.add(getCurrentPath());
                return super.visitWhileLoop(node, p);
            }

            @Override
            public Void visitDoWhileLoop(DoWhileLoopTree node, Void p) {
                lacos.add(getCurrentPath());
                return super.visitDoWhileLoop(node, p);
            }

            @Override
            public Void visitForLoop(ForLoopTree node, Void p) {
                lacos.add(getCurrentPath());
                return super.visitForLoop(node, p);
            }

            private void registrarUso() {
                if (trees.getElement(getCurrentPath()) instanceof VariableElement variavel && !ehPseudoVariavel(variavel)) {
                    usos.computeIfAbsent(variavel, k -> new ArrayList<>()).add(getCurrentPath());
                }
            }

            private void registrarValor(VariableElement variavel, TreePath valor) {
                valoresAtribuidos.computeIfAbsent(variavel, k -> new ArrayList<>()).add(valor);
            }
        }

        /* ───────────── Raízes (código que executa noutra thread) ───────────── */

        private void identificarRaizes() {
            for (TreePath lambda : lambdas) {
                if (ehTipoDeTarefa(trees.getTypeMirror(lambda)) || argumentoDeExecucaoAssincrona(lambda)) {
                    boolean multi = emLaco(lambda) || usoMultiplo(lambda);
                    criarEntrada(lambda, multi, multi);
                }
            }

            for (Map.Entry<Element, TreePath> entrada : metodos.entrySet()) {
                ExecutableElement metodo = (ExecutableElement) entrada.getKey();
                if (!ehRunOuCall(metodo)) continue;
                Element classe = metodo.getEnclosingElement();
                boolean estendeThread = eh(classe.asType(), "java.lang.Thread");
                List<TreePath> criacoes = instanciacoesDe(classe);
                boolean multiClasse = criacoes.size() >= 2 || criacoes.stream().anyMatch(c -> emLaco(c) || usoMultiplo(c));
                // Um objeto Thread só pode ser iniciado uma vez: os campos dele ficam confinados a essa thread.
                boolean multiInstancia = !estendeThread && criacoes.stream().anyMatch(this::usoMultiplo);
                criarEntrada(entrada.getValue(), multiInstancia, multiClasse);
            }

            for (TreePath referencia : referenciasMetodo) {
                if (!ehTipoDeTarefa(trees.getTypeMirror(referencia))) continue;
                TreePath alvo = metodos.get(trees.getElement(referencia));
                if (alvo != null && !raizes.containsKey(alvo.getLeaf())) {
                    boolean multi = emLaco(referencia) || usoMultiplo(referencia);
                    criarEntrada(alvo, multi, multi);
                }
            }
        }

        private void criarEntrada(TreePath caminho, boolean multiInstancia, boolean multiClasse) {
            Raiz raiz = new Raiz(caminho, true);
            raiz.multiInstancia = multiInstancia;
            raiz.multiClasse = multiClasse;
            raizes.put(caminho.getLeaf(), raiz);
        }

        /** Métodos chamados a partir de uma raiz também executam na thread dela. */
        private void propagarChamadas() {
            boolean mudou = true;
            while (mudou) {
                mudou = false;
                for (TreePath invocacao : invocacoes) {
                    Raiz chamador = raizDe(invocacao);
                    TreePath alvo = chamador == null ? null : metodos.get(trees.getElement(invocacao));
                    if (alvo == null) continue;
                    Raiz chamado = raizes.get(alvo.getLeaf());
                    if (chamado == null) {
                        chamado = new Raiz(alvo, false);
                        raizes.put(alvo.getLeaf(), chamado);
                        mudou = true;
                    }
                    if (chamado == chamador || chamado.entrada) continue;
                    Chamada nova = new Chamada(protegido(invocacao), chamadaViaThis(invocacao));
                    Chamada anterior = chamado.chamadores.get(chamador);
                    Chamada juntada = anterior == null ? nova : anterior.juntar(nova);
                    if (!juntada.equals(anterior)) {
                        chamado.chamadores.put(chamador, juntada);
                        mudou = true;
                    }
                }
            }

            mudou = true;
            while (mudou) {
                mudou = false;
                for (Raiz raiz : raizes.values()) {
                    if (raiz.entrada) continue;
                    Set<Raiz> origens = new LinkedHashSet<>();
                    boolean multiInstancia = false, multiClasse = false, protegida = true;
                    for (Map.Entry<Raiz, Chamada> chamada : raiz.chamadores.entrySet()) {
                        Raiz chamador = chamada.getKey();
                        origens.addAll(chamador.origens);
                        multiInstancia |= chamada.getValue().viaThis() ? chamador.multiInstancia : chamador.multiClasse;
                        multiClasse |= chamador.multiClasse;
                        protegida &= chamada.getValue().protegida() || chamador.protegidaPeloChamador;
                    }
                    if (origens.size() >= 2) {
                        multiInstancia = true;
                        multiClasse = true;
                    }
                    if (!origens.equals(raiz.origens) || multiInstancia != raiz.multiInstancia
                            || multiClasse != raiz.multiClasse || protegida != raiz.protegidaPeloChamador) {
                        raiz.origens.addAll(origens);
                        raiz.multiInstancia = multiInstancia;
                        raiz.multiClasse = multiClasse;
                        raiz.protegidaPeloChamador = protegida;
                        mudou = true;
                    }
                }
            }
        }

        private void coletarAcessos() {
            for (Map.Entry<VariableElement, List<TreePath>> entrada : usos.entrySet()) {
                for (TreePath uso : entrada.getValue()) {
                    Raiz raiz = raizDe(uso);
                    boolean protegido = protegido(uso) || (raiz != null && raiz.protegidaPeloChamador);
                    acessos.add(new Acesso(entrada.getKey(), uso, raiz, ehEscrita(uso),
                            METODOS_QUE_MODIFICAM.contains(metodoInvocadoSobre(uso)), protegido, usoViaThis(uso)));
                }
            }
        }

        /* ───────────── Regras ───────────── */

        /** Campo alterado por mais de uma thread sem sincronização. */
        private void detectarEscritaCompartilhada() {
            for (Map.Entry<VariableElement, List<Acesso>> grupo : acessosEmThreadsPorVariavel().entrySet()) {
                VariableElement campo = grupo.getKey();
                Set<Modifier> modificadores = campo.getModifiers();
                if (campo.getKind() != ElementKind.FIELD
                        || modificadores.contains(Modifier.FINAL) || modificadores.contains(Modifier.VOLATILE)) continue;

                Optional<Acesso> escrita = primeiro(grupo.getValue(), a -> a.escrita() && !a.protegido());
                if (escrita.isPresent() && acessoConcorrente(campo, grupo.getValue())) {
                    reportar("SRC_UNSYNCHRONIZED_SHARED_WRITE", escrita.get().caminho(),
                            "Condição de corrida: o campo '" + campo.getSimpleName() + "' é alterado por mais de uma thread "
                                    + "sem sincronização (synchronized, Lock ou tipo atômico).");
                }
            }
        }

        /** ArrayList, HashMap etc. modificados por mais de uma thread sem sincronização. */
        private void detectarColecaoNaoThreadSafe() {
            for (Map.Entry<VariableElement, List<Acesso>> grupo : acessosEmThreadsPorVariavel().entrySet()) {
                VariableElement colecao = grupo.getKey();
                if (colecao.getKind() == ElementKind.PARAMETER || !colecaoNaoThreadSafe(colecao)) continue;

                // Coleção criada dentro da própria thread fica confinada a ela.
                TreePath declaracao = declaracoes.get(colecao);
                List<Acesso> compartilhados = grupo.getValue().stream()
                        .filter(a -> declaracao == null || !contem(declaracao, a.raiz().caminho.getLeaf()))
                        .toList();

                Optional<Acesso> mutacao = primeiro(compartilhados, a -> a.mutacao() && !a.protegido());
                if (mutacao.isPresent() && acessoConcorrente(colecao, compartilhados)) {
                    reportar("SRC_NON_THREAD_SAFE_COLLECTION", mutacao.get().caminho(),
                            "A coleção '" + colecao.getSimpleName() + "' (" + nomeDaImplementacao(colecao) + ") não é thread-safe "
                                    + "e é modificada por mais de uma thread sem sincronização.");
                }
            }
        }

        /** A thread principal lê um valor produzido por outra thread sem esperar por ela. */
        private void detectarLeituraSemJoin() {
            for (TreePath inicioThread : invocacoes) {
                MethodInvocationTree chamada = (MethodInvocationTree) inicioThread.getLeaf();
                if (raizDe(inicioThread) != null || !chamada.getArguments().isEmpty()
                        || !(chamada.getMethodSelect() instanceof MemberSelectTree selecao)
                        || !selecao.getIdentifier().contentEquals("start")) continue;
                if (!(trees.getElement(new TreePath(new TreePath(inicioThread, selecao), selecao.getExpression())) instanceof VariableElement thread)
                        || !eh(thread.asType(), "java.lang.Thread")) continue;

                Raiz origem = raizDaThread(thread);
                TreePath metodo = fronteira(inicioThread);
                if (origem == null || metodo == null) continue;

                long inicio = inicio(chamada);
                List<Long> esperas = invocacoes.stream()
                        .filter(i -> contem(i, metodo.getLeaf()) && esperaPelaThread(i, thread))
                        .map(i -> inicio(i.getLeaf()))
                        .toList();
                Set<VariableElement> alteradas = acessos.stream()
                        .filter(a -> a.raiz() != null && a.raiz().origens.contains(origem) && (a.escrita() || a.mutacao()))
                        .map(Acesso::variavel)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

                for (VariableElement variavel : alteradas) {
                    usos.getOrDefault(variavel, List.of()).stream()
                            .filter(u -> contem(u, metodo.getLeaf()) && raizDe(u) == null && !ehEscrita(u))
                            .filter(u -> inicio(u.getLeaf()) > inicio
                                    && esperas.stream().noneMatch(e -> e > inicio && e < inicio(u.getLeaf())))
                            .min(Comparator.comparingLong(u -> inicio(u.getLeaf())))
                            .ifPresent(u -> reportar("SRC_READ_WITHOUT_JOIN", u,
                                    "A thread principal lê '" + variavel.getSimpleName() + "' depois de iniciar a thread '"
                                            + thread.getSimpleName() + "', sem esperar que ela termine com join(); "
                                            + "o valor lido pode ainda não ter sido calculado."));
                }
            }
        }

        /** Laço de uma thread controlado por um campo não volatile que outra thread altera. */
        private void detectarFlagSemVolatile() {
            Set<VariableElement> reportados = new HashSet<>();
            for (TreePath laco : lacos) {
                Raiz raiz = raizDe(laco);
                ExpressionTree condicao = condicaoDoLaco(laco.getLeaf());
                if (raiz == null || condicao == null || raiz.protegidaPeloChamador
                        || protegido(new TreePath(laco, condicao))) continue;

                for (Map.Entry<VariableElement, List<TreePath>> uso : usos.entrySet()) {
                    VariableElement campo = uso.getKey();
                    if (!candidatoAFlag(campo) || reportados.contains(campo)
                            || uso.getValue().stream().noneMatch(u -> contem(u, condicao))) continue;

                    boolean alteradoPorOutraThread = acessos.stream().anyMatch(a -> a.variavel().equals(campo) && a.escrita()
                            && (a.raiz() == null || Collections.disjoint(a.raiz().origens, raiz.origens)));
                    if (alteradoPorOutraThread) {
                        reportados.add(campo);
                        reportar("SRC_NON_VOLATILE_FLAG", new TreePath(laco, condicao),
                                "O campo '" + campo.getSimpleName() + "' controla o laço desta thread, mas é alterado por outra thread "
                                        + "e não é volatile nem protegido por sincronização; a thread pode nunca enxergar a mudança.");
                    }
                }
            }
        }

        /** Dois locks adquiridos em ordem inversa em pontos diferentes do código. */
        private void detectarDeadlockPorOrdemDeLocks() {
            List<ParDeLocks> pares = new ArrayList<>();
            for (TreePath sincronizado : sincronizados) {
                Object interno = chaveDoLock(new TreePath(sincronizado, ((SynchronizedTree) sincronizado.getLeaf()).getExpression()));
                adicionarPares(pares, locksMantidos(sincronizado), interno, sincronizado);
            }
            for (TreePath invocacao : invocacoes) {
                if (ehAquisicaoDeLock(invocacao)) {
                    adicionarPares(pares, locksMantidos(invocacao), chaveDoReceptor(invocacao), invocacao);
                }
                // Um nível de chamada: método que adquire locks, chamado com outro lock já adquirido.
                TreePath alvo = metodos.get(trees.getElement(invocacao));
                if (alvo != null) {
                    Set<Object> mantidos = locksMantidos(invocacao);
                    if (!mantidos.isEmpty()) {
                        for (Object adquirido : locksAdquiridosPor(alvo)) {
                            adicionarPares(pares, mantidos, adquirido, invocacao);
                        }
                    }
                }
            }

            Set<Set<Object>> reportados = new HashSet<>();
            for (int i = 0; i < pares.size(); i++) {
                for (int j = i + 1; j < pares.size(); j++) {
                    ParDeLocks a = pares.get(i), b = pares.get(j);
                    if (!a.externo().equals(b.interno()) || !a.interno().equals(b.externo())) continue;
                    if (ehParametro(a.externo()) || ehParametro(a.interno())) continue;
                    if (raizDe(a.caminho()) == null && raizDe(b.caminho()) == null) continue;
                    if (!reportados.add(Set.of(a.externo(), a.interno()))) continue;

                    ParDeLocks primeiro = inicio(a.caminho().getLeaf()) <= inicio(b.caminho().getLeaf()) ? a : b;
                    ParDeLocks segundo = primeiro == a ? b : a;
                    reportar("SRC_LOCK_ORDER_DEADLOCK", segundo.caminho(),
                            "Possível deadlock: os locks '" + nomeDoLock(primeiro.externo()) + "' e '" + nomeDoLock(primeiro.interno())
                                    + "' são adquiridos em ordem inversa (linha " + linha(primeiro.caminho().getLeaf()) + ": "
                                    + nomeDoLock(primeiro.externo()) + " → " + nomeDoLock(primeiro.interno()) + "; linha "
                                    + linha(segundo.caminho().getLeaf()) + ": " + nomeDoLock(segundo.externo()) + " → "
                                    + nomeDoLock(segundo.interno()) + ").");
                }
            }

            detectarDeadlockPorArgumentosInvertidos(pares);
        }

        /** Caso clássico da transferência: synchronized(de) { synchronized(para) } chamado com (a, b) e (b, a). */
        private void detectarDeadlockPorArgumentosInvertidos(List<ParDeLocks> pares) {
            for (ParDeLocks par : pares) {
                if (!(par.externo() instanceof VariableElement p1) || !(par.interno() instanceof VariableElement p2)
                        || !ehParametro(p1) || !ehParametro(p2)
                        || !(p1.getEnclosingElement() instanceof ExecutableElement metodo)
                        || !metodo.equals(p2.getEnclosingElement())) continue;
                int i1 = metodo.getParameters().indexOf(p1), i2 = metodo.getParameters().indexOf(p2);
                if (i1 < 0 || i2 < 0) continue;

                List<List<Object>> combinacoes = new ArrayList<>();
                for (TreePath invocacao : invocacoes) {
                    if (!metodo.equals(trees.getElement(invocacao))) continue;
                    Object a1 = elementoDoArgumento(invocacao, i1), a2 = elementoDoArgumento(invocacao, i2);
                    if (a1 == null || a2 == null || a1.equals(a2)) continue;
                    if (combinacoes.contains(List.of(a2, a1))) {
                        reportar("SRC_LOCK_ORDER_DEADLOCK", par.caminho(),
                                "Possível deadlock: '" + metodo.getSimpleName() + "' trava '" + p1.getSimpleName() + "' e depois '"
                                        + p2.getSimpleName() + "', e é chamado com os argumentos em ordem inversa ("
                                        + nomeDoLock(a1) + ", " + nomeDoLock(a2) + " e " + nomeDoLock(a2) + ", " + nomeDoLock(a1) + ").");
                        break;
                    }
                    combinacoes.add(List.of(a1, a2));
                }
            }
        }

        /** wait()/notify() chamados sem possuir o monitor do objeto. */
        private void detectarMonitorNaoAdquirido() {
            for (TreePath invocacao : invocacoes) {
                if (!(trees.getElement(invocacao) instanceof ExecutableElement metodo)) continue;
                String nome = metodo.getSimpleName().toString();
                if (!Set.of("wait", "notify", "notifyAll").contains(nome)
                        || !(metodo.getEnclosingElement() instanceof TypeElement dono)
                        || !dono.getQualifiedName().contentEquals("java.lang.Object")) continue;

                MethodInvocationTree chamada = (MethodInvocationTree) invocacao.getLeaf();
                Object monitor = chamada.getMethodSelect() instanceof MemberSelectTree selecao
                        ? chaveDoLock(new TreePath(new TreePath(invocacao, selecao), selecao.getExpression()))
                        : new ChaveThis(classeEnvolvente(invocacao));
                if (monitor == null || locksMantidos(invocacao).contains(monitor) || chamadoSomenteComLock(invocacao)) continue;

                reportar("SRC_MONITOR_NOT_HELD", invocacao,
                        "'" + nome + "()' é chamado em '" + nomeDoLock(monitor) + "' sem que a thread possua o monitor desse objeto "
                                + "(falta synchronized(" + nomeDoLock(monitor) + ")); em tempo de execução isso lança IllegalMonitorStateException.");
            }
        }

        /** InterruptedException engolida dentro de um laço: a thread não consegue ser encerrada. */
        private void detectarInterrupcaoIgnorada() {
            TypeMirror interrupcao = elements.getTypeElement("java.lang.InterruptedException").asType();
            for (TreePath captura : capturas) {
                CatchTree catchTree = (CatchTree) captura.getLeaf();
                Element parametro = trees.getElement(new TreePath(captura, catchTree.getParameter()));
                if (parametro == null || raizDe(captura) == null || !emLaco(captura)) continue;
                boolean capturaInterrupcao = tiposCapturados(parametro.asType()).stream()
                        .anyMatch(t -> types.isSameType(types.erasure(t), interrupcao));
                if (capturaInterrupcao && !preservaInterrupcao(catchTree.getBlock())) {
                    reportar("SRC_INTERRUPT_SWALLOWED_IN_LOOP", captura,
                            "A InterruptedException é capturada e ignorada dentro de um laço: o pedido de interrupção é descartado "
                                    + "e a thread não consegue ser encerrada.");
                }
            }
        }

        /** ExecutorService que recebe tarefas e nunca é encerrado: o programa não termina. */
        private void detectarExecutorSemShutdown() {
            for (Map.Entry<VariableElement, TreePath> declaracao : declaracoes.entrySet()) {
                VariableElement executor = declaracao.getKey();
                if (!eh(executor.asType(), "java.util.concurrent.ExecutorService")
                        || !valoresAtribuidos.containsKey(executor)
                        || declaracao.getValue().getParentPath().getLeaf() instanceof TryTree) continue; // try-with-resources

                List<TreePath> usosDoExecutor = usos.getOrDefault(executor, List.of());
                boolean recebeTarefas = usosDoExecutor.stream().anyMatch(u -> METODOS_QUE_ENVIAM_TAREFAS.contains(metodoInvocadoSobre(u)));
                boolean encerrado = usosDoExecutor.stream().anyMatch(u -> METODOS_QUE_ENCERRAM_EXECUTOR.contains(metodoInvocadoSobre(u)));
                boolean escapa = usosDoExecutor.stream().anyMatch(this::escapa);
                if (recebeTarefas && !encerrado && !escapa) {
                    reportar("SRC_EXECUTOR_NOT_SHUTDOWN", declaracao.getValue(),
                            "O ExecutorService '" + executor.getSimpleName() + "' recebe tarefas mas nunca é encerrado com shutdown(): "
                                    + "as threads do pool continuam ativas e o programa não termina.");
                }
            }
        }

        /* ───────────── Multiplicidade e concorrência ───────────── */

        private boolean acessoConcorrente(VariableElement variavel, List<Acesso> acessosEmThreads) {
            boolean estatico = variavel.getModifiers().contains(Modifier.STATIC);
            Set<Raiz> origens = new HashSet<>();
            for (Acesso acesso : acessosEmThreads) {
                Raiz raiz = acesso.raiz();
                boolean multi = !estatico && acesso.viaThis() ? raiz.multiInstancia : raiz.multiClasse;
                if (multi) return true;
                origens.addAll(raiz.origens);
            }
            return origens.size() >= 2;
        }

        private Map<VariableElement, List<Acesso>> acessosEmThreadsPorVariavel() {
            Map<VariableElement, List<Acesso>> grupos = new LinkedHashMap<>();
            for (Acesso acesso : acessos) {
                if (acesso.raiz() != null) {
                    grupos.computeIfAbsent(acesso.variavel(), k -> new ArrayList<>()).add(acesso);
                }
            }
            return grupos;
        }

        private Raiz raizDe(TreePath caminho) {
            for (Tree t : caminho) {
                Raiz raiz = raizes.get(t);
                if (raiz != null) return raiz;
            }
            return null;
        }

        /** A expressão (lambda, new ...) é guardada numa variável usada como argumento mais de uma vez. */
        private boolean usoMultiplo(TreePath expressao) {
            TreePath pai = expressao.getParentPath();
            if (!(pai.getLeaf() instanceof VariableTree) || !(trees.getElement(pai) instanceof VariableElement variavel)) return false;
            List<TreePath> comoArgumento = usos.getOrDefault(variavel, List.of()).stream().filter(this::ehArgumento).toList();
            if (comoArgumento.size() >= 2) return true;
            Set<Tree> lacosDaDeclaracao = lacosAte(pai);
            return comoArgumento.stream().anyMatch(u -> !lacosDaDeclaracao.containsAll(lacosAte(u)));
        }

        private boolean emLaco(TreePath caminho) {
            for (TreePath p = caminho.getParentPath(); p != null; p = p.getParentPath()) {
                if (ehLaco(p.getLeaf())) return true;
                if (ehFronteira(p.getLeaf())) return false;
            }
            return false;
        }

        private Set<Tree> lacosAte(TreePath caminho) {
            Set<Tree> encontrados = new HashSet<>();
            for (TreePath p = caminho.getParentPath(); p != null; p = p.getParentPath()) {
                Tree t = p.getLeaf();
                if (ehLaco(t)) encontrados.add(t);
                if (t instanceof MethodTree || t instanceof ClassTree) break;
            }
            return encontrados;
        }

        private List<TreePath> instanciacoesDe(Element classe) {
            List<TreePath> resultado = new ArrayList<>();
            for (TreePath caminho : instanciacoes) {
                NewClassTree criacao = (NewClassTree) caminho.getLeaf();
                Element criada = criacao.getClassBody() != null
                        ? trees.getElement(new TreePath(caminho, criacao.getClassBody()))
                        : Optional.ofNullable(trees.getElement(caminho)).map(Element::getEnclosingElement).orElse(null);
                if (classe.equals(criada)) resultado.add(caminho);
            }
            return resultado;
        }

        private Raiz raizDaThread(VariableElement thread) {
            for (TreePath valor : valoresAtribuidos.getOrDefault(thread, List.of())) {
                TreePath p = semParenteses(valor);
                if (!(p.getLeaf() instanceof NewClassTree criacao)) continue;
                if (criacao.getClassBody() != null) {
                    return raizDoRun(trees.getElement(new TreePath(p, criacao.getClassBody())));
                }
                Element construtor = trees.getElement(p);
                if (construtor == null) continue;
                Element classe = construtor.getEnclosingElement();
                if (classe instanceof TypeElement tipo && tipo.getQualifiedName().contentEquals("java.lang.Thread")) {
                    for (ExpressionTree argumento : criacao.getArguments()) {
                        Raiz raiz = raizDaTarefa(new TreePath(p, argumento), 0);
                        if (raiz != null) return raiz;
                    }
                } else {
                    return raizDoRun(classe);
                }
            }
            return null;
        }

        private Raiz raizDaTarefa(TreePath expressao, int profundidade) {
            TreePath p = semParenteses(expressao);
            Tree t = p.getLeaf();
            if (t instanceof LambdaExpressionTree) return raizes.get(t);
            if (t instanceof MemberReferenceTree) {
                TreePath metodo = metodos.get(trees.getElement(p));
                return metodo == null ? null : raizes.get(metodo.getLeaf());
            }
            if (t instanceof NewClassTree criacao) {
                if (criacao.getClassBody() != null) return raizDoRun(trees.getElement(new TreePath(p, criacao.getClassBody())));
                Element construtor = trees.getElement(p);
                return construtor == null ? null : raizDoRun(construtor.getEnclosingElement());
            }
            if (profundidade < 3 && trees.getElement(p) instanceof VariableElement variavel) {
                for (TreePath valor : valoresAtribuidos.getOrDefault(variavel, List.of())) {
                    Raiz raiz = raizDaTarefa(valor, profundidade + 1);
                    if (raiz != null) return raiz;
                }
            }
            return null;
        }

        private Raiz raizDoRun(Element classe) {
            for (Map.Entry<Element, TreePath> metodo : metodos.entrySet()) {
                if (metodo.getKey().getEnclosingElement().equals(classe) && ehRunOuCall((ExecutableElement) metodo.getKey())) {
                    return raizes.get(metodo.getValue().getLeaf());
                }
            }
            return null;
        }

        private boolean esperaPelaThread(TreePath invocacao, VariableElement thread) {
            MethodInvocationTree chamada = (MethodInvocationTree) invocacao.getLeaf();
            String nome = nomeDoMetodo(chamada);
            if (METODOS_DE_ESPERA.contains(nome)) return true;
            return (nome.equals("join") || nome.equals("isAlive"))
                    && chamada.getMethodSelect() instanceof MemberSelectTree selecao
                    && thread.equals(trees.getElement(new TreePath(new TreePath(invocacao, selecao), selecao.getExpression())));
        }

        /* ───────────── Locks ───────────── */

        private boolean protegido(TreePath caminho) {
            return !locksMantidos(caminho).isEmpty();
        }

        /** Locks que a thread possui ao executar este ponto: synchronized envolventes e Lock.lock() anteriores. */
        private Set<Object> locksMantidos(TreePath caminho) {
            Set<Object> locks = new LinkedHashSet<>();
            Tree filho = caminho.getLeaf();
            for (TreePath p = caminho.getParentPath(); p != null; p = p.getParentPath()) {
                Tree t = p.getLeaf();
                if (t instanceof SynchronizedTree sincronizado && sincronizado.getBlock() == filho) {
                    locks.add(chaveOuDesconhecida(chaveDoLock(new TreePath(p, sincronizado.getExpression()))));
                }
                if (t instanceof MethodTree metodo && metodo.getModifiers().getFlags().contains(Modifier.SYNCHRONIZED)) {
                    locks.add(chaveDoMetodoSincronizado(p));
                }
                if (ehFronteira(t)) {
                    if (!(t instanceof ClassTree)) locks.addAll(locksExplicitosAntesDe(t, inicio(caminho.getLeaf())));
                    break;
                }
                filho = t;
            }
            return locks;
        }

        private Set<Object> locksExplicitosAntesDe(Tree fronteira, long posicao) {
            Map<Object, Boolean> adquirido = new LinkedHashMap<>();
            invocacoes.stream()
                    .filter(i -> inicio(i.getLeaf()) < posicao && ehOperacaoDeLock(i) && fronteiraEh(i, fronteira))
                    .sorted(Comparator.comparingLong(i -> inicio(i.getLeaf())))
                    .forEach(i -> adquirido.put(chaveDoReceptor(i),
                            !nomeDoMetodo((MethodInvocationTree) i.getLeaf()).equals("unlock")));
            return adquirido.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        private Set<Object> locksAdquiridosPor(TreePath metodo) {
            Set<Object> locks = new LinkedHashSet<>();
            MethodTree metodoTree = (MethodTree) metodo.getLeaf();
            if (metodoTree.getModifiers().getFlags().contains(Modifier.SYNCHRONIZED)) {
                locks.add(chaveDoMetodoSincronizado(metodo));
            }
            for (TreePath sincronizado : sincronizados) {
                if (fronteiraEh(sincronizado, metodoTree)) {
                    Object chave = chaveDoLock(new TreePath(sincronizado, ((SynchronizedTree) sincronizado.getLeaf()).getExpression()));
                    if (chave != null && !ehLocal(chave)) locks.add(chave);
                }
            }
            for (TreePath invocacao : invocacoes) {
                if (fronteiraEh(invocacao, metodoTree) && ehAquisicaoDeLock(invocacao)) {
                    Object chave = chaveDoReceptor(invocacao);
                    if (!ehLocal(chave)) locks.add(chave);
                }
            }
            return locks;
        }

        private void adicionarPares(List<ParDeLocks> pares, Set<Object> externos, Object interno, TreePath caminho) {
            if (interno == null) return;
            for (Object externo : externos) {
                if (!externo.equals(interno)) pares.add(new ParDeLocks(externo, interno, caminho));
            }
        }

        private boolean chamadoSomenteComLock(TreePath invocacao) {
            TreePath metodo = fronteira(invocacao);
            if (metodo == null || !(metodo.getLeaf() instanceof MethodTree)) return false;
            Element elemento = trees.getElement(metodo);
            List<TreePath> chamadas = invocacoes.stream().filter(i -> elemento != null && elemento.equals(trees.getElement(i))).toList();
            return !chamadas.isEmpty() && chamadas.stream().allMatch(this::protegido);
        }

        private Object chaveDoLock(TreePath caminho) {
            TreePath p = semParenteses(caminho);
            Tree t = p.getLeaf();
            if (t instanceof IdentifierTree id && id.getName().contentEquals("this")) {
                return new ChaveThis(classeEnvolvente(p));
            }
            if (t instanceof MemberSelectTree selecao
                    && (selecao.getIdentifier().contentEquals("class") || selecao.getIdentifier().contentEquals("this"))) {
                Element tipo = trees.getElement(new TreePath(p, selecao.getExpression()));
                if (tipo == null) return null;
                return selecao.getIdentifier().contentEquals("class") ? new ChaveClasse(tipo) : new ChaveThis(tipo);
            }
            Element elemento = trees.getElement(p);
            return elemento instanceof VariableElement ? elemento : null;
        }

        private Object chaveDoReceptor(TreePath invocacao) {
            MethodInvocationTree chamada = (MethodInvocationTree) invocacao.getLeaf();
            if (!(chamada.getMethodSelect() instanceof MemberSelectTree selecao)) return new ChaveThis(classeEnvolvente(invocacao));
            Object chave = chaveDoLock(new TreePath(new TreePath(invocacao, selecao), selecao.getExpression()));
            return chave != null ? chave : selecao.getExpression().toString();
        }

        private Object chaveDoMetodoSincronizado(TreePath metodo) {
            Element classe = classeEnvolvente(metodo);
            return ((MethodTree) metodo.getLeaf()).getModifiers().getFlags().contains(Modifier.STATIC)
                    ? new ChaveClasse(classe) : new ChaveThis(classe);
        }

        private Object chaveOuDesconhecida(Object chave) {
            return chave != null ? chave : new Object();
        }

        private boolean ehOperacaoDeLock(TreePath invocacao) {
            MethodInvocationTree chamada = (MethodInvocationTree) invocacao.getLeaf();
            return Set.of("lock", "lockInterruptibly", "tryLock", "unlock").contains(nomeDoMetodo(chamada))
                    && receptorEh(invocacao, "java.util.concurrent.locks.Lock");
        }

        private boolean ehAquisicaoDeLock(TreePath invocacao) {
            String nome = nomeDoMetodo((MethodInvocationTree) invocacao.getLeaf());
            // tryLock não entra: é justamente a forma de evitar o deadlock.
            return (nome.equals("lock") || nome.equals("lockInterruptibly")) && ehOperacaoDeLock(invocacao);
        }

        private boolean receptorEh(TreePath invocacao, String tipo) {
            MethodInvocationTree chamada = (MethodInvocationTree) invocacao.getLeaf();
            return chamada.getMethodSelect() instanceof MemberSelectTree selecao
                    && eh(trees.getTypeMirror(new TreePath(new TreePath(invocacao, selecao), selecao.getExpression())), tipo);
        }

        private String nomeDoLock(Object chave) {
            if (chave instanceof VariableElement variavel) return variavel.getSimpleName().toString();
            if (chave instanceof ChaveThis) return "this";
            if (chave instanceof ChaveClasse classe) return classe.classe().getSimpleName() + ".class";
            return String.valueOf(chave);
        }

        private boolean ehParametro(Object chave) {
            return chave instanceof VariableElement variavel && variavel.getKind() == ElementKind.PARAMETER;
        }

        private boolean ehLocal(Object chave) {
            return chave instanceof String || (chave instanceof VariableElement variavel && variavel.getKind() != ElementKind.FIELD);
        }

        private Object elementoDoArgumento(TreePath invocacao, int indice) {
            List<? extends ExpressionTree> argumentos = ((MethodInvocationTree) invocacao.getLeaf()).getArguments();
            if (indice >= argumentos.size()) return null;
            Element elemento = trees.getElement(semParenteses(new TreePath(invocacao, argumentos.get(indice))));
            return elemento instanceof VariableElement ? elemento : null;
        }

        /* ───────────── Auxiliares de árvore e tipos ───────────── */

        private boolean ehEscrita(TreePath uso) {
            Tree folha = uso.getLeaf();
            Tree pai = uso.getParentPath().getLeaf();
            if (pai instanceof AssignmentTree atribuicao) return atribuicao.getVariable() == folha;
            if (pai instanceof CompoundAssignmentTree composta) return composta.getVariable() == folha;
            if (pai instanceof UnaryTree unaria) {
                return unaria.getExpression() == folha && switch (unaria.getKind()) {
                    case PREFIX_INCREMENT, POSTFIX_INCREMENT, PREFIX_DECREMENT, POSTFIX_DECREMENT -> true;
                    default -> false;
                };
            }
            return false;
        }

        /** Nome do método chamado sobre a variável (ex.: "add" em lista.add(x)), ou "" se não houver. */
        private String metodoInvocadoSobre(TreePath uso) {
            TreePath pai = uso.getParentPath();
            if (pai.getLeaf() instanceof MemberSelectTree selecao && selecao.getExpression() == uso.getLeaf()
                    && pai.getParentPath().getLeaf() instanceof MethodInvocationTree chamada && chamada.getMethodSelect() == selecao) {
                return selecao.getIdentifier().toString();
            }
            return "";
        }

        private boolean usoViaThis(TreePath uso) {
            return uso.getLeaf() instanceof IdentifierTree
                    || (uso.getLeaf() instanceof MemberSelectTree selecao && ehThis(selecao.getExpression()));
        }

        private boolean chamadaViaThis(TreePath invocacao) {
            ExpressionTree selecao = ((MethodInvocationTree) invocacao.getLeaf()).getMethodSelect();
            return selecao instanceof IdentifierTree
                    || (selecao instanceof MemberSelectTree membro && ehThis(membro.getExpression()));
        }

        private boolean ehThis(ExpressionTree expressao) {
            return expressao instanceof IdentifierTree id && id.getName().contentEquals("this");
        }

        private boolean ehArgumento(TreePath uso) {
            Tree folha = uso.getLeaf();
            Tree pai = uso.getParentPath().getLeaf();
            return (pai instanceof MethodInvocationTree chamada && chamada.getArguments().contains(folha))
                    || (pai instanceof NewClassTree criacao && criacao.getArguments().contains(folha));
        }

        private boolean escapa(TreePath uso) {
            Tree folha = uso.getLeaf();
            Tree pai = uso.getParentPath().getLeaf();
            return pai instanceof ReturnTree || pai instanceof VariableTree || ehArgumento(uso)
                    || (pai instanceof AssignmentTree atribuicao && atribuicao.getExpression() == folha);
        }

        private boolean colecaoNaoThreadSafe(VariableElement variavel) {
            if (ehColecaoNaoThreadSafe(variavel.asType())) return true;
            for (TreePath valor : valoresAtribuidos.getOrDefault(variavel, List.of())) {
                TreePath p = semParenteses(valor);
                if (p.getLeaf() instanceof NewClassTree && ehColecaoNaoThreadSafe(trees.getTypeMirror(p))) return true;
            }
            return false;
        }

        private boolean ehColecaoNaoThreadSafe(TypeMirror tipo) {
            return COLECOES_NAO_THREAD_SAFE.stream().anyMatch(nome -> eh(tipo, nome));
        }

        private String nomeDaImplementacao(VariableElement variavel) {
            for (TreePath valor : valoresAtribuidos.getOrDefault(variavel, List.of())) {
                if (semParenteses(valor).getLeaf() instanceof NewClassTree criacao) {
                    String nome = criacao.getIdentifier().toString();
                    return nome.contains("<") ? nome.substring(0, nome.indexOf('<')) : nome;
                }
            }
            return types.erasure(variavel.asType()).toString().replaceAll(".*\\.", "");
        }

        private boolean candidatoAFlag(VariableElement campo) {
            Set<Modifier> modificadores = campo.getModifiers();
            return campo.getKind() == ElementKind.FIELD && campo.asType().getKind().isPrimitive()
                    && !modificadores.contains(Modifier.VOLATILE) && !modificadores.contains(Modifier.FINAL);
        }

        private ExpressionTree condicaoDoLaco(Tree laco) {
            if (laco instanceof WhileLoopTree enquanto) return enquanto.getCondition();
            if (laco instanceof DoWhileLoopTree facaEnquanto) return facaEnquanto.getCondition();
            if (laco instanceof ForLoopTree para) return para.getCondition();
            return null;
        }

        private List<? extends TypeMirror> tiposCapturados(TypeMirror tipo) {
            return tipo.getKind() == TypeKind.UNION ? ((UnionType) tipo).getAlternatives() : List.of(tipo);
        }

        private boolean preservaInterrupcao(BlockTree corpo) {
            boolean[] preserva = {false};
            new TreeScanner<Void, Void>() {
                @Override
                public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
                    String nome = nomeDoMetodo(node);
                    if (nome.equals("interrupt") || nome.equals("exit")) preserva[0] = true;
                    return super.visitMethodInvocation(node, p);
                }

                @Override
                public Void visitThrow(ThrowTree node, Void p) {
                    preserva[0] = true;
                    return null;
                }

                @Override
                public Void visitReturn(ReturnTree node, Void p) {
                    preserva[0] = true;
                    return null;
                }

                @Override
                public Void visitBreak(BreakTree node, Void p) {
                    preserva[0] = true;
                    return null;
                }
            }.scan(corpo, null);
            return preserva[0];
        }

        private boolean ehTipoDeTarefa(TypeMirror tipo) {
            return eh(tipo, "java.lang.Runnable") || eh(tipo, "java.util.concurrent.Callable");
        }

        private boolean argumentoDeExecucaoAssincrona(TreePath lambda) {
            return lambda.getParentPath().getLeaf() instanceof MethodInvocationTree chamada
                    && chamada.getArguments().contains(lambda.getLeaf())
                    && METODOS_ASSINCRONOS.contains(nomeDoMetodo(chamada));
        }

        private boolean ehRunOuCall(ExecutableElement metodo) {
            if (!metodo.getParameters().isEmpty()) return false;
            String nome = metodo.getSimpleName().toString();
            TypeMirror classe = metodo.getEnclosingElement().asType();
            return (nome.equals("run") && eh(classe, "java.lang.Runnable"))
                    || (nome.equals("call") && eh(classe, "java.util.concurrent.Callable"));
        }

        private boolean eh(TypeMirror tipo, String nomeQualificado) {
            if (tipo == null || (tipo.getKind() != TypeKind.DECLARED && tipo.getKind() != TypeKind.TYPEVAR)) return false;
            TypeElement alvo = elements.getTypeElement(nomeQualificado);
            return alvo != null && types.isSubtype(types.erasure(tipo), types.erasure(alvo.asType()));
        }

        private boolean ehPseudoVariavel(VariableElement variavel) {
            String nome = variavel.getSimpleName().toString();
            return nome.equals("this") || nome.equals("super") || nome.equals("class");
        }

        private Element classeEnvolvente(TreePath caminho) {
            for (TreePath p = caminho; p != null; p = p.getParentPath()) {
                if (p.getLeaf() instanceof ClassTree) return trees.getElement(p);
            }
            return null;
        }

        private TreePath fronteira(TreePath caminho) {
            for (TreePath p = caminho.getParentPath(); p != null; p = p.getParentPath()) {
                if (ehFronteira(p.getLeaf())) return p;
            }
            return null;
        }

        private boolean fronteiraEh(TreePath caminho, Tree fronteira) {
            TreePath encontrada = fronteira(caminho);
            return encontrada != null && encontrada.getLeaf() == fronteira;
        }

        private static boolean ehFronteira(Tree t) {
            return t instanceof MethodTree || t instanceof LambdaExpressionTree || t instanceof ClassTree;
        }

        private static boolean ehLaco(Tree t) {
            return t instanceof ForLoopTree || t instanceof EnhancedForLoopTree
                    || t instanceof WhileLoopTree || t instanceof DoWhileLoopTree;
        }

        private static boolean contem(TreePath caminho, Tree alvo) {
            for (Tree t : caminho) {
                if (t == alvo) return true;
            }
            return false;
        }

        private static TreePath semParenteses(TreePath caminho) {
            TreePath p = caminho;
            while (p.getLeaf() instanceof ParenthesizedTree parenteses) {
                p = new TreePath(p, parenteses.getExpression());
            }
            return p;
        }

        private static String nomeDoMetodo(MethodInvocationTree chamada) {
            ExpressionTree selecao = chamada.getMethodSelect();
            if (selecao instanceof IdentifierTree id) return id.getName().toString();
            if (selecao instanceof MemberSelectTree membro) return membro.getIdentifier().toString();
            return "";
        }

        private Optional<Acesso> primeiro(List<Acesso> lista, Predicate<Acesso> filtro) {
            return lista.stream().filter(filtro).min(Comparator.comparingLong(a -> inicio(a.caminho().getLeaf())));
        }

        private long inicio(Tree tree) {
            return posicoes.getStartPosition(unidade, tree);
        }

        private int linha(Tree tree) {
            return (int) unidade.getLineMap().getLineNumber(inicio(tree));
        }

        private void reportar(String tipo, TreePath caminho, String mensagem) {
            int linha = linha(caminho.getLeaf());
            boolean repetido = issues.stream().anyMatch(i -> i.getType().equals(tipo) && i.getLineNumber() == linha);
            if (!repetido) {
                issues.add(new ConcurrencyIssue(tipo, linha, mensagem));
            }
        }
    }
}
