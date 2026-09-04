package com.azizaid.hub.integration;

import com.azizaid.hub.dto.request.EncaminhamentoRequestDTO;
import com.azizaid.hub.dto.response.FichaResponseDTO;
import com.azizaid.hub.model.Servico;
import com.azizaid.hub.model.enums.StatusFicha;
import com.azizaid.hub.repository.EncaminhamentoRepository;
import com.azizaid.hub.repository.ServicoRepository;
import com.azizaid.hub.service.EncaminhamentoService;
import com.azizaid.hub.service.FichaService;
import com.azizaid.hub.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static com.azizaid.hub.support.EncaminhamentoTestFactory.construirEncaminhamentoDtoValido;
import static com.azizaid.hub.support.FichaTestFactory.construirDtoValido;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class EncaminhamentoRepositoryIT extends PostgresTestContainerConfig {

    @Autowired
    FichaService fichaService;

    @Autowired
    EncaminhamentoService encaminhamentoService;

    @Autowired
    EncaminhamentoRepository encaminhamentoRepository;

    @Autowired
    ServicoRepository servicoRepository;

    @Test
    void criar_comFichaEncerrada_lancaExcecaoENaoPersiste() {
        FichaResponseDTO ficha = fichaService.criar(construirDtoValido("55566677720"));
        fichaService.atualizarStatus(ficha.id(), StatusFicha.ENCERRADO);

        Servico servico = servicoRepository.save(Servico.builder().nome("Serviço Teste").build());
        EncaminhamentoRequestDTO dto = construirEncaminhamentoDtoValido(servico.getId());

        assertThrows(IllegalArgumentException.class,
                () -> encaminhamentoService.criar(ficha.id(), dto));

        assertThat(encaminhamentoRepository.findByFichaIdOrderByDataEncaminhamentoDescIdDesc(ficha.id()))
                .isEmpty();
    }
}
