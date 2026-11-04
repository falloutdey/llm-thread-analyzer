package com.llm_thread_analyzer.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Converte para texto (VARCHAR) as colunas do tipo ENUM que versões anteriores criaram no banco H2.
 *
 * Até 04/11/2026 os enums do tutor eram gravados com @Enumerated(STRING), e o Hibernate criava no H2
 * colunas ENUM com a lista fixa dos valores da época. Um valor novo (TipoMensagem.REFLEXAO) era recusado
 * ("Value not permitted for column"). Agora os enums usam conversores para texto (tutor.entity.ConversoresEnum),
 * e esta classe ajusta, uma vez, os bancos já existentes. Roda depois de o Hibernate atualizar o esquema.
 * Os dados não mudam: cada valor continua gravado pelo nome.
 */
@Component
@DependsOn("entityManagerFactory")
public class AjusteColunasEnum {

    private final DataSource dataSource;

    public AjusteColunasEnum(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    void converterColunasEnum() {
        try (Connection c = dataSource.getConnection()) {
            if (!c.getMetaData().getDatabaseProductName().toUpperCase().contains("H2")) return;
            List<String[]> colunas = new ArrayList<>();
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT TABLE_NAME, COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS "
                         + "WHERE TABLE_SCHEMA = 'PUBLIC' AND DATA_TYPE = 'ENUM'")) {
                while (rs.next()) colunas.add(new String[] { rs.getString(1), rs.getString(2) });
            }
            for (String[] col : colunas) {
                try (Statement st = c.createStatement()) {
                    st.execute("ALTER TABLE \"" + col[0] + "\" ALTER COLUMN \"" + col[1] + "\" SET DATA TYPE VARCHAR(30)");
                    System.out.println("[Banco] Coluna " + col[0] + "." + col[1] + " convertida de ENUM para VARCHAR(30).");
                }
            }
        } catch (Exception e) {
            System.err.println("[Banco] Não foi possível conferir as colunas ENUM: " + e.getMessage());
        }
    }
}
