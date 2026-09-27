package com.azizaid.hub.repository;

import com.azizaid.hub.model.Encaminhamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EncaminhamentoRepository extends JpaRepository<Encaminhamento, Long> {

    List<Encaminhamento> findByFichaIdOrderByDataEncaminhamentoDescIdDesc(Long fichaId);

    Optional<Encaminhamento> findTopByFichaIdOrderByDataEncaminhamentoDescIdDesc(Long fichaId);

    @Query("""
            SELECT COUNT(DISTINCT e.ficha.id) FROM Encaminhamento e
            WHERE e.dataRetorno IS NULL OR e.dataRetorno >= :hoje
            """)
    long contarFichasAguardandoRetorno(@Param("hoje") LocalDate hoje);

    @Query("""
            SELECT e.servico.id, e.servico.nome, COUNT(e)
            FROM Encaminhamento e
            GROUP BY e.servico.id, e.servico.nome
            ORDER BY COUNT(e) DESC
            """)
    List<Object[]> contarPorServico();

    @Query(value = "SELECT MIN(data_encaminhamento) FROM encaminhamento WHERE data_encaminhamento IS NOT NULL",
            nativeQuery = true)
    Optional<LocalDate> findMinDataEncaminhamento();

    @Query(value = "SELECT date_trunc(:unidade, data_encaminhamento) AS periodo, COUNT(*) AS total "
            + "FROM encaminhamento WHERE data_encaminhamento IS NOT NULL GROUP BY periodo ORDER BY periodo",
            nativeQuery = true)
    List<Object[]> progressaoEncaminhamentos(@Param("unidade") String unidade);
}