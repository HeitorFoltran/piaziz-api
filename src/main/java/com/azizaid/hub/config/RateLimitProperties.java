package com.azizaid.hub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(
        Regra login,
        Regra fichaPublicaStatus,
        Regra fichaPublicaEnvio,
        LoginPorEmail loginPorEmail) {

    public record Regra(int capacidade, int janelaMinutos) {
    }

    public record LoginPorEmail(int maxFalhas, int janelaMinutos) {
    }
}
