package com.azizaid.hub.service;

import com.azizaid.hub.model.EntityAuditLog;
import com.azizaid.hub.model.Ficha;
import com.azizaid.hub.model.enums.AcaoAlteracao;
import com.azizaid.hub.repository.EntityAuditLogRepository;
import com.azizaid.hub.util.RetratoAuditoria;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EntityAuditServiceTest {

    @Mock
    EntityAuditLogRepository entityAuditLogRepository;

    EntityAuditService entityAuditService;

    @BeforeEach
    void setUp() {
        entityAuditService = new EntityAuditService(entityAuditLogRepository);
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(Long id) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                id, null, List.of(new SimpleGrantedAuthority("ROLE_PADRAO"))));
    }

    private EntityAuditLog linhaGravada() {
        ArgumentCaptor<EntityAuditLog> captor = ArgumentCaptor.forClass(EntityAuditLog.class);
        verify(entityAuditLogRepository).save(captor.capture());
        return captor.getValue();
    }

    private static Ficha fichaCriadaEm(LocalDateTime dataCriacao) {
        return Ficha.builder().id(1L).dataCriacao(dataCriacao).build();
    }

    @Test
    void registrar_pelaPropriaDona_grava() {
        autenticarComo(7L);

        entityAuditService.registrar("Ficha", 1L, 7L, 1L, AcaoAlteracao.EDITOU, null);

        EntityAuditLog linha = linhaGravada();
        assertThat(linha.getEditorId()).isEqualTo(7L);
        assertThat(linha.getDonoId()).isEqualTo(7L);
        assertThat(linha.getAcao()).isEqualTo(AcaoAlteracao.EDITOU);
        assertThat(linha.getResumo()).isEqualTo("profissional 7 EDITOU Ficha #1");
    }

    @Test
    void registrar_semDono_usaOEditorComoDono() {
        autenticarComo(7L);

        entityAuditService.registrar("StatusFicha", 1L, null, 1L, AcaoAlteracao.MUDOU_STATUS, "Ativo -> Arquivado");

        EntityAuditLog linha = linhaGravada();
        assertThat(linha.getDonoId()).isEqualTo(7L);
        assertThat(linha.getDetalhe()).isEqualTo("Ativo -> Arquivado");
    }

    @Test
    void registrar_semUsuarioNoContexto_naoGrava() {
        entityAuditService.registrar("Ficha", 1L, 7L, 1L, AcaoAlteracao.EDITOU, null);

        verify(entityAuditLogRepository, never()).save(any());
    }

    @Test
    void salvamentoDeParte_existenteSemMudanca_naoGrava() {
        autenticarComo(7L);

        entityAuditService.registrarSalvamentoDeParte("AvaliacaoSocioeconomica", 5L, 7L,
                fichaCriadaEm(LocalDateTime.now().minusDays(3)), false,
                RetratoAuditoria.de(true, "texto", List.of(1, 2)), RetratoAuditoria.de(true, "texto", List.of(2, 1)));

        verify(entityAuditLogRepository, never()).save(any());
    }

    @Test
    void salvamentoDeParte_existenteComMudanca_gravaEditou() {
        autenticarComo(7L);

        entityAuditService.registrarSalvamentoDeParte("AvaliacaoSocioeconomica", 5L, 7L,
                fichaCriadaEm(LocalDateTime.now().minusDays(3)), false,
                RetratoAuditoria.de(true), RetratoAuditoria.de(false));

        assertThat(linhaGravada().getAcao()).isEqualTo(AcaoAlteracao.EDITOU);
    }

    @Test
    void salvamentoDeParte_novaNumaFichaCriadaAgora_naoGrava() {
        autenticarComo(7L);

        entityAuditService.registrarSalvamentoDeParte("AvaliacaoSocioeconomica", 5L, 7L,
                fichaCriadaEm(LocalDateTime.now()), true, RetratoAuditoria.de((Object) null), RetratoAuditoria.de(true));

        verify(entityAuditLogRepository, never()).save(any());
    }

    @Test
    void salvamentoDeParte_novaNumaFichaCriadaHaUmaHora_gravaPreencheu() {
        autenticarComo(7L);

        entityAuditService.registrarSalvamentoDeParte("AvaliacaoSocioeconomica", 5L, 7L,
                fichaCriadaEm(LocalDateTime.now().minusHours(1)), true, RetratoAuditoria.de((Object) null), RetratoAuditoria.de(true));

        EntityAuditLog linha = linhaGravada();
        assertThat(linha.getAcao()).isEqualTo(AcaoAlteracao.PREENCHEU);
        assertThat(linha.getEntidadeId()).isEqualTo(5L);
        assertThat(linha.getFichaId()).isEqualTo(1L);
    }

    @Test
    void salvamentoDeParte_novaSemNadaPreenchido_naoGrava() {
        autenticarComo(7L);

        entityAuditService.registrarSalvamentoDeParte("AcolhimentoEquipe", 5L, 7L,
                fichaCriadaEm(LocalDateTime.now().minusHours(1)), true,
                RetratoAuditoria.de(null, "", List.of()), RetratoAuditoria.de(null, "  ", List.of()));

        verify(entityAuditLogRepository, never()).save(any());
    }
}
