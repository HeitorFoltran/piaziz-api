package com.azizaid.hub.model;

import com.azizaid.hub.model.enums.NecessidadeImediata;
import com.azizaid.hub.model.enums.OndeMoramFilhos;
import com.azizaid.hub.model.enums.StatusFicha;
import com.azizaid.hub.model.enums.SupervisaoFilhos;
import com.azizaid.hub.model.enums.TipoMoradia;
import com.azizaid.hub.model.enums.VagaNecessaria;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "ficha")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ficha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_ficha", nullable = false, unique = true, length = 20)
    private String codigoFicha;

    @Column(name = "numero_caso", nullable = false, length = 30)
    private String numeroCaso;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 14)
    private String cpf;

    private Integer idade;

    @Column(length = 20)
    private String telefone;

    @Column(name = "estado_civil", length = 40)
    private String estadoCivil;

    @Column(name = "pessoas_dependentes")
    private Integer pessoasDependentes;

    @Column(name = "idade_filhos", length = 120)
    private String idadeFilhos;

    @Column(name = "nivel_seguranca")
    private Integer nivelSeguranca;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_moradia", length = 20)
    private TipoMoradia tipoMoradia;

    @Column(name = "tipo_moradia_outra_descricao", length = 150)
    private String tipoMoradiaOutraDescricao;

    @Column(name = "qtd_moradores")
    private Integer qtdMoradores;

    @Column(name = "qtd_filhos")
    private Integer qtdFilhos;

    @Enumerated(EnumType.STRING)
    @Column(name = "onde_moram_filhos", length = 30)
    private OndeMoramFilhos ondeMoramFilhos;

    @Enumerated(EnumType.STRING)
    @Column(name = "supervisao_filhos", length = 20)
    private SupervisaoFilhos supervisaoFilhos;

    @ElementCollection(targetClass = VagaNecessaria.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "ficha_vaga_necessaria", joinColumns = @JoinColumn(name = "ficha_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "vaga_necessaria", length = 30)
    @Builder.Default
    private Set<VagaNecessaria> vagasNecessarias = new HashSet<>();

    @ElementCollection(targetClass = NecessidadeImediata.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "ficha_necessidade_imediata", joinColumns = @JoinColumn(name = "ficha_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "necessidade_imediata", length = 40)
    @Builder.Default
    private Set<NecessidadeImediata> necessidadesImediatas = new HashSet<>();

    @Column(name = "necessidade_outra_descricao", length = 200)
    private String necessidadeOutraDescricao;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao", nullable = false)
    private LocalDateTime dataAtualizacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusFicha status;

    @CreatedBy
    @Column(name = "criado_por_id", updatable = false)
    private Long criadoPorId;

    @LastModifiedBy
    @Column(name = "ultimo_editor_id")
    private Long ultimoEditorId;

    @OneToMany(mappedBy = "ficha", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dataEncaminhamento DESC, id DESC")
    @Builder.Default
    private List<Encaminhamento> encaminhamentos = new ArrayList<>();

    @OneToMany(mappedBy = "ficha", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dataInteracao DESC, id DESC")
    @Builder.Default
    private List<Interacao> interacoes = new ArrayList<>();

    @OneToOne(mappedBy = "ficha", cascade = CascadeType.ALL, orphanRemoval = true)
    private AvaliacaoSocioeconomica avaliacaoSocioeconomica;

    @OneToOne(mappedBy = "ficha", cascade = CascadeType.ALL, orphanRemoval = true)
    private HistoricoAtendimento historicoAtendimento;

    @OneToOne(mappedBy = "ficha", cascade = CascadeType.ALL, orphanRemoval = true)
    private AcolhimentoEquipe acolhimentoEquipe;

    @PrePersist
    public void prePersist() {
        LocalDateTime agora = LocalDateTime.now();
        if (dataCriacao == null) {
            dataCriacao = agora;
        }
        if (dataAtualizacao == null) {
            dataAtualizacao = agora;
        }
        if (status == null) {
            status = StatusFicha.ATIVO;
        }
    }

    @PreUpdate
    public void preUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }
}
