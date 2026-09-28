package com.azizaid.hub.system;

import com.azizaid.hub.config.JwtService;
import com.azizaid.hub.dto.request.AcolhimentoEquipeRequestDTO;
import com.azizaid.hub.dto.request.FichaRequestDTO;
import com.azizaid.hub.dto.request.AvaliacaoSocioeconomicaRequestDTO;
import com.azizaid.hub.dto.request.HistoricoAtendimentoRequestDTO;
import com.azizaid.hub.dto.response.AlteracaoFichaResponseDTO;
import com.azizaid.hub.dto.response.FichaResponseDTO;
import com.azizaid.hub.model.EntityAuditLog;
import com.azizaid.hub.model.enums.AcaoAlteracao;
import com.azizaid.hub.model.enums.PapelProfissional;
import com.azizaid.hub.model.enums.TipoViolencia;
import com.azizaid.hub.repository.EntityAuditLogRepository;
import com.azizaid.hub.repository.ProfissionalRepository;
import com.azizaid.hub.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static com.azizaid.hub.support.FichaTestFactory.construirDtoValido;
import static com.azizaid.hub.support.ProfissionalTestFactory.persistirComToken;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FichaAlteracaoSystemTest extends PostgresTestContainerConfig {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JwtService jwtService;

    @Autowired
    ProfissionalRepository profissionalRepository;

    @Autowired
    EntityAuditLogRepository entityAuditLogRepository;

    private HttpHeaders headersPara(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private Long criarFicha(String token, String cpf) {
        HttpEntity<FichaRequestDTO> requisicao = new HttpEntity<>(construirDtoValido(cpf), headersPara(token));
        ResponseEntity<FichaResponseDTO> resposta =
                restTemplate.postForEntity("/api/fichas", requisicao, FichaResponseDTO.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resposta.getBody().id();
    }

    private AvaliacaoSocioeconomicaRequestDTO avaliacaoVazia() {
        return avaliacao(null, null);
    }

    private AvaliacaoSocioeconomicaRequestDTO avaliacao(Boolean temRenda, BigDecimal valorRenda) {
        return new AvaliacaoSocioeconomicaRequestDTO(
                temRenda, valorRenda, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }

    private static FichaRequestDTO comTelefone(FichaRequestDTO b, String telefone) {
        return new FichaRequestDTO(b.numeroCaso(), b.nome(), b.cpf(), b.idade(), telefone, b.estadoCivil(),
                b.pessoasDependentes(), b.idadeFilhos(), b.nivelSeguranca(), b.tipoMoradia(),
                b.tipoMoradiaOutraDescricao(), b.qtdMoradores(), b.qtdFilhos(), b.ondeMoramFilhos(),
                b.supervisaoFilhos(), b.vagasNecessarias(), b.necessidadesImediatas(),
                b.necessidadeOutraDescricao(), b.status());
    }

    private void put(String url, Object corpo, String token, Long fichaId) {
        ResponseEntity<String> resposta = restTemplate.exchange(url, HttpMethod.PUT,
                new HttpEntity<>(corpo, headersPara(token)), String.class, fichaId);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private AlteracaoFichaResponseDTO[] listarAlteracoes(String token, Long fichaId) {
        ResponseEntity<AlteracaoFichaResponseDTO[]> resposta = restTemplate.exchange(
                "/api/fichas/{id}/alteracoes", HttpMethod.GET,
                new HttpEntity<>(headersPara(token)), AlteracaoFichaResponseDTO[].class, fichaId);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resposta.getBody();
    }

    @Test
    void get_comEdicaoCrossUserNaFicha_devolveLinhaComNomesCorretos() {
        String tokenA = persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO, "Ana Criadora");
        String tokenB = persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO, "Bruno Editor");

        Long fichaId = criarFicha(tokenA, "66048764707");

        HttpEntity<FichaRequestDTO> atualizacao =
                new HttpEntity<>(comTelefone(construirDtoValido("66048764707"), "45988887777"), headersPara(tokenB));
        restTemplate.exchange("/api/fichas/{id}", HttpMethod.PUT, atualizacao, FichaResponseDTO.class, fichaId);

        ResponseEntity<AlteracaoFichaResponseDTO[]> resposta = restTemplate.exchange(
                "/api/fichas/{id}/alteracoes", HttpMethod.GET,
                new HttpEntity<>(headersPara(tokenA)), AlteracaoFichaResponseDTO[].class, fichaId);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).hasSize(2);
        AlteracaoFichaResponseDTO linha = resposta.getBody()[0];
        assertThat(linha.tipoEntidade()).isEqualTo("Ficha");
        assertThat(linha.acao()).isEqualTo("EDITOU");
        assertThat(linha.editorNome()).isEqualTo("Bruno Editor");
        assertThat(linha.donoNome()).isEqualTo("Ana Criadora");

        AlteracaoFichaResponseDTO criacao = resposta.getBody()[1];
        assertThat(criacao.id()).isNull();
        assertThat(criacao.acao()).isEqualTo("CRIOU");
        assertThat(criacao.editorNome()).isEqualTo("Ana Criadora");
    }

    @Test
    void edicaoPelaPropriaCriadora_gravaEditouSemValorDeCampo() {
        String tokenA = persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO, "Ana Criadora");
        Long fichaId = criarFicha(tokenA, "29141777638");

        put("/api/fichas/{id}", comTelefone(construirDtoValido("29141777638"), "45988887777"), tokenA, fichaId);

        List<EntityAuditLog> linhas = entityAuditLogRepository.findByFichaIdOrderByTimestampDesc(fichaId);
        assertThat(linhas).hasSize(1);
        EntityAuditLog linha = linhas.get(0);
        assertThat(linha.getAcao()).isEqualTo(AcaoAlteracao.EDITOU);
        assertThat(linha.getEditorId()).isEqualTo(linha.getDonoId());
        assertThat(linha.getDetalhe()).isNull();
        assertThat(linha.getResumo()).doesNotContain("45988887777").doesNotContain("11999999999");
    }

    @Test
    void salvarAsQuatroPartesSemMudarNada_naoGravaNada() {
        String tokenA = persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO, "Ana Criadora");
        FichaRequestDTO ficha = construirDtoValido("41823657982");
        Long fichaId = criarFicha(tokenA, "41823657982");
        TipoViolencia[] tipos = TipoViolencia.values();
        AcolhimentoEquipeRequestDTO acolhimento = new AcolhimentoEquipeRequestDTO(null, null, "", Set.of(tipos[0], tipos[1]),
                null, null, true, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, "", null);
        HistoricoAtendimentoRequestDTO historico = new HistoricoAtendimentoRequestDTO(true, "", null, null, null,
                null, null, null, null);

        // Como a tela de Nova Ficha e a de edição fazem: as quatro partes a cada salvamento. A primeira
        // rodada cria as partes junto com o caso (dentro da janela de criação), a segunda não muda nada.
        for (int i = 0; i < 2; i++) {
            put("/api/fichas/{id}", ficha, tokenA, fichaId);
            put("/api/fichas/{id}/avaliacao-socioeconomica", avaliacao(true, new BigDecimal("1500")), tokenA, fichaId);
            put("/api/fichas/{id}/acolhimento-equipe", acolhimento, tokenA, fichaId);
            put("/api/fichas/{id}/historico-atendimento", historico, tokenA, fichaId);
        }

        assertThat(entityAuditLogRepository.findByFichaIdOrderByTimestampDesc(fichaId)).isEmpty();
        assertThat(listarAlteracoes(tokenA, fichaId)).extracting(AlteracaoFichaResponseDTO::acao)
                .containsExactly("CRIOU");
    }

    @Test
    void mudarStatus_gravaMudouStatusComOsRotulos() {
        String tokenA = persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO, "Ana Criadora");
        Long fichaId = criarFicha(tokenA, "73519482673");

        ResponseEntity<String> resposta = restTemplate.exchange("/api/fichas/{id}/status?status=ARQUIVADO",
                HttpMethod.PATCH, new HttpEntity<>(headersPara(tokenA)), String.class, fichaId);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<EntityAuditLog> linhas = entityAuditLogRepository.findByFichaIdOrderByTimestampDesc(fichaId);
        assertThat(linhas).hasSize(1);
        assertThat(linhas.get(0).getAcao()).isEqualTo(AcaoAlteracao.MUDOU_STATUS);
        assertThat(linhas.get(0).getTipoEntidade()).isEqualTo("StatusFicha");
        assertThat(linhas.get(0).getDetalhe()).isEqualTo("Ativo -> Arquivado");

        AlteracaoFichaResponseDTO[] historico = listarAlteracoes(tokenA, fichaId);
        assertThat(historico).extracting(AlteracaoFichaResponseDTO::acao).containsExactly("MUDOU_STATUS", "CRIOU");
        assertThat(historico[0].detalhe()).isEqualTo("Ativo -> Arquivado");
    }

    @Test
    void get_comEdicaoCrossUserEmSubRecurso_apareceNaFichaComTipoEntidadeCorreto() {
        String tokenA = persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO, "Ana Criadora");
        String tokenB = persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO, "Bruno Editor");

        Long fichaId = criarFicha(tokenA, "01152449303");

        HttpEntity<AvaliacaoSocioeconomicaRequestDTO> criarAvaliacao =
                new HttpEntity<>(avaliacaoVazia(), headersPara(tokenA));
        restTemplate.exchange("/api/fichas/{id}/avaliacao-socioeconomica", HttpMethod.PUT, criarAvaliacao,
                Object.class, fichaId);

        HttpEntity<AvaliacaoSocioeconomicaRequestDTO> editarAvaliacao =
                new HttpEntity<>(avaliacao(true, null), headersPara(tokenB));
        restTemplate.exchange("/api/fichas/{id}/avaliacao-socioeconomica", HttpMethod.PUT, editarAvaliacao,
                Object.class, fichaId);

        ResponseEntity<AlteracaoFichaResponseDTO[]> resposta = restTemplate.exchange(
                "/api/fichas/{id}/alteracoes", HttpMethod.GET,
                new HttpEntity<>(headersPara(tokenA)), AlteracaoFichaResponseDTO[].class, fichaId);

        assertThat(resposta.getBody()).extracting(AlteracaoFichaResponseDTO::acao).containsExactly("EDITOU", "CRIOU");
        assertThat(resposta.getBody()[0].tipoEntidade()).isEqualTo("AvaliacaoSocioeconomica");
        assertThat(resposta.getBody()[0].editorNome()).isEqualTo("Bruno Editor");
    }

    @Test
    void get_comEstagiario_devolve403() {
        String tokenA = persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO, "Ana Criadora");
        String tokenEstagiario = persistirComToken(profissionalRepository, jwtService, PapelProfissional.ESTAGIARIO, "Estagiario Teste");

        Long fichaId = criarFicha(tokenA, "39825979194");

        ResponseEntity<String> resposta = restTemplate.exchange(
                "/api/fichas/{id}/alteracoes", HttpMethod.GET,
                new HttpEntity<>(headersPara(tokenEstagiario)), String.class, fichaId);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void get_comFichaInexistente_devolve404() {
        String tokenA = persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO, "Ana Criadora");

        ResponseEntity<String> resposta = restTemplate.exchange(
                "/api/fichas/{id}/alteracoes", HttpMethod.GET,
                new HttpEntity<>(headersPara(tokenA)), String.class, 999999L);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
