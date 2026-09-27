package com.azizaid.hub.system;

import com.azizaid.hub.config.JwtService;
import com.azizaid.hub.dto.request.ProfissionalEdicaoRequestDTO;
import com.azizaid.hub.dto.request.ProfissionalRequestDTO;
import com.azizaid.hub.dto.request.ResetarSenhaRequestDTO;
import com.azizaid.hub.dto.response.ContaHistoricoResponseDTO;
import com.azizaid.hub.dto.response.ProfissionalResponseDTO;
import com.azizaid.hub.model.ContaAuditLog;
import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.model.enums.AcaoConta;
import com.azizaid.hub.model.enums.PapelProfissional;
import com.azizaid.hub.repository.ContaAuditLogRepository;
import com.azizaid.hub.repository.ProfissionalRepository;
import com.azizaid.hub.support.PostgresTestContainerConfig;
import com.azizaid.hub.support.ProfissionalTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

// Gestão de contas em /api/profissionais: matriz de acesso (gerenciador = DEV, ou PADRAO com a flag),
// regras sobre conta DEV e a própria conta, validações e trilha conta_audit_log.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProfissionalSystemTest extends PostgresTestContainerConfig {

    private static final String SENHA_PROVISORIA = "provisoria-123";

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JwtService jwtService;

    @Autowired
    ProfissionalRepository profissionalRepository;

    @Autowired
    ContaAuditLogRepository contaAuditLogRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    // --- apoio ---

    static String cpfValidoAleatorio() {
        int[] d = new int[11];
        for (int i = 0; i < 9; i++) {
            d[i] = ThreadLocalRandom.current().nextInt(10);
        }
        d[9] = digitoVerificador(d, 9);
        d[10] = digitoVerificador(d, 10);
        StringBuilder sb = new StringBuilder();
        for (int x : d) {
            sb.append(x);
        }
        return sb.toString();
    }

    private static int digitoVerificador(int[] d, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += d[i] * (tamanho + 1 - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private Profissional persistir(PapelProfissional role, boolean podeGerenciar) {
        return profissionalRepository.save(Profissional.builder()
                .nome("Profissional Gestão")
                .cpf("12345678900")
                .username(ProfissionalTestFactory.usernameUnico())
                .email("gestao." + UUID.randomUUID() + "@azizaidhub.local")
                .senhaHash(passwordEncoder.encode("senha-original-1"))
                .role(role)
                .podeGerenciarProfissionais(podeGerenciar)
                .build());
    }

    private String token(Profissional profissional) {
        return jwtService.gerarToken(profissional.getId(), profissional.getUsername(), profissional.getRole());
    }

    private <T> ResponseEntity<String> chamar(HttpMethod metodo, String url, String token, T corpo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(url, metodo, new HttpEntity<>(corpo, headers), String.class);
    }

    private ProfissionalRequestDTO novaConta(PapelProfissional role, Boolean podeGerenciar) {
        return new ProfissionalRequestDTO("Conta Nova", cpfValidoAleatorio(), null, null,
                ProfissionalTestFactory.usernameUnico(), null, SENHA_PROVISORIA, role.name(), podeGerenciar);
    }

    private ProfissionalEdicaoRequestDTO edicaoDe(Profissional p, PapelProfissional role, boolean podeGerenciar,
                                                  boolean ativo) {
        return new ProfissionalEdicaoRequestDTO(p.getNome(), p.getCpf(), p.getCarteiraProfissional(), null,
                p.getUsername(), p.getEmail(), role.name(), podeGerenciar, ativo);
    }

    private ProfissionalResponseDTO criarComo(Profissional autor, ProfissionalRequestDTO dto) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token(autor));
        ResponseEntity<ProfissionalResponseDTO> resposta = restTemplate.postForEntity(
                "/api/profissionais", new HttpEntity<>(dto, headers), ProfissionalResponseDTO.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resposta.getBody();
    }

    // --- matriz de acesso ---

    @Test
    void padraoSemFlagEEstagiario_recebem403EmTodasAsRotas() {
        Profissional alvo = persistir(PapelProfissional.PADRAO, false);
        for (PapelProfissional role : List.of(PapelProfissional.PADRAO, PapelProfissional.ESTAGIARIO)) {
            String token = token(persistir(role, false));

            assertThat(chamar(HttpMethod.GET, "/api/profissionais", token, null).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(chamar(HttpMethod.GET, "/api/profissionais/" + alvo.getId(), token, null).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(chamar(HttpMethod.POST, "/api/profissionais", token,
                    novaConta(PapelProfissional.PADRAO, false)).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(chamar(HttpMethod.PUT, "/api/profissionais/" + alvo.getId(), token,
                    edicaoDe(alvo, PapelProfissional.PADRAO, false, true)).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(chamar(HttpMethod.POST, "/api/profissionais/" + alvo.getId() + "/resetar-senha", token,
                    new ResetarSenhaRequestDTO(SENHA_PROVISORIA)).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(chamar(HttpMethod.GET, "/api/profissionais/" + alvo.getId() + "/historico", token, null)
                    .getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Test
    void padraoComFlag_criaPadraoEEstagiario_comTrocaObrigatoria() {
        Profissional gerenciador = persistir(PapelProfissional.PADRAO, true);

        ProfissionalResponseDTO padrao = criarComo(gerenciador, novaConta(PapelProfissional.PADRAO, false));
        ProfissionalResponseDTO estagiario = criarComo(gerenciador, novaConta(PapelProfissional.ESTAGIARIO, null));

        assertThat(padrao.role()).isEqualTo("PADRAO");
        assertThat(padrao.deveTrocarSenha()).isTrue();
        assertThat(padrao.cpf()).matches("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}");
        assertThat(estagiario.role()).isEqualTo("ESTAGIARIO");
        assertThat(estagiario.podeGerenciarProfissionais()).isFalse();
    }

    @Test
    void padraoComFlag_naoConcedeFlag_nemMexeEmContaDev() {
        Profissional gerenciador = persistir(PapelProfissional.PADRAO, true);
        Profissional dev = persistir(PapelProfissional.DEV, false);
        Profissional comum = persistir(PapelProfissional.PADRAO, false);
        String token = token(gerenciador);

        assertThat(chamar(HttpMethod.POST, "/api/profissionais", token,
                novaConta(PapelProfissional.PADRAO, true)).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(chamar(HttpMethod.PUT, "/api/profissionais/" + comum.getId(), token,
                edicaoDe(comum, PapelProfissional.PADRAO, true, true)).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(chamar(HttpMethod.PUT, "/api/profissionais/" + dev.getId(), token,
                edicaoDe(dev, PapelProfissional.DEV, false, true)).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(chamar(HttpMethod.POST, "/api/profissionais/" + dev.getId() + "/resetar-senha", token,
                new ResetarSenhaRequestDTO(SENHA_PROVISORIA)).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void gerenciadorNaoDev_naoMexeEmOutroGerenciador_devMexe() {
        Profissional gerenciador = persistir(PapelProfissional.PADRAO, true);
        Profissional colega = persistir(PapelProfissional.PADRAO, true);
        Profissional dev = persistir(PapelProfissional.DEV, false);
        String token = token(gerenciador);
        String url = "/api/profissionais/" + colega.getId();

        ProfissionalEdicaoRequestDTO soNome = new ProfissionalEdicaoRequestDTO("Nome Trocado", colega.getCpf(),
                null, null, colega.getUsername(), colega.getEmail(), "PADRAO", true, true);
        assertThat(chamar(HttpMethod.PUT, url, token, soNome).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(chamar(HttpMethod.PUT, url, token,
                edicaoDe(colega, PapelProfissional.PADRAO, true, false)).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(chamar(HttpMethod.POST, url + "/resetar-senha", token,
                new ResetarSenhaRequestDTO(SENHA_PROVISORIA)).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(chamar(HttpMethod.POST, url + "/resetar-senha", token(dev),
                new ResetarSenhaRequestDTO(SENHA_PROVISORIA)).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(chamar(HttpMethod.PUT, url, token(dev),
                edicaoDe(colega, PapelProfissional.PADRAO, true, false)).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void dev_concedeERetiraFlag_eRetiradaValeNaRequisicaoSeguinteComMesmoToken() {
        Profissional dev = persistir(PapelProfissional.DEV, false);
        Profissional alvo = persistir(PapelProfissional.PADRAO, false);
        String tokenDev = token(dev);
        String tokenAlvo = token(alvo);

        assertThat(chamar(HttpMethod.GET, "/api/profissionais", tokenAlvo, null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(chamar(HttpMethod.PUT, "/api/profissionais/" + alvo.getId(), tokenDev,
                edicaoDe(alvo, PapelProfissional.PADRAO, true, true)).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(chamar(HttpMethod.GET, "/api/profissionais", tokenAlvo, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(chamar(HttpMethod.PUT, "/api/profissionais/" + alvo.getId(), tokenDev,
                edicaoDe(alvo, PapelProfissional.PADRAO, false, true)).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(chamar(HttpMethod.GET, "/api/profissionais", tokenAlvo, null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void flagSoValeParaPadrao_eRoleDevNaoEntraNemSaiPelaRota() {
        Profissional dev = persistir(PapelProfissional.DEV, false);
        Profissional estagiario = persistir(PapelProfissional.ESTAGIARIO, false);
        Profissional outroDev = persistir(PapelProfissional.DEV, false);
        String token = token(dev);

        assertThat(chamar(HttpMethod.POST, "/api/profissionais", token,
                novaConta(PapelProfissional.ESTAGIARIO, true)).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(chamar(HttpMethod.POST, "/api/profissionais", token,
                novaConta(PapelProfissional.DEV, false)).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(chamar(HttpMethod.PUT, "/api/profissionais/" + estagiario.getId(), token,
                edicaoDe(estagiario, PapelProfissional.ESTAGIARIO, true, true)).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(chamar(HttpMethod.PUT, "/api/profissionais/" + estagiario.getId(), token,
                edicaoDe(estagiario, PapelProfissional.DEV, false, true)).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(chamar(HttpMethod.PUT, "/api/profissionais/" + outroDev.getId(), token,
                edicaoDe(outroDev, PapelProfissional.PADRAO, false, true)).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void naoEditaPropriaRoleAtivoOuFlag_nemResetaPropriaSenha() {
        Profissional gerenciador = persistir(PapelProfissional.PADRAO, true);
        String token = token(gerenciador);
        String url = "/api/profissionais/" + gerenciador.getId();

        assertThat(chamar(HttpMethod.PUT, url, token,
                edicaoDe(gerenciador, PapelProfissional.ESTAGIARIO, false, true)).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(chamar(HttpMethod.PUT, url, token,
                edicaoDe(gerenciador, PapelProfissional.PADRAO, true, false)).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(chamar(HttpMethod.POST, url + "/resetar-senha", token,
                new ResetarSenhaRequestDTO(SENHA_PROVISORIA)).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // O resto do próprio cadastro continua editável.
        ProfissionalEdicaoRequestDTO soNome = new ProfissionalEdicaoRequestDTO("Nome Novo", gerenciador.getCpf(),
                null, null, gerenciador.getUsername(), gerenciador.getEmail(), "PADRAO", true, true);
        assertThat(chamar(HttpMethod.PUT, url, token, soNome).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void listagemMascaraCpf_eDetalheTrazCpfCompleto() {
        Profissional dev = persistir(PapelProfissional.DEV, false);
        ProfissionalResponseDTO criado = criarComo(dev, novaConta(PapelProfissional.PADRAO, false));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token(dev));

        ResponseEntity<ProfissionalResponseDTO[]> lista = restTemplate.exchange(
                "/api/profissionais?q=" + criado.username(), HttpMethod.GET, new HttpEntity<>(headers),
                ProfissionalResponseDTO[].class);
        ResponseEntity<ProfissionalResponseDTO> detalhe = restTemplate.exchange(
                "/api/profissionais/" + criado.id(), HttpMethod.GET, new HttpEntity<>(headers),
                ProfissionalResponseDTO.class);

        assertThat(lista.getBody()).hasSize(1);
        assertThat(lista.getBody()[0].cpf()).startsWith("***.***.***-");
        assertThat(detalhe.getBody().cpf()).isEqualTo(criado.cpf());
        assertThat(chamar(HttpMethod.GET, "/api/profissionais/" + criado.id(), token(dev), null).getBody())
                .doesNotContain("senhaHash").doesNotContain("sessoesRevogadasEm");
    }

    // --- derrubar sessão ---

    @Test
    void resetarSenha_derrubaTokenExistente_eObrigaTroca() throws InterruptedException {
        Profissional dev = persistir(PapelProfissional.DEV, false);
        Profissional alvo = persistir(PapelProfissional.PADRAO, false);
        String tokenAlvo = token(alvo);
        assertThat(chamar(HttpMethod.GET, "/api/fichas", tokenAlvo, null).getStatusCode()).isEqualTo(HttpStatus.OK);

        Thread.sleep(1100);
        assertThat(chamar(HttpMethod.POST, "/api/profissionais/" + alvo.getId() + "/resetar-senha", token(dev),
                new ResetarSenhaRequestDTO(SENHA_PROVISORIA)).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(chamar(HttpMethod.GET, "/api/fichas", tokenAlvo, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        Profissional recarregado = profissionalRepository.findById(alvo.getId()).orElseThrow();
        assertThat(recarregado.isDeveTrocarSenha()).isTrue();
        assertThat(passwordEncoder.matches(SENHA_PROVISORIA, recarregado.getSenhaHash())).isTrue();
    }

    @Test
    void desativar_derrubaTokenExistente() {
        Profissional dev = persistir(PapelProfissional.DEV, false);
        Profissional alvo = persistir(PapelProfissional.PADRAO, false);
        String tokenAlvo = token(alvo);
        assertThat(chamar(HttpMethod.GET, "/api/fichas", tokenAlvo, null).getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(chamar(HttpMethod.PUT, "/api/profissionais/" + alvo.getId(), token(dev),
                edicaoDe(alvo, PapelProfissional.PADRAO, false, false)).getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(chamar(HttpMethod.GET, "/api/fichas", tokenAlvo, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(profissionalRepository.findById(alvo.getId()).orElseThrow().getSessoesRevogadasEm()).isNotNull();
    }

    // --- validações ---

    @Test
    void validacoes_retornam400() {
        Profissional dev = persistir(PapelProfissional.DEV, false);
        String token = token(dev);
        ProfissionalResponseDTO existente = criarComo(dev, novaConta(PapelProfissional.PADRAO, false));
        ProfissionalRequestDTO base = novaConta(PapelProfissional.PADRAO, false);

        List<ProfissionalRequestDTO> invalidos = List.of(
                comCpf(base, "123.456.789-00"),
                comCpf(base, existente.cpf().replaceAll("\\D", "")),
                comUsername(base, existente.username().toUpperCase()),
                comUsername(base, "ab"),
                comUsername(base, "com espaco"),
                comUsername(base, "a".repeat(31)),
                comSenha(base, "curta12"),
                comSenha(base, "a".repeat(73)),
                // 72 caracteres, mas 144 bytes: o BCrypt não aceita.
                comSenha(base, "é".repeat(72)));

        for (ProfissionalRequestDTO dto : invalidos) {
            assertThat(chamar(HttpMethod.POST, "/api/profissionais", token, dto).getStatusCode())
                    .as("%s", dto)
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
        assertThat(chamar(HttpMethod.POST, "/api/profissionais", token, base).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void editar_contaAntigaComCpfInvalido_salvaSemMexerNoCpf() {
        Profissional dev = persistir(PapelProfissional.DEV, false);
        Profissional antiga = persistir(PapelProfissional.PADRAO, false);

        ProfissionalEdicaoRequestDTO dto = new ProfissionalEdicaoRequestDTO("Nome Corrigido", antiga.getCpf(), null,
                null, antiga.getUsername(), antiga.getEmail(), "PADRAO", false, true);

        assertThat(chamar(HttpMethod.PUT, "/api/profissionais/" + antiga.getId(), token(dev), dto).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    private static ProfissionalRequestDTO comCpf(ProfissionalRequestDTO b, String cpf) {
        return new ProfissionalRequestDTO(b.nome(), cpf, null, null, b.username(), b.email(), b.senhaProvisoria(),
                b.role(), b.podeGerenciarProfissionais());
    }

    private static ProfissionalRequestDTO comUsername(ProfissionalRequestDTO b, String username) {
        return new ProfissionalRequestDTO(b.nome(), b.cpf(), null, null, username, b.email(), b.senhaProvisoria(),
                b.role(), b.podeGerenciarProfissionais());
    }

    private static ProfissionalRequestDTO comSenha(ProfissionalRequestDTO b, String senha) {
        return new ProfissionalRequestDTO(b.nome(), b.cpf(), null, null, b.username(), b.email(), senha,
                b.role(), b.podeGerenciarProfissionais());
    }

    // --- auditoria ---

    @Test
    void contaAuditLog_registraCriarEditarResetar_semSenha() {
        Profissional dev = persistir(PapelProfissional.DEV, false);
        ProfissionalResponseDTO criado = criarComo(dev, novaConta(PapelProfissional.PADRAO, false));
        Profissional alvo = profissionalRepository.findById(criado.id()).orElseThrow();
        String token = token(dev);
        String novoUsername = ProfissionalTestFactory.usernameUnico();

        ProfissionalEdicaoRequestDTO edicao = new ProfissionalEdicaoRequestDTO(alvo.getNome(), alvo.getCpf(), null,
                null, novoUsername, "novo." + UUID.randomUUID() + "@azizaidhub.local", "ESTAGIARIO", false, true);
        assertThat(chamar(HttpMethod.PUT, "/api/profissionais/" + alvo.getId(), token, edicao).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(chamar(HttpMethod.POST, "/api/profissionais/" + alvo.getId() + "/resetar-senha", token,
                new ResetarSenhaRequestDTO("outra-provisoria-1")).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        List<ContaAuditLog> linhas = contaAuditLogRepository.findByProfissionalIdOrderByTimestampAsc(alvo.getId());
        assertThat(linhas).extracting(ContaAuditLog::getAcao)
                .containsExactly(AcaoConta.CRIAR, AcaoConta.EDITAR, AcaoConta.RESETAR_SENHA);
        assertThat(linhas).allMatch(l -> l.getAutorId().equals(dev.getId()));
        assertThat(linhas.get(1).getDetalhe())
                .contains("username (" + alvo.getUsername() + " -> " + novoUsername + ")")
                .contains("email")
                .contains("role");
        assertThat(linhas).allSatisfy(l -> assertThat(String.valueOf(l.getDetalhe()))
                .doesNotContain(SENHA_PROVISORIA)
                .doesNotContain("outra-provisoria-1")
                .doesNotContain("$2a$"));
    }

    // --- histórico (GET /{id}/historico) ---

    @Test
    void historico_criarEditarResetar_devolveMaisNovoPrimeiroComAutorNome() {
        Profissional gerenciador = persistir(PapelProfissional.PADRAO, true);
        ProfissionalResponseDTO criado = criarComo(gerenciador, novaConta(PapelProfissional.PADRAO, false));
        Profissional alvo = profissionalRepository.findById(criado.id()).orElseThrow();
        String token = token(gerenciador);

        assertThat(chamar(HttpMethod.PUT, "/api/profissionais/" + alvo.getId(), token,
                edicaoDe(alvo, PapelProfissional.ESTAGIARIO, false, true)).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(chamar(HttpMethod.POST, "/api/profissionais/" + alvo.getId() + "/resetar-senha", token,
                new ResetarSenhaRequestDTO("outra-provisoria-1")).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<ContaHistoricoResponseDTO[]> resposta = restTemplate.exchange(
                "/api/profissionais/" + alvo.getId() + "/historico", HttpMethod.GET, new HttpEntity<>(headers),
                ContaHistoricoResponseDTO[].class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).extracting(ContaHistoricoResponseDTO::acao)
                .containsExactly(AcaoConta.RESETAR_SENHA, AcaoConta.EDITAR, AcaoConta.CRIAR);
        assertThat(resposta.getBody()).allSatisfy(l -> {
            assertThat(l.autorId()).isEqualTo(gerenciador.getId());
            assertThat(l.autorNome()).isEqualTo(gerenciador.getNome());
            assertThat(l.timestamp()).isNotNull();
        });
        assertThat(resposta.getBody()[1].detalhe()).contains("role");
    }

    @Test
    void historico_idInexistente_retorna404() {
        String token = token(persistir(PapelProfissional.DEV, false));

        assertThat(chamar(HttpMethod.GET, "/api/profissionais/999999999/historico", token, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
