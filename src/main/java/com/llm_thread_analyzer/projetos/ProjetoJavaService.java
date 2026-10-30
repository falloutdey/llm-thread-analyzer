package com.llm_thread_analyzer.projetos;

import com.llm_thread_analyzer.projetos.dto.ProjetosDtos.*;
import com.llm_thread_analyzer.projetos.entity.ArquivoJava;
import com.llm_thread_analyzer.projetos.entity.ProjetoJava;
import com.llm_thread_analyzer.projetos.repository.ArquivoJavaRepository;
import com.llm_thread_analyzer.projetos.repository.ProjetoJavaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Projetos e arquivos Java dos alunos.
 *
 * Toda operação recebe o idEstudante do token e busca o projeto já filtrando por ele:
 * projeto de outro aluno responde 404, como se não existisse.
 */
@Service
public class ProjetoJavaService {

    /** Nome de classe Java simples + ".java" (sem pastas, sem pacote no nome). */
    private static final Pattern NOME_ARQUIVO = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]{0,94}\\.java");

    /** Palavras reservadas não podem ser nome de classe. */
    private static final Set<String> RESERVADAS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while", "true", "false", "null", "var", "yield", "record", "sealed", "permits", "_");

    static final String ARQUIVO_INICIAL = "Main.java";

    private final ProjetoJavaRepository projetos;
    private final ArquivoJavaRepository arquivos;
    private final int maxProjetosPorAluno;
    private final int maxArquivosPorProjeto;
    private final int maxCaracteresArquivo;

    public ProjetoJavaService(ProjetoJavaRepository projetos, ArquivoJavaRepository arquivos,
                              @Value("${java.projetos.max-por-aluno:50}") int maxProjetosPorAluno,
                              @Value("${java.projetos.max-arquivos:30}") int maxArquivosPorProjeto,
                              @Value("${java.projetos.max-caracteres-arquivo:100000}") int maxCaracteresArquivo) {
        this.projetos = projetos;
        this.arquivos = arquivos;
        this.maxProjetosPorAluno = maxProjetosPorAluno;
        this.maxArquivosPorProjeto = maxArquivosPorProjeto;
        this.maxCaracteresArquivo = maxCaracteresArquivo;
    }

    // ===================== Projetos =====================

    @Transactional(readOnly = true)
    public List<ProjetoResumo> listar(long idEstudante) {
        List<ProjetoJava> lista = projetos.findByIdEstudanteOrderByAtualizadoEmDesc(idEstudante);
        if (lista.isEmpty()) return List.of();
        Map<Long, Long> quantidades = new HashMap<>();
        for (Object[] linha : arquivos.contarPorProjeto(lista.stream().map(ProjetoJava::getId).toList())) {
            quantidades.put((Long) linha[0], (Long) linha[1]);
        }
        return lista.stream().map(p -> ProjetoResumo.de(p, quantidades.getOrDefault(p.getId(), 0L))).toList();
    }

    @Transactional
    public ProjetoDetalhe criar(long idEstudante, NovoProjeto pedido) {
        String nome = validarNomeProjeto(pedido == null ? null : pedido.nome());
        if (projetos.countByIdEstudante(idEstudante) >= maxProjetosPorAluno) {
            throw new ProjetoException(HttpStatus.BAD_REQUEST,
                    "Limite de " + maxProjetosPorAluno + " projetos atingido. Exclua um projeto antes de criar outro.");
        }
        ProjetoJava projeto = projetos.save(new ProjetoJava(idEstudante, nome));
        ArquivoJava inicial = arquivos.save(new ArquivoJava(projeto, ARQUIVO_INICIAL, modelo(ARQUIVO_INICIAL)));
        return ProjetoDetalhe.de(projeto, List.of(inicial));
    }

    @Transactional(readOnly = true)
    public ProjetoDetalhe abrir(long idEstudante, long idProjeto) {
        ProjetoJava projeto = projetoDoAluno(idEstudante, idProjeto);
        return ProjetoDetalhe.de(projeto, arquivos.findByProjetoOrderByNomeAsc(projeto));
    }

    @Transactional
    public ProjetoResumo renomear(long idEstudante, long idProjeto, RenomearProjeto pedido) {
        ProjetoJava projeto = projetoDoAluno(idEstudante, idProjeto);
        projeto.setNome(validarNomeProjeto(pedido == null ? null : pedido.nome()));
        projeto.tocar();
        return ProjetoResumo.de(projeto, arquivos.countByProjeto(projeto));
    }

    @Transactional
    public void excluir(long idEstudante, long idProjeto) {
        ProjetoJava projeto = projetoDoAluno(idEstudante, idProjeto);
        arquivos.apagarDoProjeto(projeto);
        projetos.delete(projeto);
    }

    // ===================== Arquivos =====================

    @Transactional
    public ArquivoDto criarArquivo(long idEstudante, long idProjeto, NovoArquivo pedido) {
        ProjetoJava projeto = projetoDoAluno(idEstudante, idProjeto);
        String nome = validarNomeArquivo(pedido == null ? null : pedido.nome());
        if (arquivos.countByProjeto(projeto) >= maxArquivosPorProjeto) {
            throw new ProjetoException(HttpStatus.BAD_REQUEST,
                    "Limite de " + maxArquivosPorProjeto + " arquivos por projeto atingido.");
        }
        if (arquivos.existsByProjetoAndNomeIgnoreCase(projeto, nome)) {
            throw new ProjetoException(HttpStatus.CONFLICT, "Já existe um arquivo chamado " + nome + " neste projeto.");
        }
        String conteudo = pedido.conteudo() == null || pedido.conteudo().isEmpty()
                ? modelo(nome)
                : validarConteudo(pedido.conteudo());
        ArquivoJava arquivo = arquivos.save(new ArquivoJava(projeto, nome, conteudo));
        projeto.tocar();
        return ArquivoDto.de(arquivo);
    }

    @Transactional
    public ArquivoDto alterarArquivo(long idEstudante, long idProjeto, long idArquivo, AlterarArquivo pedido) {
        ProjetoJava projeto = projetoDoAluno(idEstudante, idProjeto);
        ArquivoJava arquivo = arquivoDoProjeto(projeto, idArquivo);
        if (pedido == null || (pedido.nome() == null && pedido.conteudo() == null)) {
            throw new ProjetoException(HttpStatus.BAD_REQUEST, "Informe o novo nome ou o novo conteúdo do arquivo.");
        }
        if (pedido.nome() != null) {
            String nome = validarNomeArquivo(pedido.nome());
            if (!nome.equalsIgnoreCase(arquivo.getNome()) && arquivos.existsByProjetoAndNomeIgnoreCase(projeto, nome)) {
                throw new ProjetoException(HttpStatus.CONFLICT, "Já existe um arquivo chamado " + nome + " neste projeto.");
            }
            arquivo.setNome(nome);
        }
        if (pedido.conteudo() != null) {
            arquivo.setConteudo(validarConteudo(pedido.conteudo()));
        }
        arquivo.tocar();
        projeto.tocar();
        return ArquivoDto.de(arquivo);
    }

    @Transactional
    public void excluirArquivo(long idEstudante, long idProjeto, long idArquivo) {
        ProjetoJava projeto = projetoDoAluno(idEstudante, idProjeto);
        arquivos.delete(arquivoDoProjeto(projeto, idArquivo));
        projeto.tocar();
    }

    // ===================== Apoio =====================

    private ProjetoJava projetoDoAluno(long idEstudante, long idProjeto) {
        return projetos.findByIdAndIdEstudante(idProjeto, idEstudante)
                .orElseThrow(() -> new ProjetoException(HttpStatus.NOT_FOUND, "Projeto não encontrado."));
    }

    private ArquivoJava arquivoDoProjeto(ProjetoJava projeto, long idArquivo) {
        return arquivos.findByIdAndProjeto(idArquivo, projeto)
                .orElseThrow(() -> new ProjetoException(HttpStatus.NOT_FOUND, "Arquivo não encontrado."));
    }

    static String validarNomeProjeto(String nome) {
        String limpo = nome == null ? "" : nome.strip();
        if (limpo.isEmpty()) {
            throw new ProjetoException(HttpStatus.BAD_REQUEST, "Informe o nome do projeto.");
        }
        if (limpo.length() > 100) {
            throw new ProjetoException(HttpStatus.BAD_REQUEST, "O nome do projeto pode ter até 100 caracteres.");
        }
        if (limpo.codePoints().anyMatch(Character::isISOControl)) {
            throw new ProjetoException(HttpStatus.BAD_REQUEST, "O nome do projeto tem caracteres inválidos.");
        }
        return limpo;
    }

    static String validarNomeArquivo(String nome) {
        String limpo = nome == null ? "" : nome.strip();
        if (!NOME_ARQUIVO.matcher(limpo).matches()) {
            throw new ProjetoException(HttpStatus.BAD_REQUEST,
                    "Nome de arquivo inválido. Use o nome da classe seguido de .java (ex.: Contador.java), sem espaços, acentos ou pastas.");
        }
        String classe = limpo.substring(0, limpo.length() - ".java".length());
        if (RESERVADAS.contains(classe)) {
            throw new ProjetoException(HttpStatus.BAD_REQUEST, "\"" + classe + "\" é uma palavra reservada do Java e não pode ser nome de classe.");
        }
        return limpo;
    }

    private String validarConteudo(String conteudo) {
        if (conteudo.length() > maxCaracteresArquivo) {
            throw new ProjetoException(HttpStatus.BAD_REQUEST,
                    "O arquivo passou do limite de " + maxCaracteresArquivo + " caracteres.");
        }
        return conteudo;
    }

    /** Mesmo modelo que o editor usa ao criar um arquivo .java. */
    static String modelo(String nomeArquivo) {
        String classe = nomeArquivo.substring(0, nomeArquivo.length() - ".java".length());
        return "public class " + classe + " {\n\n"
                + "    public static void main(String[] args) {\n"
                + "        // Escreva seu código aqui\n"
                + "    }\n\n"
                + "}\n";
    }
}
