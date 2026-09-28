package com.azizaid.hub.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// ARQUIVADO se chamava ENCERRADO. O script db/alteracoes/2026-09-status-arquivado.sql renomeia as
// linhas, mas roda depois do deploy; até lá (e ao restaurar um backup antigo) o banco ainda pode
// ter ENCERRADO. Lê os dois, grava sempre o nome atual.
@Converter
public class StatusFichaConverter implements AttributeConverter<StatusFicha, String> {

    private static final String NOME_ANTIGO_ARQUIVADO = "ENCERRADO";

    @Override
    public String convertToDatabaseColumn(StatusFicha status) {
        return status != null ? status.name() : null;
    }

    @Override
    public StatusFicha convertToEntityAttribute(String valor) {
        if (valor == null) {
            return null;
        }
        if (NOME_ANTIGO_ARQUIVADO.equals(valor)) {
            return StatusFicha.ARQUIVADO;
        }
        return StatusFicha.valueOf(valor);
    }
}
