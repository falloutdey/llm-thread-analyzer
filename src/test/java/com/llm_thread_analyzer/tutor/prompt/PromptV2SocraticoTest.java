package com.llm_thread_analyzer.tutor.prompt;

import com.llm_thread_analyzer.tutor.entity.Mensagem;
import com.llm_thread_analyzer.tutor.entity.TutorEnums.Autor;
import com.llm_thread_analyzer.tutor.entity.TutorEnums.TipoMensagem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Partes do prompt socrático que não dependem do modelo (revisão 5). */
class PromptV2SocraticoTest {

    @Test
    void numeraAsLinhasDeUmArquivo() {
        String numerado = PromptV2Socratico.numerarLinhas("public class A {\n    int x;\n}\n");
        assertEquals("1| public class A {\n2|     int x;\n3| }", numerado);
    }

    @Test
    void numeracaoRecomecaEmCadaArquivoDoProjeto() {
        String projeto = "// ===== Arquivo: a/A.java =====\nclass A {}\n\n"
                + "// ===== Arquivo: b/B.java =====\nclass B {\n}\n\n";
        String numerado = PromptV2Socratico.numerarLinhas(projeto);
        assertTrue(numerado.startsWith("// ===== Arquivo: a/A.java =====\n1| class A {}\n"), numerado);
        assertTrue(numerado.contains("// ===== Arquivo: b/B.java =====\n1| class B {\n2| }"), numerado);
    }

    @Test
    void tiraAsteriscosDeEnfaseMasMantemCrasesEMultiplicacao() {
        assertEquals("Isso é um deadlock.", PromptV2Socratico.semEnfase("Isso é um *deadlock*."));
        assertEquals("Isso é um deadlock.", PromptV2Socratico.semEnfase("Isso é um **deadlock**."));
        assertEquals("Use `synchronized` aqui.", PromptV2Socratico.semEnfase("Use `synchronized` aqui."));
        assertEquals("2 * 3 = 6", PromptV2Socratico.semEnfase("2 * 3 = 6"));
    }

    @Test
    void reconhecePedidoParaExecutar() {
        assertTrue(PromptV2Socratico.pedeParaExecutar("Execute o programa algumas vezes."));
        assertTrue(PromptV2Socratico.pedeParaExecutar("Tente executar o programa e veja a saída."));
        assertFalse(PromptV2Socratico.pedeParaExecutar("O que acontece quando duas threads executam ao mesmo tempo?"));
    }

    @Test
    void estadoAvisaQueOTutorJaPediuParaExecutar() {
        Mensagem tutor = new Mensagem();
        tutor.setAutor(Autor.TUTOR);
        tutor.setTexto("Execute o programa algumas vezes. O que você nota?");
        tutor.setNivelDica(0);
        Mensagem aluno = new Mensagem();
        aluno.setAutor(Autor.ALUNO);
        aluno.setTipo(TipoMensagem.PERGUNTA);
        aluno.setTexto("não sei");

        assertTrue(PromptV2Socratico.calcularEstado(List.of(tutor, aluno)).jaPediuExecutar());

        tutor.setTexto("O que você espera que o programa imprima?");
        assertFalse(PromptV2Socratico.calcularEstado(List.of(tutor, aluno)).jaPediuExecutar());
    }

    private static String resposta(String avaliacao, String mensagem) {
        return "{\"diagnostico\":\"d\",\"avaliacao\":\"" + avaliacao + "\",\"nivel\":1,"
                + "\"aluno_descreveu_correcao\":false,\"mensagem\":\"" + mensagem + "\"}";
    }

    private final PromptV2Socratico prompt = new PromptV2Socratico(new com.fasterxml.jackson.databind.ObjectMapper());

    @Test
    void recusaAvaliacaoForaDaLista() {
        assertTrue(prompt.valido(resposta("PEDIU_SOLUCAO", "Olhe a linha 5. O que acontece ali?")));
        assertFalse(prompt.valido(resposta("PEDIDO_SOLUCAO", "Olhe a linha 5. O que acontece ali?")));
    }

    @Test
    void recusaFalaRepetidaOuSemAcentos() {
        String anterior = "Imagine duas pessoas escrevendo no mesmo quadro ao mesmo tempo. O que acontece com o contador?";
        assertFalse(prompt.aceitavel(resposta("SEM_RESPOSTA", anterior), List.of(anterior)), "fala igual");
        assertTrue(prompt.aceitavel(resposta("SEM_RESPOSTA", "Pense em dois caixas de banco atualizando o mesmo saldo. O que muda?"),
                List.of(anterior)), "fala diferente");
        assertFalse(prompt.aceitavel(resposta("SEM_RESPOSTA", "Como a variavel e alterada no seu codigo?"), List.of()),
                "fala sem acentos");
        assertTrue(prompt.aceitavel(resposta("SEM_RESPOSTA", "Como `codigo` é alterado aqui?"), List.of()),
                "nome entre crases não conta");
    }
}
