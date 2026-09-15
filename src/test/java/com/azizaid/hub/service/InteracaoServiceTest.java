package com.azizaid.hub.service;

import com.azizaid.hub.dto.request.InteracaoRequestDTO;
import com.azizaid.hub.exception.RecursoNaoEncontradoException;
import com.azizaid.hub.model.Ficha;
import com.azizaid.hub.model.Interacao;
import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.repository.InteracaoRepository;
import com.azizaid.hub.repository.ProfissionalRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InteracaoServiceTest {

    @Mock
    InteracaoRepository interacaoRepository;

    @Mock
    FichaService fichaService;

    @Mock
    ProfissionalRepository profissionalRepository;

    InteracaoService interacaoService;

    @BeforeEach
    void setUp() {
        interacaoService = new InteracaoService(interacaoRepository, fichaService, profissionalRepository);
    }

    @AfterEach
    void limparContextoSeguranca() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(Long profissionalId) {
        var auth = new UsernamePasswordAuthenticationToken(profissionalId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void criar_comUsuarioAutenticado_usaNomeDoProfissionalComoAutor() {
        Ficha ficha = Ficha.builder().id(1L).build();
        when(fichaService.buscarEntidade(1L)).thenReturn(ficha);

        autenticarComo(7L);
        Profissional profissional = Profissional.builder().id(7L).nome("Ana Beatriz").build();
        when(profissionalRepository.findById(7L)).thenReturn(Optional.of(profissional));
        when(interacaoRepository.save(any(Interacao.class))).thenAnswer(inv -> inv.getArgument(0));

        var resposta = interacaoService.criar(1L, new InteracaoRequestDTO("Texto da interação"));

        assertThat(resposta.autor()).isEqualTo("Ana Beatriz");
    }

    @Test
    void criar_semUsuarioAutenticado_lancaIllegalState() {
        Ficha ficha = Ficha.builder().id(1L).build();
        when(fichaService.buscarEntidade(1L)).thenReturn(ficha);

        assertThrows(IllegalStateException.class,
                () -> interacaoService.criar(1L, new InteracaoRequestDTO("Texto da interação")));

        verify(interacaoRepository, never()).save(any());
    }

    @Test
    void criar_comProfissionalAutenticadoInexistente_lancaRecursoNaoEncontrado() {
        Ficha ficha = Ficha.builder().id(1L).build();
        when(fichaService.buscarEntidade(1L)).thenReturn(ficha);

        autenticarComo(99L);
        when(profissionalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class,
                () -> interacaoService.criar(1L, new InteracaoRequestDTO("Texto da interação")));

        verify(interacaoRepository, never()).save(any());
    }
}
