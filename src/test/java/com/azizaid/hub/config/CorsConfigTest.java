package com.azizaid.hub.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorsConfigTest {

    @Test
    void curingaNaLista_falhaNaConstrucao() {
        assertThatThrownBy(() -> new CorsConfig(new String[]{"http://localhost:5173", " * "}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("'*'");
    }

    @Test
    void espacosEItensVazios_saoDescartados() {
        CorsConfig config = new CorsConfig(new String[]{" https://a.exemplo.com ", "", "   ", "https://b.exemplo.com"});

        assertThat(config.origensPermitidas()).containsExactly("https://a.exemplo.com", "https://b.exemplo.com");
    }

    @Test
    void listaVazia_ePermitida() {
        assertThat(new CorsConfig(new String[]{}).origensPermitidas()).isEmpty();
    }
}
