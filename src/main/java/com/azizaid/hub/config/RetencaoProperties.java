package com.azizaid.hub.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// Prazo 0 apagaria tudo de uma vez, por isso o @Min(1) falha a subida.
@Validated
@ConfigurationProperties(prefix = "retencao")
public record RetencaoProperties(
        @NotBlank String cron,
        @Valid @NotNull FichaPendente fichaPendente,
        @Valid @NotNull LeituraAudit leituraAudit,
        @Valid @NotNull Ip ip) {

    public record FichaPendente(@Min(1) int aprovadaDias, @Min(1) int rejeitadaDias) {
    }

    public record LeituraAudit(@Min(1) int dias) {
    }

    public record Ip(@Min(1) int dias) {
    }
}
