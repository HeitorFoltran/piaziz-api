package com.azizaid.hub.repository;

import com.azizaid.hub.model.Servico;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

    boolean existsByNomeIgnoreCase(String nome);

    // Trava a linha até o fim da transação: um encaminhamento criado ao mesmo tempo espera a
    // exclusão terminar e falha na FK, em vez de passar entre a checagem de uso e o DELETE.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Servico s WHERE s.id = :id")
    Optional<Servico> buscarParaExclusao(@Param("id") Long id);

    @Query(value = "SELECT COUNT(*) FROM encaminhamento WHERE servico_id = :id", nativeQuery = true)
    long contarEncaminhamentos(@Param("id") Long id);

    @Query(value = "SELECT COUNT(*) FROM profissional WHERE servico_id = :id", nativeQuery = true)
    long contarProfissionais(@Param("id") Long id);

    @Query(value = """
            SELECT s.id FROM servico s
            WHERE EXISTS (SELECT 1 FROM encaminhamento e WHERE e.servico_id = s.id)
               OR EXISTS (SELECT 1 FROM profissional p WHERE p.servico_id = s.id)
            """, nativeQuery = true)
    List<Long> idsEmUso();
}
