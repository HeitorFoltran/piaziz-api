package com.azizaid.hub.service;

import com.azizaid.hub.model.EntityAuditLog;
import com.azizaid.hub.model.Ficha;
import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.repository.EntityAuditLogRepository;
import com.azizaid.hub.repository.ProfissionalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FichaAlteracaoServiceTest {

    @Mock
    FichaService fichaService;

    @Mock
    EntityAuditLogRepository entityAuditLogRepository;

    @Mock
    ProfissionalRepository profissionalRepository;

    FichaAlteracaoService fichaAlteracaoService;

    @BeforeEach
    void setUp() {
        fichaAlteracaoService = new FichaAlteracaoService(fichaService, entityAuditLogRepository, profissionalRepository);
    }

    @Test
    void listarPorFicha_mapeiaNomesDeEditorEDono_mantendoOrdemDoRepositorio() {
        when(fichaService.buscarEntidade(1L)).thenReturn(Ficha.builder().id(1L).build());

        EntityAuditLog linhaMaisRecente = EntityAuditLog.builder()
                .id(2L).tipoEntidade("Ficha").entidadeId(1L).fichaId(1L)
                .editorId(20L).donoId(10L).timestamp(LocalDateTime.now())
                .build();
        EntityAuditLog linhaMaisAntiga = EntityAuditLog.builder()
                .id(1L).tipoEntidade("AvaliacaoSocioeconomica").entidadeId(5L).fichaId(1L)
                .editorId(20L).donoId(10L).timestamp(LocalDateTime.now().minusDays(1))
                .build();
        when(entityAuditLogRepository.findByFichaIdOrderByTimestampDesc(1L))
                .thenReturn(List.of(linhaMaisRecente, linhaMaisAntiga));

        when(profissionalRepository.findAllById(List.of(20L, 10L))).thenReturn(List.of(
                Profissional.builder().id(20L).nome("Beatriz Editor").build(),
                Profissional.builder().id(10L).nome("Ana Dona").build()));

        var resultado = fichaAlteracaoService.listarPorFicha(1L);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).id()).isEqualTo(2L);
        assertThat(resultado.get(0).editorNome()).isEqualTo("Beatriz Editor");
        assertThat(resultado.get(0).donoNome()).isEqualTo("Ana Dona");
        assertThat(resultado.get(1).id()).isEqualTo(1L);
    }

    @Test
    void listarPorFicha_comFichaInexistente_lancaExcecaoDoProjeto() {
        when(fichaService.buscarEntidade(99L))
                .thenThrow(com.azizaid.hub.exception.RecursoNaoEncontradoException.de("Ficha", 99L));

        assertThrows(com.azizaid.hub.exception.RecursoNaoEncontradoException.class,
                () -> fichaAlteracaoService.listarPorFicha(99L));
    }
}
