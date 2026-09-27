package com.azizaid.hub.system;

import com.azizaid.hub.config.JwtService;
import com.azizaid.hub.dto.request.LoginRequestDTO;
import com.azizaid.hub.dto.request.TrocarSenhaRequestDTO;
import com.azizaid.hub.dto.response.LoginResponseDTO;
import com.azizaid.hub.dto.response.UsuarioAtualResponseDTO;
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

import static org.assertj.core.api.Assertions.assertThat;

// GET /api/auth/me, PUT /api/auth/senha e a barreira de troca obrigatória no JwtAuthFilter.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthSenhaSystemTest extends PostgresTestContainerConfig {

    private static final String SENHA = "provisoria-123";
    private static final String NOVA_SENHA = "definitiva-456";

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

    private Profissional persistir(PapelProfissional role, boolean deveTrocarSenha, boolean podeGerenciar) {
        return profissionalRepository.save(Profissional.builder()
                .nome("Profissional Senha")
                .cpf("12345678900")
                .username(ProfissionalTestFactory.usernameUnico())
                .senhaHash(passwordEncoder.encode(SENHA))
                .role(role)
                .deveTrocarSenha(deveTrocarSenha)
                .podeGerenciarProfissionais(podeGerenciar)
                .build());
    }

    private String token(Profissional profissional) {
        return jwtService.gerarToken(profissional.getId(), profissional.getUsername(), profissional.getRole());
    }

    private <T, R> ResponseEntity<R> chamar(HttpMethod metodo, String url, String token, T corpo, Class<R> tipo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(url, metodo, new HttpEntity<>(corpo, headers), tipo);
    }

    @Test
    void me_devolveDadosDoBanco_comPermissaoCalculada() {
        Profissional gerenciador = persistir(PapelProfissional.PADRAO, false, true);
        Profissional dev = persistir(PapelProfissional.DEV, false, false);
        Profissional comum = persistir(PapelProfissional.PADRAO, false, false);

        UsuarioAtualResponseDTO meGerenciador =
                chamar(HttpMethod.GET, "/api/auth/me", token(gerenciador), null, UsuarioAtualResponseDTO.class).getBody();
        UsuarioAtualResponseDTO meDev =
                chamar(HttpMethod.GET, "/api/auth/me", token(dev), null, UsuarioAtualResponseDTO.class).getBody();
        UsuarioAtualResponseDTO meComum =
                chamar(HttpMethod.GET, "/api/auth/me", token(comum), null, UsuarioAtualResponseDTO.class).getBody();

        assertThat(meGerenciador.id()).isEqualTo(gerenciador.getId());
        assertThat(meGerenciador.username()).isEqualTo(gerenciador.getUsername());
        assertThat(meGerenciador.email()).isNull();
        assertThat(meGerenciador.podeGerenciarProfissionais()).isTrue();
        assertThat(meDev.podeGerenciarProfissionais()).isTrue();
        assertThat(meComum.podeGerenciarProfissionais()).isFalse();
    }

    @Test
    void contaComTrocaObrigatoria_soAcessaMeESenha_eDepoisDaTrocaTokenNovoFuncionaEAntigoNao()
            throws InterruptedException {
        Profissional profissional = persistir(PapelProfissional.PADRAO, true, false);

        ResponseEntity<LoginResponseDTO> login = restTemplate.postForEntity("/api/auth/login",
                new LoginRequestDTO(profissional.getUsername(), SENHA), LoginResponseDTO.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login.getBody().deveTrocarSenha()).isTrue();
        String tokenAntigo = login.getBody().token();

        ResponseEntity<String> fichas = chamar(HttpMethod.GET, "/api/fichas", tokenAntigo, null, String.class);
        assertThat(fichas.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(fichas.getBody()).contains("Troca de senha obrigatória");
        assertThat(chamar(HttpMethod.GET, "/api/auth/me", tokenAntigo, null, UsuarioAtualResponseDTO.class)
                .getBody().deveTrocarSenha()).isTrue();

        // O token antigo precisa ser de um segundo anterior à revogação (o iat é em segundos).
        Thread.sleep(1100);
        ResponseEntity<LoginResponseDTO> troca = chamar(HttpMethod.PUT, "/api/auth/senha", tokenAntigo,
                new TrocarSenhaRequestDTO(SENHA, NOVA_SENHA), LoginResponseDTO.class);
        assertThat(troca.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(troca.getBody().deveTrocarSenha()).isFalse();
        String tokenNovo = troca.getBody().token();

        // Emitido logo depois da revogação (possivelmente no mesmo segundo): tem que ser aceito.
        assertThat(chamar(HttpMethod.GET, "/api/fichas", tokenNovo, null, String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(chamar(HttpMethod.GET, "/api/fichas", tokenAntigo, null, String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        assertThat(restTemplate.postForEntity("/api/auth/login",
                new LoginRequestDTO(profissional.getUsername(), NOVA_SENHA), String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        List<ContaAuditLog> linhas = contaAuditLogRepository.findByProfissionalIdOrderByTimestampAsc(profissional.getId());
        assertThat(linhas).extracting(ContaAuditLog::getAcao).containsExactly(AcaoConta.TROCAR_PROPRIA_SENHA);
        assertThat(linhas.get(0).getAutorId()).isEqualTo(profissional.getId());
    }

    @Test
    void trocarSenha_senhaAtualErrada_retorna400_eNaoContaComoFalhaDeLogin() {
        Profissional profissional = persistir(PapelProfissional.PADRAO, false, false);

        ResponseEntity<String> resposta = chamar(HttpMethod.PUT, "/api/auth/senha", token(profissional),
                new TrocarSenhaRequestDTO("errada-000", NOVA_SENHA), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(contaAuditLogRepository.findByProfissionalIdOrderByTimestampAsc(profissional.getId())).isEmpty();
    }

    @Test
    void trocarSenha_novaSenhaInvalida_retorna400() {
        Profissional profissional = persistir(PapelProfissional.PADRAO, false, false);
        String token = token(profissional);

        for (String nova : List.of("curta12", "a".repeat(73), "é".repeat(72), SENHA)) {
            assertThat(chamar(HttpMethod.PUT, "/api/auth/senha", token,
                    new TrocarSenhaRequestDTO(SENHA, nova), String.class).getStatusCode())
                    .as(nova)
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }
}
