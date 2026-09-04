package com.azizaid.hub.integration;

import com.azizaid.hub.dto.request.TipoAcompanhamentoRequestDTO;
import com.azizaid.hub.repository.TipoAcompanhamentoRepository;
import com.azizaid.hub.service.TipoAcompanhamentoService;
import com.azizaid.hub.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class TipoAcompanhamentoRepositoryIT extends PostgresTestContainerConfig {

    @Autowired
    TipoAcompanhamentoService tipoAcompanhamentoService;

    @Autowired
    TipoAcompanhamentoRepository tipoAcompanhamentoRepository;

    @Test
    void criar_comNomeDuplicadoVariandoCase_segundaFalhaEPrimeiraPersistida() {
        long totalAntes = tipoAcompanhamentoRepository.count();

        tipoAcompanhamentoService.criar(new TipoAcompanhamentoRequestDTO("jurídico"));

        assertThrows(IllegalArgumentException.class,
                () -> tipoAcompanhamentoService.criar(new TipoAcompanhamentoRequestDTO("JURÍDICO")));

        assertThat(tipoAcompanhamentoRepository.count()).isEqualTo(totalAntes + 1);
        assertThat(tipoAcompanhamentoRepository.existsByNomeIgnoreCase("jurídico")).isTrue();
    }
}
