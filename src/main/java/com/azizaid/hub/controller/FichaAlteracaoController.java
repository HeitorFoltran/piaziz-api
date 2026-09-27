package com.azizaid.hub.controller;

import com.azizaid.hub.dto.response.AlteracaoFichaResponseDTO;
import com.azizaid.hub.service.FichaAlteracaoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fichas/{fichaId}/alteracoes")
@PreAuthorize("hasAnyRole('PADRAO','DEV')")
public class FichaAlteracaoController {

    private final FichaAlteracaoService fichaAlteracaoService;

    public FichaAlteracaoController(FichaAlteracaoService fichaAlteracaoService) {
        this.fichaAlteracaoService = fichaAlteracaoService;
    }

    @GetMapping
    public List<AlteracaoFichaResponseDTO> listar(@PathVariable Long fichaId) {
        return fichaAlteracaoService.listarPorFicha(fichaId);
    }
}
