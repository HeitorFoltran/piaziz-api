package com.azizaid.hub.repository;

import com.azizaid.hub.model.Ficha;
import com.azizaid.hub.model.enums.StatusFicha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FichaRepository extends JpaRepository<Ficha, Long> {

    long countByStatus(StatusFicha status);

    long countByStatusAndDataAtualizacaoBefore(StatusFicha status, LocalDateTime limite);

    long countByDataCriacaoBetween(LocalDateTime inicio, LocalDateTime fim);

    boolean existsByCpf(String cpf);

    @Query("SELECT COUNT(DISTINCT f.id) FROM Ficha f JOIN f.encaminhamentos e")
    long contarFichasComEncaminhamento();

    @Query("SELECT COUNT(DISTINCT f.id) FROM Ficha f JOIN f.encaminhamentos e WHERE f.historicoAtendimento IS NOT NULL")
    long contarFichasComEncaminhamentoEAtendimento();

    @Query(value = "SELECT MIN(data_criacao) FROM ficha", nativeQuery = true)
    Optional<LocalDateTime> findMinDataCriacao();

    @Query(value = "SELECT date_trunc(:unidade, data_criacao) AS periodo, COUNT(*) AS total "
            + "FROM ficha GROUP BY periodo ORDER BY periodo", nativeQuery = true)
    List<Object[]> progressaoCadastros(@Param("unidade") String unidade);

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
