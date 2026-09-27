package com.azizaid.hub.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// Prazo 0 apagaria tudo o que já foi revisado, por isso o @Min(1) falha a subida.
@Validated
@ConfigurationProperties(prefix = "retencao.ficha-pendente")
public record RetencaoProperties(
        @Min(1) int aprovadaDias,
        @Min(1) int rejeitadaDias,
        @NotBlank String cron) {
}
