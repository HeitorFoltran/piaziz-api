package com.azizaid.hub.service;

import com.azizaid.hub.dto.request.EncaminhamentoRequestDTO;
import com.azizaid.hub.model.Encaminhamento;
import com.azizaid.hub.model.Ficha;
import com.azizaid.hub.model.Servico;
import com.azizaid.hub.model.enums.StatusFicha;
import com.azizaid.hub.repository.EncaminhamentoRepository;
import com.azizaid.hub.repository.ServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static com.azizaid.hub.support.EncaminhamentoTestFactory.construirEncaminhamentoDtoValido;
import static com.azizaid.hub.support.FichaTestFactory.construirFichaComStatus;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EncaminhamentoServiceTest {

    @Mock
    EncaminhamentoRepository encaminhamentoRepository;

    @Mock
    ServicoRepository servicoRepository;

    @Mock
    FichaService fichaService;

    EncaminhamentoService encaminhamentoService;

    @BeforeEach
    void setUp() {
        encaminhamentoService = new EncaminhamentoService(encaminhamentoRepository, servicoRepository, fichaService);
    }

    @Test
    void criar_comFichaAtiva_salvaEncaminhamento() {
        Ficha ficha = construirFichaComStatus(StatusFicha.ATIVO);
        when(fichaService.buscarEntidade(1L)).thenReturn(ficha);
        when(servicoRepository.findById(2L)).thenReturn(Optional.of(new Servico(2L, "Serviço Teste")));
        when(encaminhamentoRepository.save(any(Encaminhamento.class))).thenAnswer(inv -> inv.getArgument(0));

        EncaminhamentoRequestDTO dto = construirEncaminhamentoDtoValido(2L);

        encaminhamentoService.criar(1L, dto);

        verify(encaminhamentoRepository).save(any(Encaminhamento.class));
    }

    @Test
    void criar_comFichaPausada_salvaEncaminhamento() {
        Ficha ficha = construirFichaComStatus(StatusFicha.PAUSADO);
        when(fichaService.buscarEntidade(1L)).thenReturn(ficha);
        when(servicoRepository.findById(2L)).thenReturn(Optional.of(new Servico(2L, "Serviço Teste")));
        when(encaminhamentoRepository.save(any(Encaminhamento.class))).thenAnswer(inv -> inv.getArgument(0));

        EncaminhamentoRequestDTO dto = construirEncaminhamentoDtoValido(2L);

        encaminhamentoService.criar(1L, dto);

        verify(encaminhamentoRepository).save(any(Encaminhamento.class));
    }

    @Test
    void criar_comFichaEncerrada_lancaExcecaoENaoSalva() {
        Ficha ficha = construirFichaComStatus(StatusFicha.ENCERRADO);
        when(fichaService.buscarEntidade(1L)).thenReturn(ficha);

        EncaminhamentoRequestDTO dto = construirEncaminhamentoDtoValido(2L);

        assertThrows(IllegalArgumentException.class, () -> encaminhamentoService.criar(1L, dto));

        verify(encaminhamentoRepository, never()).save(any());
    }

    @Test
    void criar_comDataRetornoAnteriorADataEncaminhamento_lancaExcecaoENaoSalva() {
        Ficha ficha = construirFichaComStatus(StatusFicha.ATIVO);
        when(fichaService.buscarEntidade(1L)).thenReturn(ficha);

        EncaminhamentoRequestDTO dto = new EncaminhamentoRequestDTO(
                2L, null, "Profissional Teste",
                LocalDate.now(), LocalDate.now().minusDays(1),
                "Encaminhamento de teste");

        assertThrows(IllegalArgumentException.class, () -> encaminhamentoService.criar(1L, dto));

        verify(encaminhamentoRepository, never()).save(any());
    }
}
