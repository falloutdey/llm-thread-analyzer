package com.llm_thread_analyzer.tutor.entity;

import com.llm_thread_analyzer.tutor.entity.TutorEnums.Autor;
import com.llm_thread_analyzer.tutor.entity.TutorEnums.EstadoConversa;
import com.llm_thread_analyzer.tutor.entity.TutorEnums.Modo;
import com.llm_thread_analyzer.tutor.entity.TutorEnums.TipoMensagem;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Grava os enums do tutor como texto comum (VARCHAR), pelo nome.
 *
 * Com @Enumerated(STRING), o Hibernate 6 cria no H2 uma coluna do tipo ENUM com a lista de valores
 * da época, e o ddl-auto=update não atualiza essa lista: um valor novo (como TipoMensagem.REFLEXAO,
 * de 04/11/2026) passa a ser recusado pelo banco. Com o conversor, a coluna é texto e aceita valores novos.
 * As colunas antigas são convertidas na inicialização por config.AjusteColunasEnum.
 */
public final class ConversoresEnum {

    private ConversoresEnum() {}

    private abstract static class PorNome<E extends Enum<E>> implements AttributeConverter<E, String> {
        private final Class<E> tipo;

        PorNome(Class<E> tipo) {
            this.tipo = tipo;
        }

        @Override
        public String convertToDatabaseColumn(E valor) {
            return valor == null ? null : valor.name();
        }

        @Override
        public E convertToEntityAttribute(String texto) {
            return texto == null || texto.isBlank() ? null : Enum.valueOf(tipo, texto.trim());
        }
    }

    @Converter
    public static class ModoTexto extends PorNome<Modo> {
        public ModoTexto() { super(Modo.class); }
    }

    @Converter
    public static class AutorTexto extends PorNome<Autor> {
        public AutorTexto() { super(Autor.class); }
    }

    @Converter
    public static class TipoMensagemTexto extends PorNome<TipoMensagem> {
        public TipoMensagemTexto() { super(TipoMensagem.class); }
    }

    @Converter
    public static class EstadoConversaTexto extends PorNome<EstadoConversa> {
        public EstadoConversaTexto() { super(EstadoConversa.class); }
    }
}
