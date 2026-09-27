package com.azizaid.hub.dto.response;

import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.util.CpfUtils;

// Nunca incluir senhaHash nem sessoesRevogadasEm.
public record ProfissionalResponseDTO(
        Long id,
        String nome,
        String username,
        String email,
        String cpf,
        String carteiraProfissional,
        Long servicoId,
        String servicoNome,
        String role,
        boolean ativo,
        boolean podeGerenciarProfissionais,
        boolean deveTrocarSenha
) {
    // Detalhe (formulário de edição): CPF completo.
    public static ProfissionalResponseDTO from(Profissional p) {
        return montar(p, p.getCpf());
    }

    // Listagem: CPF mascarado.
    public static ProfissionalResponseDTO resumo(Profissional p) {
        return montar(p, CpfUtils.mascarar(p.getCpf()));
    }

    private static ProfissionalResponseDTO montar(Profissional p, String cpf) {
        return new ProfissionalResponseDTO(
                p.getId(),
                p.getNome(),
                p.getUsername(),
                p.getEmail(),
                cpf,
                p.getCarteiraProfissional(),
                p.getServico() != null ? p.getServico().getId() : null,
                p.getServico() != null ? p.getServico().getNome() : null,
                p.getRole() != null ? p.getRole().name() : null,
                p.isAtivo(),
                p.isPodeGerenciarProfissionais(),
                p.isDeveTrocarSenha());
    }
}
