package com.azizaid.hub.service;

import com.azizaid.hub.dto.request.ProfissionalRequestDTO;
import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.model.enums.PapelProfissional;
import com.azizaid.hub.repository.ProfissionalRepository;
import com.azizaid.hub.repository.ServicoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfissionalServiceTest {

    @Mock
    ProfissionalRepository profissionalRepository;

    @Mock
    ServicoRepository servicoRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    ContaAuditLogService contaAuditLogService;

    ProfissionalService profissionalService;

    @BeforeEach
    void setUp() {
        profissionalService = new ProfissionalService(
                profissionalRepository, servicoRepository, passwordEncoder, contaAuditLogService);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void criar_comUsernameCriadoAoMesmoTempoPorOutraRequisicao_retornaMesmoErroDaChecagem() {
        Profissional dev = Profissional.builder().id(1L).role(PapelProfissional.DEV).build();
        when(profissionalRepository.findById(1L)).thenReturn(Optional.of(dev));
        // A checagem passa (a outra requisição ainda não gravou), mas o banco recusa pela constraint UNIQUE.
        when(profissionalRepository.existsByUsername("nova.conta")).thenReturn(false);
        when(profissionalRepository.existsByCpfIn(any())).thenReturn(false);
        when(profissionalRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq"));

        ProfissionalRequestDTO dto = new ProfissionalRequestDTO("Conta Nova", "52998224725", null, null,
                "nova.conta", null, "provisoria-123", "PADRAO", null);

        assertThatThrownBy(() -> profissionalService.criar(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("username ou email já em uso");
        verify(contaAuditLogService, never()).registrar(any(), any(), any(), any());
    }
}
