package com.azizaid.hub.service;

import com.azizaid.hub.dto.request.FichaRequestDTO;
import com.azizaid.hub.model.Ficha;
import com.azizaid.hub.repository.FichaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.azizaid.hub.support.FichaTestFactory.construirDtoValido;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FichaServiceTest {

    @Mock
    FichaRepository fichaRepository;

    @Mock
    EntityAuditService entityAuditService;

    FichaService fichaService;

    @BeforeEach
    void setUp() {
        fichaService = new FichaService(fichaRepository, entityAuditService);
    }

    @Test
    void criar_comCpfNovo_salvaFicha() {
        when(fichaRepository.existsByCpf("12345678900")).thenReturn(false);
        when(fichaRepository.buscarMaiorSequencialCodigo()).thenReturn(0);
        when(fichaRepository.save(any(Ficha.class))).thenAnswer(inv -> inv.getArgument(0));

        FichaRequestDTO dto = construirDtoValido("12345678900");

        fichaService.criar(dto);

        verify(fichaRepository).save(any(Ficha.class));
    }

    @Test
    void criar_comCpfDuplicado_lancaExcecaoENaoSalva() {
        when(fichaRepository.existsByCpf("12345678900")).thenReturn(true);
        FichaRequestDTO dto = construirDtoValido("12345678900");

        assertThrows(IllegalArgumentException.class, () -> fichaService.criar(dto));

        verify(fichaRepository, never()).save(any());
    }
}
