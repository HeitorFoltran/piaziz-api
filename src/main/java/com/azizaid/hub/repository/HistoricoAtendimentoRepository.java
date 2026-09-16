package com.azizaid.hub.repository;

import com.azizaid.hub.model.HistoricoAtendimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HistoricoAtendimentoRepository extends JpaRepository<HistoricoAtendimento, Long> {

    Optional<HistoricoAtendimento> findByFichaId(Long fichaId);

    @Query(value = "SELECT date_trunc(:unidade, data_criacao) AS periodo, COUNT(*) AS total "
            + "FROM historico_atendimento WHERE data_criacao IS NOT NULL GROUP BY periodo ORDER BY periodo",
            nativeQuery = true)
    List<Object[]> progressaoAtendimentos(@Param("unidade") String unidade);

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (ha.data_criacao - f.data_criacao)) / 86400.0) "
            + "FROM historico_atendimento ha JOIN ficha f ON f.id = ha.ficha_id "
            + "WHERE ha.data_criacao IS NOT NULL", nativeQuery = true)
    Double calcularTempoMedioCadastroAtendimentoDias();
}
