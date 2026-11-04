package com.llm_thread_analyzer.tutor.repository;

import com.llm_thread_analyzer.tutor.entity.Conversa;
import com.llm_thread_analyzer.tutor.entity.TutorEnums.EstadoConversa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversaRepository extends JpaRepository<Conversa, Long> {

    List<Conversa> findByAlertaIdAndEstado(Long alertaId, EstadoConversa estado);
}
