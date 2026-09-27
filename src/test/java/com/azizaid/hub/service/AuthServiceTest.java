package com.azizaid.hub.service;

import com.azizaid.hub.config.JwtService;
import com.azizaid.hub.config.RateLimitProperties;
import com.azizaid.hub.dto.request.LoginRequestDTO;
import com.azizaid.hub.exception.CredenciaisInvalidasException;
import com.azizaid.hub.exception.MuitasTentativasException;
import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.model.enums.PapelProfissional;
import com.azizaid.hub.repository.AuthAuditLogRepository;
import com.azizaid.hub.repository.ProfissionalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    ProfissionalRepository profissionalRepository;

    @Mock
    AuthAuditLogRepository authAuditLogRepository;

    @Mock
    AuthAuditLogService authAuditLogService;

    @Mock
    ContaAuditLogService contaAuditLogService;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtService jwtService;

    AuthService authService;

    @BeforeEach
    void setUp() {
        RateLimitProperties rateLimitProperties = new RateLimitProperties(
                null, null, null, new RateLimitProperties.LoginPorEmail(10, 15));
        authService = new AuthService(
                profissionalRepository, authAuditLogRepository, authAuditLogService, contaAuditLogService, passwordEncoder,
                jwtService,
                rateLimitProperties);
    }

    @Test
    void login_comContaInativaESenhaCerta_lancaCredenciaisInvalidasEGravaMotivo() {
        Profissional profissional = Profissional.builder()
                .id(1L)
                .nome("Inativa")
                .cpf("12345678900")
                .username("inativa")
                .email("inativa@azizaidhub.local")
                .senhaHash("hash")
                .role(PapelProfissional.PADRAO)
                .ativo(false)
                .build();

        when(profissionalRepository.findByEmail("inativa@azizaidhub.local")).thenReturn(Optional.of(profissional));
        when(authAuditLogRepository.contarFalhasRecentesDaConta(eq(1L), any(LocalDateTime.class), anyString()))
                .thenReturn(0L);
        when(passwordEncoder.matches("senha123", "hash")).thenReturn(true);

        assertThrows(CredenciaisInvalidasException.class,
                () -> authService.login(new LoginRequestDTO("Inativa@AzizaidHub.local ", "senha123"), "127.0.0.1"));

        verify(authAuditLogService).registrar("inativa@azizaidhub.local", 1L, false, "127.0.0.1", "conta inativa");
    }

    @Test
    void login_porUsername_buscaPorUsernameEContaFalhasPelaConta() {
        Profissional profissional = Profissional.builder()
                .id(2L)
                .nome("Por Username")
                .cpf("12345678900")
                .username("por.username")
                .senhaHash("hash")
                .role(PapelProfissional.PADRAO)
                .build();

        when(profissionalRepository.findByUsername("por.username")).thenReturn(Optional.of(profissional));
        when(authAuditLogRepository.contarFalhasRecentesDaConta(eq(2L), any(LocalDateTime.class), anyString()))
                .thenReturn(10L);

        assertThrows(MuitasTentativasException.class,
                () -> authService.login(new LoginRequestDTO("Por.Username", "qualquer"), "127.0.0.1"));

        verify(authAuditLogService).registrar("por.username", 2L, false, "127.0.0.1", "limite por conta");
        verify(profissionalRepository, never()).findByEmail(anyString());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_comDezFalhasNaJanela_lancaMuitasTentativasSemChamarPasswordEncoder() {
        when(authAuditLogRepository.contarFalhasRecentes(anyString(), any(LocalDateTime.class), anyString()))
                .thenReturn(10L);

        assertThrows(MuitasTentativasException.class,
                () -> authService.login(new LoginRequestDTO("alvo@azizaidhub.local", "qualquer"), "127.0.0.1"));

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_comLimiteAtingido_naoContaTentativasBarradasAnterioresNaQuery() {
        when(authAuditLogRepository.contarFalhasRecentes(eq("alvo@azizaidhub.local"), any(LocalDateTime.class), eq("limite por conta")))
                .thenReturn(10L);

        assertThrows(MuitasTentativasException.class,
                () -> authService.login(new LoginRequestDTO("alvo@azizaidhub.local", "qualquer"), "127.0.0.1"));

        verify(authAuditLogService).registrar("alvo@azizaidhub.local", null, false, "127.0.0.1", "limite por conta");
        verify(authAuditLogRepository).contarFalhasRecentes(eq("alvo@azizaidhub.local"), any(LocalDateTime.class), eq("limite por conta"));
    }
}
