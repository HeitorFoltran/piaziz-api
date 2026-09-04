package com.azizaid.hub.controller;

import com.azizaid.hub.dto.response.AcompanhamentoResumoDTO;
import com.azizaid.hub.service.AcompanhamentoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/acompanhamentos")
@PreAuthorize("hasAnyRole('PADRAO','DEV')")
public class AcompanhamentoController {

    private final AcompanhamentoService acompanhamentoService;

    public AcompanhamentoController(AcompanhamentoService acompanhamentoService) {
        this.acompanhamentoService = acompanhamentoService;
    }

    @GetMapping
    public List<AcompanhamentoResumoDTO> listar(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "servicoId", required = false) Long servicoId) {
        return acompanhamentoService.listar(q, servicoId);
    }
}