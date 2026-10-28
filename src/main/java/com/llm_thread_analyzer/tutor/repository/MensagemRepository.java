package com.llm_thread_analyzer.tutor.repository;

import com.llm_thread_analyzer.tutor.entity.Mensagem;
import com.llm_thread_analyzer.tutor.entity.TutorEnums.Autor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MensagemRepository extends JpaRepository<Mensagem, Long> {

    List<Mensagem> findByConversaIdOrderByOrdemAsc(Long conversaId);

    int countByConversaId(Long conversaId);

    int countByConversaIdAndAutor(Long conversaId, Autor autor);
}
