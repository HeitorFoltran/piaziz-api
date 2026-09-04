package com.azizaid.hub.dto.response;

import com.azizaid.hub.model.Servico;

public record ServicoResponseDTO(
        Long id,
        String nome
) {
    public static ServicoResponseDTO from(Servico s) {
        return new ServicoResponseDTO(s.getId(), s.getNome());
    }
}
