package com.azizaid.hub.integration;

import com.azizaid.hub.model.LeituraAuditLog;
import com.azizaid.hub.repository.LeituraAuditLogRepository;
import com.azizaid.hub.service.RetencaoLeituraAuditService;
import com.azizaid.hub.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RetencaoLeituraAuditIT extends PostgresTestContainerConfig {

    @Autowired
    RetencaoLeituraAuditService retencaoLeituraAuditService;

    @Autowired
    LeituraAuditLogRepository leituraAuditLogRepository;

    // Longe da borda de 365 dias de propósito, pelo mesmo motivo do RetencaoFichaPendenteIT.
    @Test
    void aplicarRetencao_apagaLinhaCom366DiasEMantemAde364() {
        LocalDateTime agora = LocalDateTime.now();
        Long antiga = persistir(agora.minusDays(366));
        Long recente = persistir(agora.minusDays(364));

        int apagadas = retencaoLeituraAuditService.aplicarRetencao();

        assertThat(apagadas).isEqualTo(1);
        assertThat(leituraAuditLogRepository.existsById(antiga)).isFalse();
        assertThat(leituraAuditLogRepository.existsById(recente)).isTrue();
    }

    private Long persistir(LocalDateTime timestamp) {
        return leituraAuditLogRepository.save(LeituraAuditLog.builder()
                .fichaId(424_242L)
                .profissionalId(424_242L)
                .timestamp(timestamp)
                .build()).getId();
    }
}
