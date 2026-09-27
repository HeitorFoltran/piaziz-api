package com.azizaid.hub.system;

import com.azizaid.hub.dto.request.LoginRequestDTO;
import com.azizaid.hub.dto.response.LoginResponseDTO;
import com.azizaid.hub.model.AuthAuditLog;
import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.model.enums.PapelProfissional;
import com.azizaid.hub.repository.AuthAuditLogRepository;
import com.azizaid.hub.repository.ProfissionalRepository;
import com.azizaid.hub.support.PostgresTestContainerConfig;
import com.azizaid.hub.support.ProfissionalTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Contra Postgres real de propósito: o bug era o rollback da transação de AuthService.login levar
// junto a linha de auditoria da tentativa com falha, o que um teste com Mockito não enxerga.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "rate-limit.login-por-email.max-falhas=3"
})
class AuthAuditoriaSystemTest extends PostgresTestContainerConfig {

    private static final String SENHA = "senha-correta-123";

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    ProfissionalRepository profissionalRepository;

    @Autowired
    AuthAuditLogRepository authAuditLogRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    private Profissional persistirProfissional() {
        return profissionalRepository.save(Profissional.builder()
                .nome("Profissional Auditoria")
                .cpf("12345678900")
                .username(ProfissionalTestFactory.usernameUnico())
                .email("auditoria." + UUID.randomUUID() + "@azizaidhub.local")
                .senhaHash(passwordEncoder.encode(SENHA))
                .role(PapelProfissional.PADRAO)
                .build());
    }

    private ResponseEntity<String> login(String identificador, String senha) {
        return restTemplate.postForEntity("/api/auth/login", new LoginRequestDTO(identificador, senha), String.class);
    }

    private List<AuthAuditLog> linhasDe(String identificador) {
        return authAuditLogRepository.findAll().stream()
                .filter(linha -> linha.getEmailTentado().equals(identificador))
                .toList();
    }

    private List<AuthAuditLog> linhasDaConta(Long profissionalId) {
        return authAuditLogRepository.findAll().stream()
                .filter(linha -> profissionalId.equals(linha.getProfissionalId()))
                .toList();
    }

    @Test
    void login_comSenhaErrada_retorna401EGravaFalhaComProfissionalId() {
        Profissional profissional = persistirProfissional();

        assertThat(login(profissional.getEmail(), "errada").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        List<AuthAuditLog> linhas = linhasDe(profissional.getEmail());
        assertThat(linhas).hasSize(1);
        assertThat(linhas.get(0).isSucesso()).isFalse();
        assertThat(linhas.get(0).getMotivoFalha()).isEqualTo("senha incorreta");
        assertThat(linhas.get(0).getProfissionalId()).isEqualTo(profissional.getId());
    }

    @Test
    void login_comIdentificadorDesconhecido_retorna401EGravaFalhaSemProfissionalId() {
        String email = "desconhecido." + UUID.randomUUID() + "@azizaidhub.local";

        assertThat(login(email, "qualquer").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        List<AuthAuditLog> linhas = linhasDe(email);
        assertThat(linhas).hasSize(1);
        assertThat(linhas.get(0).getMotivoFalha()).isEqualTo("identificador desconhecido");
        assertThat(linhas.get(0).getProfissionalId()).isNull();
    }

    @Test
    void login_porUsernameEPorEmail_comMaiusculasMisturadas_retorna200() {
        Profissional profissional = persistirProfissional();

        ResponseEntity<LoginResponseDTO> porUsername = restTemplate.postForEntity("/api/auth/login",
                new LoginRequestDTO("  " + profissional.getUsername().toUpperCase() + " ", SENHA), LoginResponseDTO.class);
        ResponseEntity<LoginResponseDTO> porEmail = restTemplate.postForEntity("/api/auth/login",
                new LoginRequestDTO(profissional.getEmail().toUpperCase(), SENHA), LoginResponseDTO.class);

        assertThat(porUsername.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(porUsername.getBody().username()).isEqualTo(profissional.getUsername());
        assertThat(porUsername.getBody().profissionalId()).isEqualTo(profissional.getId());
        assertThat(porEmail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(porEmail.getBody().email()).isEqualTo(profissional.getEmail());
        assertThat(linhasDaConta(profissional.getId())).allMatch(AuthAuditLog::isSucesso).hasSize(2);
    }

    @Test
    void login_comCampoAntigoEmail_continuaFuncionando() {
        Profissional profissional = persistirProfissional();

        ResponseEntity<String> resposta = restTemplate.postForEntity("/api/auth/login",
                Map.of("email", profissional.getEmail(), "senha", SENHA), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void login_depoisDoLimiteDeFalhas_retorna429MesmoComSenhaCerta() {
        Profissional profissional = persistirProfissional();
        for (int i = 0; i < 3; i++) {
            assertThat(login(profissional.getEmail(), "errada").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        assertThat(login(profissional.getEmail(), SENHA).getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        assertThat(linhasDaConta(profissional.getId()))
                .extracting(AuthAuditLog::getMotivoFalha)
                .containsExactlyInAnyOrder("senha incorreta", "senha incorreta", "senha incorreta", "limite por conta");
    }

    @Test
    void login_falhasAlternandoEmailEUsername_somamNoMesmoContador() {
        Profissional profissional = persistirProfissional();

        assertThat(login(profissional.getEmail(), "errada").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(login(profissional.getUsername(), "errada").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(login(profissional.getEmail(), "errada").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        assertThat(login(profissional.getUsername(), SENHA).getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void login_comIdentificadorMaiorQueAColunaDeAuditoria_retorna400() {
        String email = "a".repeat(140) + "@exemplo.com";

        assertThat(login(email, "qualquer").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
