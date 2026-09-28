package com.azizaid.hub.controller;

import com.azizaid.hub.dto.request.FiltroAcompanhamentos;
import com.azizaid.hub.dto.request.FiltroAcompanhamentos.CampoData;
import com.azizaid.hub.dto.response.AcompanhamentoResumoDTO;
import com.azizaid.hub.dto.response.PaginaDTO;
import com.azizaid.hub.model.enums.StatusFicha;
import com.azizaid.hub.service.AcompanhamentoService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/acompanhamentos")
@PreAuthorize("hasAnyRole('PADRAO','DEV')")
public class AcompanhamentoController {

    private final AcompanhamentoService acompanhamentoService;

    public AcompanhamentoController(AcompanhamentoService acompanhamentoService) {
        this.acompanhamentoService = acompanhamentoService;
    }

    @GetMapping
    public PaginaDTO<AcompanhamentoResumoDTO> listar(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "servicoId", required = false) Long servicoId,
            @RequestParam(value = "status", required = false) StatusFicha status,
            @RequestParam(value = "tipoId", required = false) Long tipoId,
            @RequestParam(value = "meus", defaultValue = "false") boolean meus,
            @RequestParam(value = "campoData", required = false) String campoData,
            @RequestParam(value = "de", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(value = "ate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(value = "page", defaultValue = "0")
            @Min(value = 0, message = "deve ser maior ou igual a 0") int page,
            @RequestParam(value = "size", defaultValue = "20")
            @Min(value = 1, message = "deve estar entre 1 e 100")
            @Max(value = 100, message = "deve estar entre 1 e 100") int size) {
        FiltroAcompanhamentos filtro = new FiltroAcompanhamentos(
                q, servicoId, status, tipoId, meus, CampoData.fromParametro(campoData), de, ate);
        return acompanhamentoService.listar(filtro, page, size);
    }
}
