package com.azizaid.hub.model;

import com.azizaid.hub.model.enums.CategoriaClassificacao;
import com.azizaid.hub.model.enums.FrequenciaViolencia;
import com.azizaid.hub.model.enums.TipoViolencia;
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
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "acolhimento_equipe")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcolhimentoEquipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ficha_id", nullable = false, unique = true)
    private Ficha ficha;

    @Column(name = "numero_processo_mpu", length = 40)
    private String numeroProcessoMpu;

    @Column(name = "data_reuniao_acolhimento")
    private LocalDate dataReuniaoAcolhimento;

    @Column(name = "servidor_responsavel", length = 150)
    private String servidorResponsavel;

    @ElementCollection(targetClass = TipoViolencia.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "acolhimento_tipo_violencia", joinColumns = @JoinColumn(name = "acolhimento_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_violencia", length = 20)
    @Builder.Default
    private Set<TipoViolencia> tiposViolencia = new HashSet<>();

    @Column(name = "tipo_violencia_outra_descricao", length = 200)
    private String tipoViolenciaOutraDescricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequencia_violencia", length = 20)
    private FrequenciaViolencia frequenciaViolencia;

    @Column(name = "medidas_protetivas_anteriores")
    private Boolean medidasProtetivasAnteriores;

    @Column(name = "ameacas_relatadas", length = 1000)
    private String ameacasRelatadas;

    @Column(name = "necessidade_atendimento_medico_imediato")
    private Boolean necessidadeAtendimentoMedicoImediato;

    @Column(name = "acompanhamento_saude_mental_em_curso")
    private Boolean acompanhamentoSaudeMentalEmCurso;

    @Column(name = "acompanhamento_saude_mental_local", length = 150)
    private String acompanhamentoSaudeMentalLocal;

    @Column(name = "dependente_sofreu_violencia")
    private Boolean dependenteSofreuViolencia;

    @Column(name = "dependente_precisa_auxilio_medico")
    private Boolean dependentePrecisaAuxilioMedico;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria_classificacao", length = 15)
    private CategoriaClassificacao categoriaClassificacao;

    @Column(name = "observacoes_relevantes", length = 2000)
    private String observacoesRelevantes;

    @Column(name = "responsavel_acolhimento_juridico", length = 150)
    private String responsavelAcolhimentoJuridico;

    @CreatedBy
    @Column(name = "criado_por_id", updatable = false)
    private Long criadoPorId;

    @LastModifiedBy
    @Column(name = "ultimo_editor_id")
    private Long ultimoEditorId;
}
