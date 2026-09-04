package com.azizaid.hub.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "historico_atendimento")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricoAtendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ficha_id", nullable = false, unique = true)
    private Ficha ficha;

    @Column(name = "ja_procurou_servico")
    private Boolean jaProcurouServico;

    @Column(name = "servico_procurado_qual_onde", length = 300)
    private String servicoProcuradoQualOnde;

    @Column(name = "em_fila_espera")
    private Boolean emFilaEspera;

    @Column(name = "fila_espera_qual", length = 200)
    private String filaEsperaQual;

    @Column(name = "ja_pediu_ajuda_justica_policia")
    private Boolean jaPediuAjudaJusticaPolicia;

    @Column(name = "justica_policia_qual", length = 200)
    private String justicaPoliciaQual;

    @Column(name = "como_foi_atendimento", length = 1000)
    private String comoFoiAtendimento;

    @Column(name = "resolveu_situacao")
    private Boolean resolveuSituacao;

    @Column(name = "reacao_agressor", length = 500)
    private String reacaoAgressor;

    @CreatedBy
    @Column(name = "criado_por_id", updatable = false)
    private Long criadoPorId;

    @LastModifiedBy
    @Column(name = "ultimo_editor_id")
    private Long ultimoEditorId;
}
