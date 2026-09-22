package com.azizaid.hub.controller;

import com.azizaid.hub.dto.request.FichaPendenteRejeitarRequestDTO;
import com.azizaid.hub.dto.response.FichaPendenteResponseDTO;
import com.azizaid.hub.model.enums.StatusFichaPendente;
import com.azizaid.hub.service.FichaPendenteService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fichas-pendentes")
@PreAuthorize("hasAnyRole('PADRAO','DEV')")
public class FichaPendenteController {

    private final FichaPendenteService fichaPendenteService;

    public FichaPendenteController(FichaPendenteService fichaPendenteService) {
        this.fichaPendenteService = fichaPendenteService;
    }

    @GetMapping
    public List<FichaPendenteResponseDTO> listar(
            @RequestParam(value = "status", required = false) StatusFichaPendente status) {
        return fichaPendenteService.listar(status);
    }

    @PostMapping("/{id}/aprovar")
    public FichaPendenteResponseDTO aprovar(@PathVariable Long id) {
        return fichaPendenteService.aprovar(id);
    }

    @PostMapping("/{id}/rejeitar")
    public FichaPendenteResponseDTO rejeitar(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) FichaPendenteRejeitarRequestDTO dto) {
        String motivo = dto != null ? dto.motivo() : null;
        return fichaPendenteService.rejeitar(id, motivo);
    }
}
