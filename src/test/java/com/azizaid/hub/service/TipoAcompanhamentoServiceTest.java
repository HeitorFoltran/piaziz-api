package com.azizaid.hub.service;

import com.azizaid.hub.dto.request.TipoAcompanhamentoRequestDTO;
import com.azizaid.hub.model.TipoAcompanhamento;
import com.azizaid.hub.repository.TipoAcompanhamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TipoAcompanhamentoServiceTest {

    @Mock
    TipoAcompanhamentoRepository tipoAcompanhamentoRepository;

    TipoAcompanhamentoService tipoAcompanhamentoService;

    @BeforeEach
    void setUp() {
        tipoAcompanhamentoService = new TipoAcompanhamentoService(tipoAcompanhamentoRepository);
    }

    @Test
    void criar_comNomeNovo_salva() {
        when(tipoAcompanhamentoRepository.existsByNomeIgnoreCase("Jurídico")).thenReturn(false);
        when(tipoAcompanhamentoRepository.save(any(TipoAcompanhamento.class))).thenAnswer(inv -> inv.getArgument(0));

        tipoAcompanhamentoService.criar(new TipoAcompanhamentoRequestDTO("Jurídico"));

        verify(tipoAcompanhamentoRepository).save(any(TipoAcompanhamento.class));
    }

    @Test
    void criar_comNomeDuplicado_lancaExcecaoENaoSalva() {
        when(tipoAcompanhamentoRepository.existsByNomeIgnoreCase("Jurídico")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> tipoAcompanhamentoService.criar(new TipoAcompanhamentoRequestDTO("Jurídico")));

        verify(tipoAcompanhamentoRepository, never()).save(any());
    }

    @Test
    void atualizar_paraNomeJaUsadoPorOutro_lancaExcecao() {
        TipoAcompanhamento existente = TipoAcompanhamento.builder().id(1L).nome("Jurídico").build();
        when(tipoAcompanhamentoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(tipoAcompanhamentoRepository.existsByNomeIgnoreCase("Psicológico")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> tipoAcompanhamentoService.atualizar(1L, new TipoAcompanhamentoRequestDTO("Psicológico")));

        verify(tipoAcompanhamentoRepository, never()).save(any());
    }

    @Test
    void atualizar_mantendoOMesmoNome_naoLancaExcecao() {
        TipoAcompanhamento existente = TipoAcompanhamento.builder().id(1L).nome("Jurídico").build();
        when(tipoAcompanhamentoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(tipoAcompanhamentoRepository.save(any(TipoAcompanhamento.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() ->
                tipoAcompanhamentoService.atualizar(1L, new TipoAcompanhamentoRequestDTO("JURÍDICO")));

        verify(tipoAcompanhamentoRepository, never()).existsByNomeIgnoreCase(any());
        verify(tipoAcompanhamentoRepository).save(any(TipoAcompanhamento.class));
    }
}
