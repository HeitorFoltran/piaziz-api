package com.azizaid.hub.dto.response;

import com.azizaid.hub.model.Profissional;

public record ProfissionalResponseDTO(
        Long id,
        String nome,
        String cpf,
        String carteiraProfissional,
        Long servicoId,
        String servicoNome,
        String email,
        String role
) {
    public static ProfissionalResponseDTO from(Profissional p) {
        return new ProfissionalResponseDTO(
                p.getId(),
                p.getNome(),
                p.getCpf(),
                p.getCarteiraProfissional(),
                p.getServico() != null ? p.getServico().getId() : null,
                p.getServico() != null ? p.getServico().getNome() : null,
                p.getEmail(),
                p.getRole() != null ? p.getRole().name() : null
        );
    }
}
