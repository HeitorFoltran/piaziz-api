package com.azizaid.hub.model;

import com.azizaid.hub.model.enums.StatusFichaPendente;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ficha_pendente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FichaPendente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "convite_id", nullable = false)
    private Long conviteId;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 14)
    private String cpf;

    @Column(length = 20)
    private String telefone;

    private Integer idade;

    @Column(name = "situacao_relatada", columnDefinition = "TEXT")
    private String situacaoRelatada;

    @Column(name = "data_submissao", nullable = false)
    private LocalDateTime dataSubmissao;

    @Column(name = "ip_submissao", length = 45)
    private String ipSubmissao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusFichaPendente status;

    @Column(name = "revisado_por_id")
    private Long revisadoPorId;

    @Column(name = "data_revisao")
    private LocalDateTime dataRevisao;

    @Column(name = "motivo_rejeicao", length = 300)
    private String motivoRejeicao;

    @Column(name = "ficha_id")
    private Long fichaId;

    @PrePersist
    public void prePersist() {
        if (dataSubmissao == null) {
            dataSubmissao = LocalDateTime.now();
        }
        if (status == null) {
            status = StatusFichaPendente.PENDENTE;
        }
    }
}
