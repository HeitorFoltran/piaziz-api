package com.azizaid.hub.service;

import com.azizaid.hub.config.JwtService;
import com.azizaid.hub.config.RateLimitProperties;
import com.azizaid.hub.dto.request.LoginRequestDTO;
import com.azizaid.hub.exception.CredenciaisInvalidasException;
import com.azizaid.hub.exception.MuitasTentativasException;
import com.azizaid.hub.model.AuthAuditLog;
import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.model.enums.PapelProfissional;
import com.azizaid.hub.repository.AuthAuditLogRepository;
import com.azizaid.hub.repository.ProfissionalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    PasswordEncoder passwordEncoder;

    @Mock
    JwtService jwtService;

    AuthService authService;

    @BeforeEach
    void setUp() {
        RateLimitProperties rateLimitProperties = new RateLimitProperties(
                null, null, null, new RateLimitProperties.LoginPorEmail(10, 15));
        authService = new AuthService(
                profissionalRepository, authAuditLogRepository, passwordEncoder, jwtService, rateLimitProperties);
    }

    @Test
    void login_comContaInativaESenhaCerta_lancaCredenciaisInvalidasEGravaMotivo() {
        Profissional profissional = Profissional.builder()
                .id(1L)
                .nome("Inativa")
                .cpf("12345678900")
                .email("inativa@azizaidhub.local")
                .senhaHash("hash")
                .role(PapelProfissional.PADRAO)
                .ativo(false)
                .build();

        when(authAuditLogRepository.countByEmailTentadoAndSucessoFalseAndTimestampAfter(anyString(), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(profissionalRepository.findByEmail("inativa@azizaidhub.local")).thenReturn(Optional.of(profissional));
        when(passwordEncoder.matches("senha123", "hash")).thenReturn(true);

        assertThrows(CredenciaisInvalidasException.class,
                () -> authService.login(new LoginRequestDTO("inativa@azizaidhub.local", "senha123"), "127.0.0.1"));

        ArgumentCaptor<AuthAuditLog> captor = ArgumentCaptor.forClass(AuthAuditLog.class);
        verify(authAuditLogRepository).save(captor.capture());
        assertEquals("conta inativa", captor.getValue().getMotivoFalha());
    }

    @Test
    void login_comDezFalhasNaJanela_lancaMuitasTentativasSemChamarPasswordEncoder() {
        when(authAuditLogRepository.countByEmailTentadoAndSucessoFalseAndTimestampAfter(anyString(), any(LocalDateTime.class)))
                .thenReturn(10L);

        assertThrows(MuitasTentativasException.class,
                () -> authService.login(new LoginRequestDTO("alvo@azizaidhub.local", "qualquer"), "127.0.0.1"));

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }
}
