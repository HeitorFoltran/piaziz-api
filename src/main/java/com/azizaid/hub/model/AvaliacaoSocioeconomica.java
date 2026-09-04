package com.azizaid.hub.model;

import com.azizaid.hub.model.enums.NivelEscolaridade;
import com.azizaid.hub.model.enums.NivelEscrita;
import com.azizaid.hub.model.enums.PeriodoTrabalho;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;

@Entity
@Table(name = "avaliacao_socioeconomica")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvaliacaoSocioeconomica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ficha_id", nullable = false, unique = true)
    private Ficha ficha;

    @Column(name = "tem_renda")
    private Boolean temRenda;

    @Column(name = "valor_renda", precision = 10, scale = 2)
    private BigDecimal valorRenda;

    @Column(name = "pessoas_dependem_renda")
    private Integer pessoasDependemRenda;

    @Column(name = "origem_renda", length = 200)
    private String origemRenda;

    @Column(name = "trabalho_formal")
    private Boolean trabalhoFormal;

    @Column(name = "renda_suficiente")
    private Boolean rendaSuficiente;

    @Column(name = "trabalhando_atualmente")
    private Boolean trabalhandoAtualmente;

    @Column(name = "onde_trabalha", length = 200)
    private String ondeTrabalha;

    @Column(name = "problema_saude_atrapalha_trabalho")
    private Boolean problemaSaudeAtrapalhaTrabalho;

    @Column(name = "problema_saude_qual", length = 200)
    private String problemaSaudeQual;

    @Column(name = "situacao_familiar_atrapalha_trabalho")
    private Boolean situacaoFamiliarAtrapalhaTrabalho;

    @Column(name = "situacao_familiar_qual", length = 200)
    private String situacaoFamiliarQual;

    @Column(name = "deseja_trabalhar")
    private Boolean desejaTrabalhar;

    @Enumerated(EnumType.STRING)
    @Column(name = "periodo_desejado", length = 20)
    private PeriodoTrabalho periodoDesejado;

    @Column(name = "sabe_ler")
    private Boolean sabeLer;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_escrita", length = 20)
    private NivelEscrita nivelEscrita;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_escolaridade", length = 20)
    private NivelEscolaridade nivelEscolaridade;

    @Column(name = "escolaridade_detalhe", length = 150)
    private String escolaridadeDetalhe;

    @Column(name = "fez_curso_profissionalizante")
    private Boolean fezCursoProfissionalizante;

    @Column(name = "curso_profissionalizante_qual", length = 200)
    private String cursoProfissionalizanteQual;

    @Column(name = "deseja_auxilio_ceebja")
    private Boolean desejaAuxilioCeebja;

    @Column(name = "deseja_curso_senai")
    private Boolean desejaCursoSenai;

    @Column(name = "area_curso_senai", length = 150)
    private String areaCursoSenai;

    @Column(name = "tem_rede_apoio")
    private Boolean temRedeApoio;

    @Column(name = "precisa_ajuda_moradia")
    private Boolean precisaAjudaMoradia;

    @Column(name = "tem_o_que_comer")
    private Boolean temOQueComer;

    @Column(name = "acompanhamento_medico")
    private Boolean acompanhamentoMedico;

    @Column(name = "precisa_ajuda_tratamento_medico")
    private Boolean precisaAjudaTratamentoMedico;

    @Column(name = "uso_continuo_medicamento")
    private Boolean usoContinuoMedicamento;

    @Column(name = "medicamento_quais", length = 300)
    private String medicamentoQuais;

    @Column(name = "acesso_medicamentos")
    private Boolean acessoMedicamentos;

    @CreatedBy
    @Column(name = "criado_por_id", updatable = false)
    private Long criadoPorId;

    @LastModifiedBy
    @Column(name = "ultimo_editor_id")
    private Long ultimoEditorId;
}
