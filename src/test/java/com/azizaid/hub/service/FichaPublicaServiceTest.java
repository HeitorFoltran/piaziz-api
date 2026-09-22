package com.azizaid.hub.service;

import com.azizaid.hub.config.ConviteTokenService;
import com.azizaid.hub.config.ConviteTokenService.MotivoTokenInvalido;
import com.azizaid.hub.config.ConviteTokenService.ValidacaoTokenResult;
import com.azizaid.hub.dto.request.FichaPublicaRequestDTO;
import com.azizaid.hub.dto.response.FichaPublicaStatusResponseDTO;
import com.azizaid.hub.exception.RecursoNaoEncontradoException;
import com.azizaid.hub.model.ConviteFicha;
import com.azizaid.hub.model.FichaPendente;
import com.azizaid.hub.model.FichaPublicaAuditLog;
import com.azizaid.hub.model.enums.ResultadoFichaPublica;
import com.azizaid.hub.model.enums.StatusConvite;
import com.azizaid.hub.repository.ConviteFichaRepository;
import com.azizaid.hub.repository.FichaPendenteRepository;
import com.azizaid.hub.repository.FichaPublicaAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FichaPublicaServiceTest {

    @Mock
    ConviteTokenService conviteTokenService;

    @Mock
    ConviteFichaRepository conviteFichaRepository;

    @Mock
    FichaPendenteRepository fichaPendenteRepository;

    @Mock
    FichaPublicaAuditLogRepository auditLogRepository;

    FichaPublicaService fichaPublicaService;

    @BeforeEach
    void setUp() {
        fichaPublicaService = new FichaPublicaService(conviteTokenService, conviteFichaRepository,
                fichaPendenteRepository, auditLogRepository);
    }

    @Test
    void status_tokenValido_retornaValido() {
        ConviteFicha convite = ConviteFicha.builder().id(1L).status(StatusConvite.ATIVO).build();
        when(conviteTokenService.validar("tok")).thenReturn(ValidacaoTokenResult.valido(convite));

        FichaPublicaStatusResponseDTO resposta = fichaPublicaService.status("tok");

        assertTrue(resposta.valido());
        assertNull(resposta.motivo());
    }

    @Test
    void status_tokenExpirado_retornaMotivoExpirado() {
        when(conviteTokenService.validar("tok")).thenReturn(
                ValidacaoTokenResult.invalido(MotivoTokenInvalido.EXPIRADO));

        FichaPublicaStatusResponseDTO resposta = fichaPublicaService.status("tok");

        assertFalse(resposta.valido());
        assertEquals("expirado", resposta.motivo());
    }

    @Test
    void submeter_tokenInvalido_registraAuditoriaELanca404() {
        when(conviteTokenService.validar("tok")).thenReturn(
                ValidacaoTokenResult.invalido(MotivoTokenInvalido.INVALIDO));
        FichaPublicaRequestDTO dto = new FichaPublicaRequestDTO("Maria", "12345678909", null, null, null);

        assertThrows(RecursoNaoEncontradoException.class,
                () -> fichaPublicaService.submeter("tok", dto, "127.0.0.1"));

        verify(auditLogRepository).save(argThat(log ->
                log.getResultado() == ResultadoFichaPublica.TOKEN_INVALIDO));
        verify(fichaPendenteRepository, never()).save(any());
    }

    @Test
    void submeter_tokenValido_criaFichaPendenteEMarcaConviteUsado() {
        ConviteFicha convite = ConviteFicha.builder().id(3L).status(StatusConvite.ATIVO).build();
        when(conviteTokenService.validar("tok")).thenReturn(ValidacaoTokenResult.valido(convite));
        when(conviteFichaRepository.marcarComoUsadoSeAtivo(eq(3L), eq(StatusConvite.USADO),
                eq(StatusConvite.ATIVO), any())).thenReturn(1);
        FichaPublicaRequestDTO dto = new FichaPublicaRequestDTO("Maria", "12345678909", "4599990000", 30, "relato");

        fichaPublicaService.submeter("tok", dto, "127.0.0.1");

        verify(fichaPendenteRepository).save(argThat((FichaPendente p) ->
                p.getNome().equals("Maria") && p.getConviteId().equals(3L)));
        verify(auditLogRepository).save(argThat((FichaPublicaAuditLog log) ->
                log.getResultado() == ResultadoFichaPublica.SUBMETIDO && log.getConviteId().equals(3L)));
    }

    @Test
    void submeter_perdeCorridaDeUso_registraTokenJaUsadoELanca404() {
        ConviteFicha convite = ConviteFicha.builder().id(3L).status(StatusConvite.ATIVO).build();
        when(conviteTokenService.validar("tok")).thenReturn(ValidacaoTokenResult.valido(convite));
        when(conviteFichaRepository.marcarComoUsadoSeAtivo(eq(3L), eq(StatusConvite.USADO),
                eq(StatusConvite.ATIVO), any())).thenReturn(0);
        FichaPublicaRequestDTO dto = new FichaPublicaRequestDTO("Maria", "12345678909", null, null, null);

        assertThrows(RecursoNaoEncontradoException.class,
                () -> fichaPublicaService.submeter("tok", dto, "127.0.0.1"));

        verify(auditLogRepository).save(argThat((FichaPublicaAuditLog log) ->
                log.getResultado() == ResultadoFichaPublica.TOKEN_JA_USADO));
        verify(fichaPendenteRepository, never()).save(any());
    }
}
