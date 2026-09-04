package com.azizaid.hub.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "entity_audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_entidade", nullable = false, length = 60)
    private String tipoEntidade;

    @Column(name = "entidade_id", nullable = false)
    private Long entidadeId;

    @Column(name = "editor_id", nullable = false)
    private Long editorId;

    @Column(name = "dono_id", nullable = false)
    private Long donoId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(length = 300)
    private String resumo;

    @PrePersist
    public void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
