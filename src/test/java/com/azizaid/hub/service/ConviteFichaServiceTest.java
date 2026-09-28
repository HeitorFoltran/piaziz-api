package com.azizaid.hub.service;

import com.azizaid.hub.config.ConviteTokenService;
import com.azizaid.hub.dto.response.ConviteFichaResponseDTO;
import com.azizaid.hub.model.ConviteFicha;
import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.model.enums.StatusConvite;
import com.azizaid.hub.repository.ConviteFichaRepository;
import com.azizaid.hub.repository.ProfissionalRepository;
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
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConviteFichaServiceTest {

    @Mock
    ConviteFichaRepository conviteFichaRepository;

    @Mock
    ConviteTokenService conviteTokenService;

    @Mock
    ProfissionalRepository profissionalRepository;

    ConviteFichaService conviteFichaService;

    @BeforeEach
    void setUp() {
        conviteFichaService = new ConviteFichaService(conviteFichaRepository, conviteTokenService,
                profissionalRepository, "http://localhost:5173");
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

    private static Profissional profissional(Long id, String nome) {
        return Profissional.builder().id(id).nome(nome).build();
    }

    private static ConviteFicha convite(Long id, Long criadoPorId, StatusConvite status,
                                        LocalDateTime dataExpiracao, String tokenCifrado) {
        return ConviteFicha.builder().id(id).criadoPorId(criadoPorId).status(status)
                .dataExpiracao(dataExpiracao).tokenCifrado(tokenCifrado).build();
    }

    private Map<Long, ConviteFichaResponseDTO> porId(List<ConviteFichaResponseDTO> lista) {
        return lista.stream().collect(Collectors.toMap(ConviteFichaResponseDTO::id, Function.identity()));
    }

    @Test
    void criar_geraConviteEDevolveLinkComToken() {
        autenticarComo(1L, "PADRAO");
        when(conviteTokenService.gerarTokenCru()).thenReturn("token-cru");
        when(conviteTokenService.hash("token-cru")).thenReturn("hash-do-token");
        when(conviteTokenService.cifrar("token-cru")).thenReturn("token-cifrado");
        when(profissionalRepository.findById(1L)).thenReturn(Optional.of(profissional(1L, "Ana Beatriz")));
        when(conviteTokenService.calcularExpiracao()).thenReturn(LocalDateTime.now().plusDays(7));
        when(conviteFichaRepository.save(any(ConviteFicha.class))).thenAnswer(inv -> {
            ConviteFicha c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        ConviteFichaResponseDTO resposta = conviteFichaService.criar();

        assertEquals("http://localhost:5173/ficha-publica/token-cru", resposta.linkCompleto());
        assertEquals(1L, resposta.criadoPorId());
        assertEquals("Ana Beatriz", resposta.criadoPorNome());
        verify(conviteFichaRepository).save(argThat(c -> c.getTokenHash().equals("hash-do-token")
                && "token-cifrado".equals(c.getTokenCifrado())
                && c.getCriadoPorId().equals(1L)));
    }

    @Test
    void listar_devolveConvitesDeVariosProfissionaisComONomeDeCadaUm() {
        autenticarComo(3L, "ESTAGIARIO");
        LocalDateTime noPrazo = LocalDateTime.now().plusDays(3);
        when(conviteFichaRepository.findAllByOrderByDataCriacaoDesc()).thenReturn(List.of(
                convite(10L, 1L, StatusConvite.ATIVO, noPrazo, "cifrado-10"),
                convite(11L, 2L, StatusConvite.ATIVO, noPrazo, "cifrado-11"),
                convite(12L, 1L, StatusConvite.CANCELADO, noPrazo, null)));
        when(profissionalRepository.findAllById(any())).thenReturn(List.of(
                profissional(1L, "Ana Beatriz"), profissional(2L, "Carlos Mendes")));
        when(conviteTokenService.decifrar("cifrado-10", 10L)).thenReturn(Optional.of("token-10"));
        when(conviteTokenService.decifrar("cifrado-11", 11L)).thenReturn(Optional.of("token-11"));

        Map<Long, ConviteFichaResponseDTO> resposta = porId(conviteFichaService.listar());

        assertEquals(3, resposta.size());
        assertEquals("Ana Beatriz", resposta.get(10L).criadoPorNome());
        assertEquals("Carlos Mendes", resposta.get(11L).criadoPorNome());
        assertEquals("Ana Beatriz", resposta.get(12L).criadoPorNome());
        assertEquals(2L, resposta.get(11L).criadoPorId());
        verify(profissionalRepository, times(1)).findAllById(any());
        verify(profissionalRepository, never()).findById(any());
    }

    @Test
    void listar_soDevolveLinkParaConviteAtivoNoPrazo() {
        autenticarComo(1L, "PADRAO");
        LocalDateTime noPrazo = LocalDateTime.now().plusDays(3);
        LocalDateTime vencido = LocalDateTime.now().minusMinutes(1);
        when(conviteFichaRepository.findAllByOrderByDataCriacaoDesc()).thenReturn(List.of(
                convite(1L, 1L, StatusConvite.ATIVO, noPrazo, "cifrado-1"),
                convite(2L, 1L, StatusConvite.ATIVO, vencido, "cifrado-2"),
                convite(3L, 1L, StatusConvite.USADO, noPrazo, "cifrado-3"),
                convite(4L, 1L, StatusConvite.CANCELADO, noPrazo, "cifrado-4"),
                convite(5L, 1L, StatusConvite.ATIVO, noPrazo, null)));
        when(profissionalRepository.findAllById(any())).thenReturn(List.of(profissional(1L, "Ana Beatriz")));
        when(conviteTokenService.decifrar("cifrado-1", 1L)).thenReturn(Optional.of("token-1"));
        when(conviteTokenService.decifrar(null, 5L)).thenReturn(Optional.empty());

        Map<Long, ConviteFichaResponseDTO> resposta = porId(conviteFichaService.listar());

        assertEquals("http://localhost:5173/ficha-publica/token-1", resposta.get(1L).linkCompleto());
        assertNull(resposta.get(2L).linkCompleto());
        assertNull(resposta.get(3L).linkCompleto());
        assertNull(resposta.get(4L).linkCompleto());
        assertNull(resposta.get(5L).linkCompleto());
        verify(conviteTokenService, never()).decifrar(eq("cifrado-2"), any());
        verify(conviteTokenService, never()).decifrar(eq("cifrado-3"), any());
        verify(conviteTokenService, never()).decifrar(eq("cifrado-4"), any());
    }

    @Test
    void cancelar_zeraOTokenCifrado() {
        autenticarComo(1L, "PADRAO");
        ConviteFicha convite = convite(5L, 1L, StatusConvite.ATIVO, LocalDateTime.now().plusDays(1), "cifrado");
        when(conviteFichaRepository.findById(5L)).thenReturn(Optional.of(convite));
        when(conviteFichaRepository.save(any(ConviteFicha.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profissionalRepository.findById(1L)).thenReturn(Optional.of(profissional(1L, "Ana Beatriz")));

        ConviteFichaResponseDTO resposta = conviteFichaService.cancelar(5L);

        verify(conviteFichaRepository).save(argThat(c -> c.getTokenCifrado() == null));
        assertNull(resposta.linkCompleto());
        assertEquals("Ana Beatriz", resposta.criadoPorNome());
    }

    @Test
    void cancelar_porEstagiarioQueNaoCriou_lancaAccessDenied() {
        autenticarComo(3L, "ESTAGIARIO");
        ConviteFicha convite = convite(5L, 1L, StatusConvite.ATIVO, LocalDateTime.now().plusDays(1), "cifrado");
        when(conviteFichaRepository.findById(5L)).thenReturn(Optional.of(convite));

        assertThrows(AccessDeniedException.class, () -> conviteFichaService.cancelar(5L));
        verify(conviteFichaRepository, never()).save(any());
        assertEquals("cifrado", convite.getTokenCifrado());
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
