package com.azizaid.hub.model;

import com.azizaid.hub.model.enums.TipoEncaminhamento;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;

@Entity
@Table(name = "encaminhamento")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Encaminhamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ficha_id", nullable = false)
    private Ficha ficha;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servico_id", nullable = false)
    private Servico servico;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", length = 30)
    private TipoEncaminhamento categoria;

    @Column(length = 150)
    private String profissional;

    @Column(name = "data_encaminhamento")
    private LocalDate dataEncaminhamento;

    @Column(name = "data_retorno")
    private LocalDate dataRetorno;

    @Column(length = 1000)
    private String descricao;

    @CreatedBy
    @Column(name = "criado_por_id", updatable = false)
    private Long criadoPorId;

    @LastModifiedBy
    @Column(name = "ultimo_editor_id")
    private Long ultimoEditorId;
}