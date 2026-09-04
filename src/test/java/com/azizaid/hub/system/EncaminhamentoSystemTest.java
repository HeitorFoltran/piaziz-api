package com.azizaid.hub.system;

import com.azizaid.hub.config.JwtService;
import com.azizaid.hub.dto.request.EncaminhamentoRequestDTO;
import com.azizaid.hub.dto.request.FichaRequestDTO;
import com.azizaid.hub.dto.response.FichaResponseDTO;
import com.azizaid.hub.model.Servico;
import com.azizaid.hub.model.enums.PapelProfissional;
import com.azizaid.hub.repository.ServicoRepository;
import com.azizaid.hub.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static com.azizaid.hub.support.EncaminhamentoTestFactory.construirEncaminhamentoDtoValido;
import static com.azizaid.hub.support.FichaTestFactory.construirDtoValido;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EncaminhamentoSystemTest extends PostgresTestContainerConfig {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JwtService jwtService;

    @Autowired
    ServicoRepository servicoRepository;

    @Test
    void postEncaminhamento_comFichaEncerrada_retorna400() {
        String token = jwtService.gerarToken(1L, "teste@azizaidhub.local", PapelProfissional.PADRAO);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        FichaRequestDTO fichaDto = construirDtoValido("99988877714");
        HttpEntity<FichaRequestDTO> criarFichaRequisicao = new HttpEntity<>(fichaDto, headers);
        ResponseEntity<FichaResponseDTO> fichaCriada =
                restTemplate.postForEntity("/api/fichas", criarFichaRequisicao, FichaResponseDTO.class);
        assertThat(fichaCriada.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long fichaId = fichaCriada.getBody().id();

        HttpEntity<Void> atualizarStatusRequisicao = new HttpEntity<>(headers);
        ResponseEntity<FichaResponseDTO> statusAtualizado = restTemplate.exchange(
                "/api/fichas/{id}/status?status=ENCERRADO",
                org.springframework.http.HttpMethod.PATCH,
                atualizarStatusRequisicao,
                FichaResponseDTO.class,
                fichaId);
        assertThat(statusAtualizado.getStatusCode()).isEqualTo(HttpStatus.OK);

        Servico servico = servicoRepository.save(Servico.builder().nome("Serviço Teste").build());
        EncaminhamentoRequestDTO encaminhamentoDto = construirEncaminhamentoDtoValido(servico.getId());
        HttpEntity<EncaminhamentoRequestDTO> criarEncaminhamentoRequisicao =
                new HttpEntity<>(encaminhamentoDto, headers);

        ResponseEntity<String> resposta = restTemplate.postForEntity(
                "/api/fichas/{id}/encaminhamentos", criarEncaminhamentoRequisicao, String.class, fichaId);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).contains("encerrado");
    }
}
