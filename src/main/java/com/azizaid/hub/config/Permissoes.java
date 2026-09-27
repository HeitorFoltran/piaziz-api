package com.azizaid.hub.config;

import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.model.enums.PapelProfissional;
import com.azizaid.hub.repository.ProfissionalRepository;
import org.springframework.stereotype.Component;

// Usado em @PreAuthorize("@permissoes...."). Sempre lê o banco, nunca o JWT, como o JwtAuthFilter faz
// com a role: retirar a flag vale já na próxima requisição.
@Component("permissoes")
public class Permissoes {

    private final ProfissionalRepository profissionalRepository;

    public Permissoes(ProfissionalRepository profissionalRepository) {
        this.profissionalRepository = profissionalRepository;
    }

    public boolean podeGerenciarProfissionais() {
        return CurrentUser.id()
                .flatMap(profissionalRepository::findById)
                .map(Permissoes::ehGerenciador)
                .orElse(false);
    }

    // Gerenciador: DEV, ou PADRAO ativo com a flag pode_gerenciar_profissionais.
    public static boolean ehGerenciador(Profissional profissional) {
        if (!profissional.isAtivo()) {
            return false;
        }
        return profissional.getRole() == PapelProfissional.DEV
                || (profissional.getRole() == PapelProfissional.PADRAO && profissional.isPodeGerenciarProfissionais());
    }
}
