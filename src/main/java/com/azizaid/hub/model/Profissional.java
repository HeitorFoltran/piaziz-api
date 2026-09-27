package com.azizaid.hub.model;

import com.azizaid.hub.model.enums.PapelProfissional;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "profissional")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profissional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 14)
    private String cpf;

    @Column(name = "carteira_profissional", length = 40)
    private String carteiraProfissional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servico_id")
    private Servico servico;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(unique = true, length = 150)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PapelProfissional role = PapelProfissional.PADRAO;

    @Column(nullable = false)
    @Builder.Default
    private boolean ativo = true;

    @Column(name = "pode_gerenciar_profissionais", nullable = false)
    private boolean podeGerenciarProfissionais;

    @Column(name = "deve_trocar_senha", nullable = false)
    private boolean deveTrocarSenha;

    @Column(name = "sessoes_revogadas_em")
    private Instant sessoesRevogadasEm;
}
