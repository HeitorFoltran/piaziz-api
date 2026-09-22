package com.azizaid.hub.service;

import com.azizaid.hub.config.ConviteTokenService;
import com.azizaid.hub.dto.response.ConviteFichaResponseDTO;
import com.azizaid.hub.model.ConviteFicha;
import com.azizaid.hub.model.enums.StatusConvite;
import com.azizaid.hub.repository.ConviteFichaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConviteFichaServiceTest {

    @Mock
    ConviteFichaRepository conviteFichaRepository;

    @Mock
    ConviteTokenService conviteTokenService;

    ConviteFichaService conviteFichaService;

    @BeforeEach
    void setUp() {
        conviteFichaService = new ConviteFichaService(conviteFichaRepository, conviteTokenService,
                "http://localhost:5173");
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(Long id, String role) {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(id, null, authorities));
    }

    @Test
    void criar_geraConviteEDevolveLinkComToken() {
        autenticarComo(1L, "PADRAO");
        when(conviteTokenService.gerarTokenCru()).thenReturn("token-cru");
        when(conviteTokenService.hash("token-cru")).thenReturn("hash-do-token");
        when(conviteTokenService.calcularExpiracao()).thenReturn(LocalDateTime.now().plusDays(7));
        when(conviteFichaRepository.save(any(ConviteFicha.class))).thenAnswer(inv -> {
            ConviteFicha c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        ConviteFichaResponseDTO resposta = conviteFichaService.criar();

        assertEquals("http://localhost:5173/ficha-publica/token-cru", resposta.linkCompleto());
        verify(conviteFichaRepository).save(argThat(c -> c.getTokenHash().equals("hash-do-token")
                && c.getCriadoPorId().equals(1L)));
    }

    @Test
    void cancelar_peloCriador_cancela() {
        autenticarComo(1L, "PADRAO");
        ConviteFicha convite = ConviteFicha.builder().id(5L).criadoPorId(1L).status(StatusConvite.ATIVO).build();
        when(conviteFichaRepository.findById(5L)).thenReturn(java.util.Optional.of(convite));
        when(conviteFichaRepository.save(any(ConviteFicha.class))).thenAnswer(inv -> inv.getArgument(0));

        ConviteFichaResponseDTO resposta = conviteFichaService.cancelar(5L);

        assertEquals(StatusConvite.CANCELADO.name(), resposta.status());
    }

    @Test
    void cancelar_porOutroProfissionalNaoDev_lancaAccessDenied() {
        autenticarComo(2L, "PADRAO");
        ConviteFicha convite = ConviteFicha.builder().id(5L).criadoPorId(1L).status(StatusConvite.ATIVO).build();
        when(conviteFichaRepository.findById(5L)).thenReturn(java.util.Optional.of(convite));

        assertThrows(AccessDeniedException.class, () -> conviteFichaService.cancelar(5L));
        verify(conviteFichaRepository, never()).save(any());
    }

    @Test
    void cancelar_porDevQueNaoCriou_cancela() {
        autenticarComo(2L, "DEV");
        ConviteFicha convite = ConviteFicha.builder().id(5L).criadoPorId(1L).status(StatusConvite.ATIVO).build();
        when(conviteFichaRepository.findById(5L)).thenReturn(java.util.Optional.of(convite));
        when(conviteFichaRepository.save(any(ConviteFicha.class))).thenAnswer(inv -> inv.getArgument(0));

        ConviteFichaResponseDTO resposta = conviteFichaService.cancelar(5L);

        assertEquals(StatusConvite.CANCELADO.name(), resposta.status());
    }

    @Test
    void cancelar_conviteJaUsado_lancaExcecao() {
        autenticarComo(1L, "PADRAO");
        ConviteFicha convite = ConviteFicha.builder().id(5L).criadoPorId(1L).status(StatusConvite.USADO).build();
        when(conviteFichaRepository.findById(5L)).thenReturn(java.util.Optional.of(convite));

        assertThrows(IllegalArgumentException.class, () -> conviteFichaService.cancelar(5L));
        verify(conviteFichaRepository, never()).save(any());
    }
}
