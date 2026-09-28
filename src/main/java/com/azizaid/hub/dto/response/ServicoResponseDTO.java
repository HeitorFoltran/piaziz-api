package com.azizaid.hub.dto.response;

import com.azizaid.hub.model.Servico;

// emUso: tem encaminhamento ou profissional ligado a ele, então não pode ser excluído.
public record ServicoResponseDTO(
        Long id,
        String nome,
        boolean emUso
) {
    public static ServicoResponseDTO from(Servico s, boolean emUso) {
        return new ServicoResponseDTO(s.getId(), s.getNome(), emUso);
    }
}
