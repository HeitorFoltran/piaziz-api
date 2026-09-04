package com.azizaid.hub.service;

import com.azizaid.hub.dto.request.FichaRequestDTO;
import com.azizaid.hub.model.Ficha;
import com.azizaid.hub.model.TipoAcompanhamento;
import com.azizaid.hub.repository.FichaRepository;
import com.azizaid.hub.repository.TipoAcompanhamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.azizaid.hub.support.FichaTestFactory.construirDtoValido;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FichaServiceTest {

    @Mock
    FichaRepository fichaRepository;

    @Mock
    EntityAuditService entityAuditService;

    @Mock
    TipoAcompanhamentoRepository tipoAcompanhamentoRepository;

    FichaService fichaService;

    @BeforeEach
    void setUp() {
        fichaService = new FichaService(fichaRepository, entityAuditService, tipoAcompanhamentoRepository);
    }

    @Test
    void criar_comCpfNovo_salvaFicha() {
        when(fichaRepository.existsByCpf("12345678900")).thenReturn(false);
        when(fichaRepository.buscarMaiorSequencialCodigo()).thenReturn(0);
        when(fichaRepository.save(any(Ficha.class))).thenAnswer(inv -> inv.getArgument(0));

        FichaRequestDTO dto = construirDtoValido("12345678900");

        fichaService.criar(dto);

        verify(fichaRepository).save(any(Ficha.class));
    }

    @Test
    void criar_comCpfDuplicado_lancaExcecaoENaoSalva() {
        when(fichaRepository.existsByCpf("12345678900")).thenReturn(true);
        FichaRequestDTO dto = construirDtoValido("12345678900");

        assertThrows(IllegalArgumentException.class, () -> fichaService.criar(dto));

        verify(fichaRepository, never()).save(any());
    }

    @Test
    void atribuirTiposAcompanhamento_comIdsValidos_atualiza() {
        Ficha ficha = Ficha.builder().id(1L).build();
        TipoAcompanhamento t1 = TipoAcompanhamento.builder().id(10L).nome("Jurídico").build();
        TipoAcompanhamento t2 = TipoAcompanhamento.builder().id(20L).nome("Psicológico").build();

        when(fichaRepository.findById(1L)).thenReturn(Optional.of(ficha));
        when(tipoAcompanhamentoRepository.findAllById(List.of(10L, 20L))).thenReturn(List.of(t1, t2));
        when(fichaRepository.save(any(Ficha.class))).thenAnswer(inv -> inv.getArgument(0));

        fichaService.atribuirTiposAcompanhamento(1L, List.of(10L, 20L));

        assertEquals(Set.of(t1, t2), ficha.getTiposAcompanhamento());
    }

    @Test
    void atribuirTiposAcompanhamento_comIdInexistente_lancaExcecao() {
        Ficha ficha = Ficha.builder().id(1L).build();
        TipoAcompanhamento t1 = TipoAcompanhamento.builder().id(10L).nome("Jurídico").build();

        when(fichaRepository.findById(1L)).thenReturn(Optional.of(ficha));
        when(tipoAcompanhamentoRepository.findAllById(List.of(10L, 999L))).thenReturn(List.of(t1));

        assertThrows(IllegalArgumentException.class,
                () -> fichaService.atribuirTiposAcompanhamento(1L, List.of(10L, 999L)));

        verify(fichaRepository, never()).save(any());
    }

    @Test
    void atribuirTiposAcompanhamento_comListaVazia_removeTodasAsTags() {
        TipoAcompanhamento t1 = TipoAcompanhamento.builder().id(10L).nome("Jurídico").build();
        Ficha ficha = Ficha.builder().id(1L).tiposAcompanhamento(new HashSet<>(Set.of(t1))).build();

        when(fichaRepository.findById(1L)).thenReturn(Optional.of(ficha));
        when(tipoAcompanhamentoRepository.findAllById(List.of())).thenReturn(List.of());
        when(fichaRepository.save(any(Ficha.class))).thenAnswer(inv -> inv.getArgument(0));

        fichaService.atribuirTiposAcompanhamento(1L, List.of());

        assertTrue(ficha.getTiposAcompanhamento().isEmpty());
    }
}
