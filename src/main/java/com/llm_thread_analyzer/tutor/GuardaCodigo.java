package com.llm_thread_analyzer.tutor;

import java.util.regex.Pattern;

/**
 * Verificação automática do guarda-corpo "não entregar código pronto".
 * Por enquanto só MARCA a resposta (Mensagem.possivelCodigo) para análise posterior; não bloqueia.
 * Nomes de conceitos entre crases (`synchronized`, `join()`) não contam.
 */
public final class GuardaCodigo {

    private GuardaCodigo() {}

    private static final Pattern BLOCO_CERCADO = Pattern.compile("```");

    /** Linha que termina como instrução Java: ; { } */
    private static final Pattern LINHA_JAVA = Pattern.compile("(?m)^\\s*([^\\s#*\\-].*)?[;{}]\\s*$");

    public static boolean pareceCodigo(String texto) {
        if (texto == null || texto.isBlank()) return false;
        if (BLOCO_CERCADO.matcher(texto).find()) return true;

        int linhas = 0;
        var m = LINHA_JAVA.matcher(texto);
        while (m.find()) {
            linhas++;
            if (linhas >= 3) return true;
        }
        return false;
    }
}
