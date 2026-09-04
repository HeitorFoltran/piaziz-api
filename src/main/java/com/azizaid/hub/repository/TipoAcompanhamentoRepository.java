package com.azizaid.hub.repository;

import com.azizaid.hub.model.TipoAcompanhamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoAcompanhamentoRepository extends JpaRepository<TipoAcompanhamento, Long> {
    boolean existsByNomeIgnoreCase(String nome);
}
