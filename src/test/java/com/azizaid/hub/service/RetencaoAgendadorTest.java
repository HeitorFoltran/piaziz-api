package com.azizaid.hub.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetencaoAgendadorTest {

    @Mock
    RetencaoFichaPendenteService retencaoFichaPendenteService;

    @Mock
    RetencaoLeituraAuditService retencaoLeituraAuditService;

    @Mock
    RetencaoIpService retencaoIpService;

    @Mock
    RetencaoConviteService retencaoConviteService;

    @InjectMocks
    RetencaoAgendador retencaoAgendador;

    @Test
    void executar_comFalhaNumaEtapa_rodaAsOutrasMesmoAssim() {
        when(retencaoFichaPendenteService.aplicarRetencao()).thenThrow(new IllegalStateException("falha de teste"));

        retencaoAgendador.executar();

        verify(retencaoLeituraAuditService).aplicarRetencao();
        verify(retencaoIpService).limparIps();
        verify(retencaoConviteService).limparTokensVencidos();
    }
}
