package com.azizaid.hub.service;

import com.azizaid.hub.dto.response.FichaPendenteResponseDTO;
import com.azizaid.hub.dto.response.FichaResponseDTO;
import com.azizaid.hub.exception.RecursoNaoEncontradoException;
import com.azizaid.hub.model.Ficha;
import com.azizaid.hub.model.FichaPendente;
import com.azizaid.hub.model.enums.StatusFichaPendente;
import com.azizaid.hub.repository.FichaPendenteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FichaPendenteServiceTest {

    @Mock
    FichaPendenteRepository fichaPendenteRepository;

    @Mock
    FichaService fichaService;

    FichaPendenteService fichaPendenteService;

    @BeforeEach
    void setUp() {
        fichaPendenteService = new FichaPendenteService(fichaPendenteRepository, fichaService);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_PADRAO"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(9L, null, authorities));
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void aprovar_pendentePendente_criaFichaEMarcaAprovada() {
        FichaPendente pendente = FichaPendente.builder().id(1L).nome("Maria").cpf("12345678909")
                .status(StatusFichaPendente.PENDENTE).build();
        when(fichaPendenteRepository.findById(1L)).thenReturn(Optional.of(pendente));
        Ficha fichaCriada = Ficha.builder().id(42L).build();
        when(fichaService.criar(any())).thenReturn(FichaResponseDTO.from(fichaCriada));
        when(fichaPendenteRepository.save(any(FichaPendente.class))).thenAnswer(inv -> inv.getArgument(0));

        FichaPendenteResponseDTO resposta = fichaPendenteService.aprovar(1L);

        assertEquals(StatusFichaPendente.APROVADA.name(), resposta.status());
        assertEquals(42L, resposta.fichaId());
        assertEquals(9L, resposta.revisadoPorId());
    }

    @Test
    void aprovar_jaRevisada_lancaExcecao() {
        FichaPendente pendente = FichaPendente.builder().id(1L).status(StatusFichaPendente.APROVADA).build();
        when(fichaPendenteRepository.findById(1L)).thenReturn(Optional.of(pendente));

        assertThrows(IllegalArgumentException.class, () -> fichaPendenteService.aprovar(1L));
        verify(fichaService, never()).criar(any());
    }

    @Test
    void aprovar_idInexistente_lanca404() {
        when(fichaPendenteRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> fichaPendenteService.aprovar(1L));
    }

    @Test
    void rejeitar_pendentePendente_marcaRejeitadaComMotivo() {
        FichaPendente pendente = FichaPendente.builder().id(1L).status(StatusFichaPendente.PENDENTE).build();
        when(fichaPendenteRepository.findById(1L)).thenReturn(Optional.of(pendente));
        when(fichaPendenteRepository.save(any(FichaPendente.class))).thenAnswer(inv -> inv.getArgument(0));

        FichaPendenteResponseDTO resposta = fichaPendenteService.rejeitar(1L, "CPF inválido");

        assertEquals(StatusFichaPendente.REJEITADA.name(), resposta.status());
        assertEquals("CPF inválido", resposta.motivoRejeicao());
    }
}
