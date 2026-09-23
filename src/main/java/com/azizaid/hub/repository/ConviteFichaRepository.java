package com.azizaid.hub.repository;

import com.azizaid.hub.model.ConviteFicha;
import com.azizaid.hub.model.enums.StatusConvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ConviteFichaRepository extends JpaRepository<ConviteFicha, Long> {

    Optional<ConviteFicha> findByTokenHash(String tokenHash);

    List<ConviteFicha> findByCriadoPorIdOrderByDataCriacaoDesc(Long criadoPorId);

    @Modifying
    @Query("""
            UPDATE ConviteFicha c SET c.status = :statusUsado, c.usadoEm = :agora
            WHERE c.id = :id AND c.status = :statusAtivo
            """)
    int marcarComoUsadoSeAtivo(@Param("id") Long id,
                                @Param("statusUsado") StatusConvite statusUsado,
                                @Param("statusAtivo") StatusConvite statusAtivo,
                                @Param("agora") LocalDateTime agora);
}
