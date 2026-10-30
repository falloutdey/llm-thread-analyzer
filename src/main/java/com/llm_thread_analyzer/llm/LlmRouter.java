package com.llm_thread_analyzer.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llm_thread_analyzer.llm.LlmTipos.LlmException;
import com.llm_thread_analyzer.llm.LlmTipos.MotivoFalha;
import com.llm_thread_analyzer.llm.LlmTipos.Requisicao;
import com.llm_thread_analyzer.llm.LlmTipos.Resposta;
import com.llm_thread_analyzer.llm.LlmTipos.Resultado;
import com.llm_thread_analyzer.llm.LlmTipos.Tentativa;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Escolhe o modelo e aplica as regras de tempo.
 *
 * Ordem do que acontece em cada pedido:
 *   1. Pega o primeiro modelo da cadeia (llm.cadeia).
 *   2. Reserva um horário respeitando o intervalo mínimo daquele modelo; se a espera
 *      estourar o prazo total, pula o modelo (PRAZO_ESGOTADO).
 *   3. Chama o modelo com timeout = menor entre timeout-chamada-ms e o tempo que resta.
 *   4. Falha temporária (503, timeout, rede): tenta de novo no mesmo modelo, se configurado.
 *   5. Qualquer outra falha, ou tentativas esgotadas: passa ao próximo modelo (se o fallback estiver ligado).
 *   6. Cada tentativa vira um registro (Tentativa), com ou sem sucesso.
 */
@Service
public class LlmRouter {

    private final LlmProperties props;
    private final List<LlmProvider> cadeia;
    /** Próximo instante (System.nanoTime) em que cada modelo pode receber chamada. */
    private final Map<String, Long> proximoHorarioLivre = new ConcurrentHashMap<>();

    public LlmRouter(LlmProperties props, ObjectMapper json) {
        this.props = props;
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(props.getTimeoutConexaoMs()))
                .build();

        List<LlmProvider> montados = new ArrayList<>();
        for (String nome : props.getCadeia()) {
            LlmProperties.Modelo cfg = props.getModelos().get(nome);
            if (cfg == null) {
                System.err.println("[LLM] Modelo '" + nome + "' está em llm.cadeia mas não tem configuração. Ignorado.");
                continue;
            }
            if (cfg.getChave() == null || cfg.getChave().isBlank()) {
                System.err.println("[LLM] Modelo '" + nome + "' sem chave de API. Ignorado.");
                continue;
            }
            LlmProvider p = switch (cfg.getTipo() == null ? "gemini" : cfg.getTipo().toLowerCase()) {
                case "gemini" -> new GeminiProvider(nome, cfg, http, json);
                case "openai" -> new OpenAiCompatProvider(nome, cfg, http, json);
                default -> null;
            };
            if (p == null) {
                System.err.println("[LLM] Tipo desconhecido '" + cfg.getTipo() + "' no modelo '" + nome + "'. Ignorado.");
                continue;
            }
            montados.add(p);
        }
        this.cadeia = Collections.unmodifiableList(montados);
        System.out.println("[LLM] Cadeia ativa: " + cadeia.stream().map(p -> p.nomeConfig() + "(" + p.modelo() + ")").toList()
                + " | fallback=" + props.isFallbackHabilitado());
    }

    /** Modelos ativos, na ordem de tentativa. */
    public List<String> modelosAtivos() {
        return cadeia.stream().map(p -> p.nomeConfig() + ":" + p.modelo()).toList();
    }

    /**
     * Gera uma resposta. Nunca lança exceção por falha do modelo: se nada der certo,
     * devolve Resultado sem resposta e com todas as tentativas registradas.
     *
     * @param permitirFallback false = usa só o primeiro modelo (mesmo com fallback ligado na configuração)
     */
    public Resultado gerar(Requisicao requisicao, boolean permitirFallback) {
        long inicio = System.nanoTime();
        long prazoFinal = inicio + props.getPrazoTotalMs() * 1_000_000L;
        List<Tentativa> tentativas = new ArrayList<>();

        if (cadeia.isEmpty()) {
            tentativas.add(new Tentativa(1, "-", "-", "-", false, MotivoFalha.ERRO_CLIENTE,
                    "Nenhum modelo configurado com chave (veja llm.cadeia e as variáveis de ambiente).", 0, null, null));
            return new Resultado(null, tentativas, 0);
        }

        boolean usarFallback = permitirFallback && props.isFallbackHabilitado();
        List<LlmProvider> candidatos = usarFallback ? cadeia : cadeia.subList(0, 1);
        int ordem = 0;

        for (LlmProvider provider : candidatos) {
            int maxTentativas = Math.max(1, props.getTentativasPorModelo());
            for (int t = 1; t <= maxTentativas; t++) {
                ordem++;
                long restanteMs = (prazoFinal - System.nanoTime()) / 1_000_000L;
                if (restanteMs <= 0 || !aguardarVez(provider, restanteMs)) {
                    tentativas.add(new Tentativa(ordem, provider.nomeConfig(), provider.provedor(), provider.modelo(),
                            false, MotivoFalha.PRAZO_ESGOTADO, "Sem tempo restante para este modelo", 0, null, null));
                    break;
                }
                restanteMs = (prazoFinal - System.nanoTime()) / 1_000_000L;
                long timeout = Math.min(props.getTimeoutChamadaMs(), Math.max(restanteMs, 1));

                long t0 = System.nanoTime();
                try {
                    Resposta r = provider.gerar(requisicao, timeout);
                    if (requisicao.validador() != null && !requisicao.validador().test(r.texto())) {
                        throw new LlmException(MotivoFalha.RESPOSTA_INVALIDA,
                                "Resposta fora do formato esperado: " + resumir(r.texto()));
                    }
                    long lat = (System.nanoTime() - t0) / 1_000_000L;
                    tentativas.add(new Tentativa(ordem, provider.nomeConfig(), r.provedor(), r.modelo(),
                            true, null, null, lat, r.tokensEntrada(), r.tokensSaida()));
                    System.out.println("[LLM] OK " + provider.nomeConfig() + " em " + lat + " ms");
                    return new Resultado(r, tentativas, (System.nanoTime() - inicio) / 1_000_000L);
                } catch (LlmException e) {
                    long lat = (System.nanoTime() - t0) / 1_000_000L;
                    tentativas.add(new Tentativa(ordem, provider.nomeConfig(), provider.provedor(), provider.modelo(),
                            false, e.getMotivo(), resumir(e.getMessage()), lat, null, null));
                    System.err.println("[LLM] Falha " + provider.nomeConfig() + " (" + e.getMotivo() + "): " + resumir(e.getMessage()));
                    if (!e.temporaria()) break; // falha definitiva neste modelo: vai para o próximo
                } catch (RuntimeException e) {
                    long lat = (System.nanoTime() - t0) / 1_000_000L;
                    tentativas.add(new Tentativa(ordem, provider.nomeConfig(), provider.provedor(), provider.modelo(),
                            false, MotivoFalha.DESCONHECIDO, resumir(String.valueOf(e)), lat, null, null));
                    System.err.println("[LLM] Erro inesperado em " + provider.nomeConfig() + ": " + e);
                    break;
                }
            }
        }
        return new Resultado(null, tentativas, (System.nanoTime() - inicio) / 1_000_000L);
    }

    /**
     * Respeita o intervalo mínimo entre chamadas ao mesmo modelo.
     * Reserva o horário sem segurar trava durante a espera, então vários alunos entram numa fila justa.
     *
     * @return false se a espera passaria do tempo restante (o horário não é reservado)
     */
    private boolean aguardarVez(LlmProvider provider, long restanteMs) {
        long intervaloNs = provider.config().getIntervaloMinimoMs() * 1_000_000L;
        if (intervaloNs <= 0) return true;

        long horario;
        synchronized (proximoHorarioLivre) {
            long agora = System.nanoTime();
            long livre = proximoHorarioLivre.getOrDefault(provider.nomeConfig(), agora);
            horario = Math.max(agora, livre);
            if ((horario - agora) / 1_000_000L >= restanteMs) {
                return false;
            }
            proximoHorarioLivre.put(provider.nomeConfig(), horario + intervaloNs);
        }

        long esperaNs = horario - System.nanoTime();
        if (esperaNs > 0) {
            try {
                Thread.sleep(esperaNs / 1_000_000L, (int) (esperaNs % 1_000_000L));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }

    private static String resumir(String s) {
        if (s == null) return null;
        return s.length() <= 500 ? s : s.substring(0, 500) + "...";
    }
}
