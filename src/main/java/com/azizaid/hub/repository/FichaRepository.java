package com.azizaid.hub.repository;

import com.azizaid.hub.model.Ficha;
import com.azizaid.hub.model.enums.StatusFicha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FichaRepository extends JpaRepository<Ficha, Long> {

    long countByStatus(StatusFicha status);

    long countByStatusAndDataAtualizacaoBefore(StatusFicha status, LocalDateTime limite);

    long countByDataCriacaoBetween(LocalDateTime inicio, LocalDateTime fim);

    boolean existsByCpf(String cpf);

    @Query("""
            SELECT f FROM Ficha f
            WHERE :termo IS NULL OR :termo = ''
               OR LOWER(f.nome) LIKE LOWER(CONCAT('%', :termo, '%'))
               OR f.cpf LIKE CONCAT('%', :termo, '%')
               OR LOWER(f.codigoFicha) LIKE LOWER(CONCAT('%', :termo, '%'))
               OR f.numeroCaso LIKE CONCAT('%', :termo, '%')
            ORDER BY f.dataAtualizacao DESC, f.id DESC
            """)
    List<Ficha> buscar(@Param("termo") String termo);

    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(codigo_ficha FROM 3) AS INTEGER)), 0) "
            + "FROM ficha WHERE codigo_ficha LIKE 'F-%'", nativeQuery = true)
    int buscarMaiorSequencialCodigo();
}
