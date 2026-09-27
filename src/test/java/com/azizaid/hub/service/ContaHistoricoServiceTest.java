package com.azizaid.hub.service;

import com.azizaid.hub.dto.response.ContaHistoricoResponseDTO;
import com.azizaid.hub.exception.RecursoNaoEncontradoException;
import com.azizaid.hub.model.ContaAuditLog;
import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.model.enums.AcaoConta;
import com.azizaid.hub.repository.ContaAuditLogRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContaHistoricoServiceTest {

    @Mock
    ContaAuditLogRepository contaAuditLogRepository;

    @Mock
    ProfissionalRepository profissionalRepository;

    ContaHistoricoService contaHistoricoService;

    @BeforeEach
    void setUp() {
        contaHistoricoService = new ContaHistoricoService(contaAuditLogRepository, profissionalRepository);
    }

    private static ContaAuditLog linha(Long id, Long autorId, AcaoConta acao) {
        return ContaAuditLog.builder()
                .id(id).profissionalId(10L).autorId(autorId).acao(acao)
                .timestamp(LocalDateTime.of(2026, 9, 27, 10, 0))
                .build();
    }

    @Test
    void listarPorProfissional_resolveAutoresNumaConsultaSo_eAutorRemovidoFicaNull() {
        when(profissionalRepository.existsById(10L)).thenReturn(true);
        when(contaAuditLogRepository.findByProfissionalIdOrderByTimestampDescIdDesc(10L)).thenReturn(List.of(
                linha(3L, 1L, AcaoConta.RESETAR_SENHA),
                linha(2L, 2L, AcaoConta.EDITAR),
                linha(1L, 1L, AcaoConta.CRIAR)));
        when(profissionalRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(Profissional.builder().id(1L).nome("Ana").build()));

        List<ContaHistoricoResponseDTO> resposta = contaHistoricoService.listarPorProfissional(10L);

        assertThat(resposta).extracting(ContaHistoricoResponseDTO::id).containsExactly(3L, 2L, 1L);
        assertThat(resposta).extracting(ContaHistoricoResponseDTO::autorNome).containsExactly("Ana", null, "Ana");
        verify(profissionalRepository, times(1)).findAllById(any());
    }

    @Test
    void listarPorProfissional_idInexistente_lancaRecursoNaoEncontrado() {
        when(profissionalRepository.existsById(99L)).thenReturn(false);

        assertThrows(RecursoNaoEncontradoException.class, () -> contaHistoricoService.listarPorProfissional(99L));
        verify(contaAuditLogRepository, never()).findByProfissionalIdOrderByTimestampDescIdDesc(any());
    }
}
