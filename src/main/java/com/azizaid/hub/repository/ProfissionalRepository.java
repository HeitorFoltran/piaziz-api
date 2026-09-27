package com.azizaid.hub.repository;

import com.azizaid.hub.model.Profissional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {

    Optional<Profissional> findByEmail(String email);

    Optional<Profissional> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByCpfIn(Collection<String> cpfs);

    @Query("""
            SELECT p FROM Profissional p
            WHERE (:termo IS NULL OR :termo = ''
                   OR LOWER(p.nome) LIKE LOWER(CONCAT('%', :termo, '%'))
                   OR p.username LIKE LOWER(CONCAT('%', :termo, '%'))
                   OR p.email LIKE LOWER(CONCAT('%', :termo, '%')))
              AND (:servicoId IS NULL OR p.servico.id = :servicoId)
            ORDER BY p.nome ASC
            """)
    List<Profissional> buscar(@Param("termo") String termo, @Param("servicoId") Long servicoId);
}
