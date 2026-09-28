package com.azizaid.hub.model;

import com.azizaid.hub.model.enums.StatusConvite;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "convite_ficha")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConviteFicha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    // Só para mostrar o link de novo na lista; a validação é pelo token_hash. Vira NULL quando o
    // convite deixa de valer (usado, cancelado, expirado).
    @Column(name = "token_cifrado")
    private String tokenCifrado;

    @Column(name = "criado_por_id", nullable = false)
    private Long criadoPorId;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_expiracao", nullable = false)
    private LocalDateTime dataExpiracao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusConvite status;

    @Column(name = "usado_em")
    private LocalDateTime usadoEm;

    @PrePersist
    public void prePersist() {
        if (dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }
        if (status == null) {
            status = StatusConvite.ATIVO;
        }
    }
}
