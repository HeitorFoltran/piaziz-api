package com.azizaid.hub.integration;

import com.azizaid.hub.dto.request.FichaRequestDTO;
import com.azizaid.hub.repository.FichaRepository;
import com.azizaid.hub.service.FichaService;
import com.azizaid.hub.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static com.azizaid.hub.support.FichaTestFactory.construirDtoValido;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class FichaRepositoryIT extends PostgresTestContainerConfig {

    @Autowired
    FichaService fichaService;

    @Autowired
    FichaRepository fichaRepository;

    @Test
    void criar_duasFichasMesmoCpf_segundaFalhaEPrimeiraPersistida() {
        long totalAntes = fichaRepository.count();

        fichaService.criar(construirDtoValido("98765432100"));

        assertThrows(IllegalArgumentException.class,
                () -> fichaService.criar(construirDtoValido("98765432100")));

        assertThat(fichaRepository.count()).isEqualTo(totalAntes + 1);
        assertThat(fichaRepository.existsByCpf("98765432100")).isTrue();
    }
}
