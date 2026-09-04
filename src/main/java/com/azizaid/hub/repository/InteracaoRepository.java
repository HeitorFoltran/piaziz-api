package com.azizaid.hub.repository;

import com.azizaid.hub.model.Interacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InteracaoRepository extends JpaRepository<Interacao, Long> {

    List<Interacao> findByFichaIdOrderByDataInteracaoDescIdDesc(Long fichaId);
}
