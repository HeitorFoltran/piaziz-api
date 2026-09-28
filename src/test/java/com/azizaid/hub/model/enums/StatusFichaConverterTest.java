package com.azizaid.hub.model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatusFichaConverterTest {

    private final StatusFichaConverter converter = new StatusFichaConverter();

    @Test
    void leNomeAntigoEncerradoComoArquivado() {
        assertEquals(StatusFicha.ARQUIVADO, converter.convertToEntityAttribute("ENCERRADO"));
    }

    @Test
    void leNomesAtuais() {
        for (StatusFicha status : StatusFicha.values()) {
            assertEquals(status, converter.convertToEntityAttribute(status.name()));
        }
    }

    @Test
    void gravaSempreONomeAtual() {
        assertEquals("ARQUIVADO", converter.convertToDatabaseColumn(StatusFicha.ARQUIVADO));
    }

    @Test
    void nuloContinuaNulo() {
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToDatabaseColumn(null));
    }
}
