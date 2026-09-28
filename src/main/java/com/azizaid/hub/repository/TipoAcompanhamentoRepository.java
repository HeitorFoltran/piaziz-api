package com.azizaid.hub.repository;

import com.azizaid.hub.model.TipoAcompanhamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TipoAcompanhamentoRepository extends JpaRepository<TipoAcompanhamento, Long> {
    boolean existsByNomeIgnoreCase(String nome);

    // A FK de ficha_tipo_acompanhamento é ON DELETE CASCADE: sem a trava, um tipo atribuído a um
    // caso entre a checagem de uso e o DELETE sumiria do caso sem aviso. Com ela, a atribuição
    // espera a exclusão terminar e falha na FK.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TipoAcompanhamento t WHERE t.id = :id")
    Optional<TipoAcompanhamento> buscarParaExclusao(@Param("id") Long id);

    @Query(value = "SELECT COUNT(*) FROM ficha_tipo_acompanhamento WHERE tipo_acompanhamento_id = :id",
            nativeQuery = true)
    long contarFichas(@Param("id") Long id);

    @Query(value = "SELECT DISTINCT tipo_acompanhamento_id FROM ficha_tipo_acompanhamento", nativeQuery = true)
    List<Long> idsEmUso();
}
