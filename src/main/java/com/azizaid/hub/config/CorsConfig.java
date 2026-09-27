package com.azizaid.hub.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final List<String> origensPermitidas;

    public CorsConfig(@Value("${cors.allowed-origins}") String[] origens) {
        this.origensPermitidas = Arrays.stream(origens)
                .map(String::trim)
                .filter(origem -> !origem.isEmpty())
                .toList();
        if (origensPermitidas.contains("*")) {
            throw new IllegalStateException(
                    "cors.allowed-origins não pode conter '*': liste as origens explicitamente "
                            + "(itens separados por vírgula, ex.: https://app.exemplo.com)");
        }
    }

    List<String> origensPermitidas() {
        return origensPermitidas;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(origensPermitidas.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE");
    }
}
