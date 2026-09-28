package com.azizaid.hub.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Quem abriu o detalhe de um caso (GET /api/fichas/{id}). Sem FK: o log não trava a exclusão de
// nada, nem some junto.
@Entity
@Table(name = "leitura_audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeituraAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ficha_id", nullable = false)
    private Long fichaId;

    @Column(name = "profissional_id", nullable = false)
    private Long profissionalId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    public void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
