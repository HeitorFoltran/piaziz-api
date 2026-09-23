package com.azizaid.hub.controller;

import com.azizaid.hub.dto.response.ConviteFichaResponseDTO;
import com.azizaid.hub.service.ConviteFichaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/convites-ficha")
@PreAuthorize("hasAnyRole('ESTAGIARIO','PADRAO','DEV')")
public class ConviteFichaController {

    private final ConviteFichaService conviteFichaService;

    public ConviteFichaController(ConviteFichaService conviteFichaService) {
        this.conviteFichaService = conviteFichaService;
    }

    @PostMapping
    public ResponseEntity<ConviteFichaResponseDTO> criar() {
        ConviteFichaResponseDTO criado = conviteFichaService.criar();
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @GetMapping
    public List<ConviteFichaResponseDTO> listar() {
        return conviteFichaService.listarMeusConvites();
    }

    @PostMapping("/{id}/cancelar")
    public ConviteFichaResponseDTO cancelar(@PathVariable Long id) {
        return conviteFichaService.cancelar(id);
    }
}
