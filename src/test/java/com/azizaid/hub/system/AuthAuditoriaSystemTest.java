package com.azizaid.hub.system;

import com.azizaid.hub.dto.request.LoginRequestDTO;
import com.azizaid.hub.model.AuthAuditLog;
import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.model.enums.PapelProfissional;
import com.azizaid.hub.repository.AuthAuditLogRepository;
import com.azizaid.hub.repository.ProfissionalRepository;
import com.azizaid.hub.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
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

    private String persistirProfissional() {
        String email = "auditoria." + UUID.randomUUID() + "@azizaidhub.local";
        profissionalRepository.save(Profissional.builder()
                .nome("Profissional Auditoria")
                .cpf("12345678900")
                .email(email)
                .senhaHash(passwordEncoder.encode(SENHA))
                .role(PapelProfissional.PADRAO)
                .build());
        return email;
    }

    private ResponseEntity<String> login(String email, String senha) {
        return restTemplate.postForEntity("/api/auth/login", new LoginRequestDTO(email, senha), String.class);
    }

    private List<AuthAuditLog> linhasDe(String email) {
        return authAuditLogRepository.findAll().stream()
                .filter(linha -> linha.getEmailTentado().equals(email))
                .toList();
    }

    @Test
    void login_comSenhaErrada_retorna401EGravaFalha() {
        String email = persistirProfissional();

        assertThat(login(email, "errada").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        List<AuthAuditLog> linhas = linhasDe(email);
        assertThat(linhas).hasSize(1);
        assertThat(linhas.get(0).isSucesso()).isFalse();
        assertThat(linhas.get(0).getMotivoFalha()).isEqualTo("senha incorreta");
    }

    @Test
    void login_comEmailDesconhecido_retorna401EGravaFalha() {
        String email = "desconhecido." + UUID.randomUUID() + "@azizaidhub.local";

        assertThat(login(email, "qualquer").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        List<AuthAuditLog> linhas = linhasDe(email);
        assertThat(linhas).hasSize(1);
        assertThat(linhas.get(0).getMotivoFalha()).isEqualTo("email desconhecido");
    }

    @Test
    void login_depoisDoLimiteDeFalhasPorEmail_retorna429MesmoComSenhaCerta() {
        String email = persistirProfissional();
        for (int i = 0; i < 3; i++) {
            assertThat(login(email, "errada").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        assertThat(login(email, SENHA).getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        assertThat(linhasDe(email))
                .extracting(AuthAuditLog::getMotivoFalha)
                .containsExactlyInAnyOrder("senha incorreta", "senha incorreta", "senha incorreta", "limite por email");
    }

    @Test
    void login_comEmailMaiorQueAColunaDeAuditoria_retorna400() {
        String email = "a".repeat(140) + "@exemplo.com";

        assertThat(login(email, "qualquer").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
