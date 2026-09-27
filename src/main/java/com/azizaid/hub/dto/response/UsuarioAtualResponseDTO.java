package com.azizaid.hub.dto.response;

import com.azizaid.hub.config.Permissoes;
import com.azizaid.hub.model.Profissional;

public record UsuarioAtualResponseDTO(
        Long id,
        String nome,
        String username,
        String email,
        String role,
        boolean podeGerenciarProfissionais,
        boolean deveTrocarSenha
) {
    public static UsuarioAtualResponseDTO from(Profissional p) {
        return new UsuarioAtualResponseDTO(
                p.getId(),
                p.getNome(),
                p.getUsername(),
                p.getEmail(),
                p.getRole().name(),
                Permissoes.ehGerenciador(p),
                p.isDeveTrocarSenha());
    }
}
