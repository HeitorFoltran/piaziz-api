package com.azizaid.hub.service;

import com.azizaid.hub.config.ConviteTokenService;
import com.azizaid.hub.config.ConviteTokenService.MotivoTokenInvalido;
import com.azizaid.hub.config.ConviteTokenService.ValidacaoTokenResult;
import com.azizaid.hub.dto.request.FichaPublicaRequestDTO;
import com.azizaid.hub.dto.request.FichaRequestDTO;
import com.azizaid.hub.dto.response.FichaPublicaStatusResponseDTO;
import com.azizaid.hub.exception.RecursoNaoEncontradoException;
import com.azizaid.hub.model.ConviteFicha;
import com.azizaid.hub.model.FichaPendente;
import com.azizaid.hub.model.enums.ResultadoFichaPublica;
import com.azizaid.hub.model.enums.StatusConvite;
import com.azizaid.hub.repository.ConviteFichaRepository;
import com.azizaid.hub.repository.FichaPendenteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.azizaid.hub.support.FichaTestFactory.construirDtoValido;
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
    FichaPublicaAuditLogService auditLogService;

    FichaPublicaService fichaPublicaService;

    @BeforeEach
    void setUp() {
        fichaPublicaService = new FichaPublicaService(conviteTokenService, conviteFichaRepository,
                fichaPendenteRepository, auditLogService, new ObjectMapper());
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
    void submeter_comCpfInvalido_lancaExcecaoRegistraAuditoriaENaoConsomeToken() {
        FichaRequestDTO fichaDto = construirDtoValido("11111111111");
        FichaPublicaRequestDTO dto = new FichaPublicaRequestDTO(fichaDto, null, null, null);

        assertThrows(IllegalArgumentException.class,
                () -> fichaPublicaService.submeter("tok", dto, "127.0.0.1"));

        verify(auditLogService).registrar(null, "127.0.0.1", ResultadoFichaPublica.VALIDACAO_FALHOU);
        verify(conviteTokenService, never()).validar(any());
        verify(fichaPendenteRepository, never()).save(any());
    }

    @Test
    void submeter_tokenInvalido_registraAuditoriaELanca404() {
        when(conviteTokenService.validar("tok")).thenReturn(
                ValidacaoTokenResult.invalido(MotivoTokenInvalido.INVALIDO));
        FichaPublicaRequestDTO dto = new FichaPublicaRequestDTO(construirDtoValido("12345678909"), null, null, null);

        assertThrows(RecursoNaoEncontradoException.class,
                () -> fichaPublicaService.submeter("tok", dto, "127.0.0.1"));

        verify(auditLogService).registrar(null, "127.0.0.1", ResultadoFichaPublica.TOKEN_INVALIDO);
        verify(fichaPendenteRepository, never()).save(any());
    }

    @Test
    void submeter_tokenValido_criaFichaPendenteComJsonEMarcaConviteUsado() {
        ConviteFicha convite = ConviteFicha.builder().id(3L).status(StatusConvite.ATIVO).build();
        when(conviteTokenService.validar("tok")).thenReturn(ValidacaoTokenResult.valido(convite));
        when(conviteFichaRepository.marcarComoUsadoSeAtivo(eq(3L), eq(StatusConvite.USADO),
                eq(StatusConvite.ATIVO), any())).thenReturn(1);
        FichaPublicaRequestDTO dto = new FichaPublicaRequestDTO(
                construirDtoValido("12345678909"), null, null, "relato");

        fichaPublicaService.submeter("tok", dto, "127.0.0.1");

        verify(fichaPendenteRepository).save(argThat((FichaPendente p) ->
                p.getNome().equals("Vítima Teste")
                        && p.getConviteId().equals(3L)
                        && p.getDadosFichaJson() != null
                        && p.getDadosFichaJson().contains("\"numeroCaso\":null")
                        && p.getDadosAvaliacaoJson() == null
                        && p.getDadosHistoricoJson() == null));
        verify(auditLogService).registrar(3L, "127.0.0.1", ResultadoFichaPublica.SUBMETIDO);
    }

    @Test
    void submeter_comNumeroCasoEStatusNoPayload_zeraAntesDeSalvar() {
        ConviteFicha convite = ConviteFicha.builder().id(3L).status(StatusConvite.ATIVO).build();
        when(conviteTokenService.validar("tok")).thenReturn(ValidacaoTokenResult.valido(convite));
        when(conviteFichaRepository.marcarComoUsadoSeAtivo(eq(3L), eq(StatusConvite.USADO),
                eq(StatusConvite.ATIVO), any())).thenReturn(1);
        FichaRequestDTO fichaDto = new FichaRequestDTO(
                "2024/999999", "Vítima Teste", "12345678909", 30, "11999999999",
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                com.azizaid.hub.model.enums.StatusFicha.ENCERRADO);
        FichaPublicaRequestDTO dto = new FichaPublicaRequestDTO(fichaDto, null, null, null);

        fichaPublicaService.submeter("tok", dto, "127.0.0.1");

        verify(fichaPendenteRepository).save(argThat((FichaPendente p) ->
                p.getDadosFichaJson().contains("\"numeroCaso\":null")
                        && p.getDadosFichaJson().contains("\"status\":null")));
    }

    @Test
    void submeter_perdeCorridaDeUso_registraTokenJaUsadoELanca404() {
        ConviteFicha convite = ConviteFicha.builder().id(3L).status(StatusConvite.ATIVO).build();
        when(conviteTokenService.validar("tok")).thenReturn(ValidacaoTokenResult.valido(convite));
        when(conviteFichaRepository.marcarComoUsadoSeAtivo(eq(3L), eq(StatusConvite.USADO),
                eq(StatusConvite.ATIVO), any())).thenReturn(0);
        FichaPublicaRequestDTO dto = new FichaPublicaRequestDTO(construirDtoValido("12345678909"), null, null, null);

        assertThrows(RecursoNaoEncontradoException.class,
                () -> fichaPublicaService.submeter("tok", dto, "127.0.0.1"));

        verify(auditLogService).registrar(3L, "127.0.0.1", ResultadoFichaPublica.TOKEN_JA_USADO);
        verify(fichaPendenteRepository, never()).save(any());
    }
}
