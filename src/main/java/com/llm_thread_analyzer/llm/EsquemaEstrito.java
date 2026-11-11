package com.llm_thread_analyzer.llm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Esquema JSON no formato aceito pelas APIs com saída estruturada estrita (Claude structured outputs,
 * json_schema strict na Groq/OpenAI): tira minimum/maximum, que essas APIs não aceitam, e põe
 * additionalProperties=false em todo objeto, que elas exigem. A faixa numérica continua conferida
 * pelo tutor depois (o nível é ajustado ao intervalo permitido).
 */
public final class EsquemaEstrito {

    private EsquemaEstrito() {}

    public static Object adaptar(Object no) {
        if (no instanceof Map<?, ?> mapa) {
            Map<String, Object> copia = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : mapa.entrySet()) {
                String chave = String.valueOf(e.getKey());
                if (chave.equals("minimum") || chave.equals("maximum")) continue;
                copia.put(chave, adaptar(e.getValue()));
            }
            if ("object".equals(copia.get("type"))) copia.put("additionalProperties", false);
            return copia;
        }
        if (no instanceof List<?> lista) {
            List<Object> copia = new ArrayList<>();
            for (Object item : lista) copia.add(adaptar(item));
            return copia;
        }
        return no;
    }
}
