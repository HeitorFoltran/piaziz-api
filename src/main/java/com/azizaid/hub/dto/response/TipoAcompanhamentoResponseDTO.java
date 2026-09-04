package com.azizaid.hub.dto.response;

import com.azizaid.hub.model.TipoAcompanhamento;

public record TipoAcompanhamentoResponseDTO(Long id, String nome) {
    public static TipoAcompanhamentoResponseDTO from(TipoAcompanhamento t) {
        return new TipoAcompanhamentoResponseDTO(t.getId(), t.getNome());
    }
}
