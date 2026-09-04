package com.azizaid.hub.integration;

import com.azizaid.hub.dto.request.TipoAcompanhamentoRequestDTO;
import com.azizaid.hub.model.Ficha;
import com.azizaid.hub.repository.FichaRepository;
import com.azizaid.hub.repository.TipoAcompanhamentoRepository;
import com.azizaid.hub.service.FichaService;
import com.azizaid.hub.service.TipoAcompanhamentoService;
import com.azizaid.hub.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static com.azizaid.hub.support.FichaTestFactory.construirDtoValido;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FichaTipoAcompanhamentoIT extends PostgresTestContainerConfig {

    @Autowired
    FichaService fichaService;

    @Autowired
    FichaRepository fichaRepository;

    @Autowired
    TipoAcompanhamentoService tipoAcompanhamentoService;

    @Autowired
    TipoAcompanhamentoRepository tipoAcompanhamentoRepository;

    @Test
    void atribuirTiposAcompanhamento_substituiConjuntoDeTags() {
        Long fichaId = fichaService.criar(construirDtoValido("11223344556")).id();

        Long tipo1Id = tipoAcompanhamentoService.criar(new TipoAcompanhamentoRequestDTO("Jurídico IT")).id();
        Long tipo2Id = tipoAcompanhamentoService.criar(new TipoAcompanhamentoRequestDTO("Psicológico IT")).id();

        fichaService.atribuirTiposAcompanhamento(fichaId, List.of(tipo1Id, tipo2Id));

        Ficha recarregada = fichaRepository.findById(fichaId).orElseThrow();
        assertThat(recarregada.getTiposAcompanhamento()).hasSize(2)
                .extracting("id")
                .containsExactlyInAnyOrder(tipo1Id, tipo2Id);

        fichaService.atribuirTiposAcompanhamento(fichaId, List.of(tipo1Id));

        Ficha substituida = fichaRepository.findById(fichaId).orElseThrow();
        assertThat(substituida.getTiposAcompanhamento()).hasSize(1)
                .extracting("id")
                .containsExactly(tipo1Id);
    }
}
