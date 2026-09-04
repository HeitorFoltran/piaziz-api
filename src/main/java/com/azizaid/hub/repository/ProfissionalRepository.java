package com.azizaid.hub.repository;

import com.azizaid.hub.model.Profissional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {

    Optional<Profissional> findByEmail(String email);

    @Query("""
            SELECT p FROM Profissional p
            WHERE (:termo IS NULL OR :termo = ''
                   OR LOWER(p.nome) LIKE LOWER(CONCAT('%', :termo, '%'))
                   OR p.cpf LIKE CONCAT('%', :termo, '%')
                   OR LOWER(p.carteiraProfissional) LIKE LOWER(CONCAT('%', :termo, '%')))
              AND (:servicoId IS NULL OR p.servico.id = :servicoId)
            ORDER BY p.nome ASC
            """)
    List<Profissional> buscar(@Param("termo") String termo, @Param("servicoId") Long servicoId);
}
