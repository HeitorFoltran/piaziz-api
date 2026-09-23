package com.azizaid.hub.service;

import com.azizaid.hub.model.FichaPublicaAuditLog;
import com.azizaid.hub.model.enums.ResultadoFichaPublica;
import com.azizaid.hub.repository.FichaPublicaAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FichaPublicaAuditLogServiceTest {

    @Mock
    FichaPublicaAuditLogRepository repository;

    FichaPublicaAuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new FichaPublicaAuditLogService(repository);
    }

    @Test
    void registrar_salvaLogComOsCamposInformados() {
        auditLogService.registrar(7L, "127.0.0.1", ResultadoFichaPublica.TOKEN_EXPIRADO);

        verify(repository).save(org.mockito.ArgumentMatchers.argThat((FichaPublicaAuditLog log) ->
                log.getConviteId().equals(7L)
                        && log.getIp().equals("127.0.0.1")
                        && log.getResultado() == ResultadoFichaPublica.TOKEN_EXPIRADO));
    }
}
