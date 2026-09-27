package com.azizaid.hub.controller;

import com.azizaid.hub.dto.request.ProfissionalEdicaoRequestDTO;
import com.azizaid.hub.dto.request.ProfissionalRequestDTO;
import com.azizaid.hub.dto.request.ResetarSenhaRequestDTO;
import com.azizaid.hub.dto.response.ProfissionalResponseDTO;
import com.azizaid.hub.service.ProfissionalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Gerenciador: DEV, ou PADRAO ativo com pode_gerenciar_profissionais (lido do banco a cada requisição).
@RestController
@RequestMapping("/api/profissionais")
@PreAuthorize("@permissoes.podeGerenciarProfissionais()")
public class ProfissionalController {

    private final ProfissionalService profissionalService;

    public ProfissionalController(ProfissionalService profissionalService) {
        this.profissionalService = profissionalService;
    }

    @GetMapping
    public List<ProfissionalResponseDTO> listar(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "servicoId", required = false) Long servicoId) {
        return profissionalService.listar(q, servicoId);
    }

    @GetMapping("/{id}")
    public ProfissionalResponseDTO buscarPorId(@PathVariable Long id) {
        return profissionalService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<ProfissionalResponseDTO> criar(@Valid @RequestBody ProfissionalRequestDTO dto) {
        ProfissionalResponseDTO criado = profissionalService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @PutMapping("/{id}")
    public ProfissionalResponseDTO editar(@PathVariable Long id, @Valid @RequestBody ProfissionalEdicaoRequestDTO dto) {
        return profissionalService.editar(id, dto);
    }

    @PostMapping("/{id}/resetar-senha")
    public ResponseEntity<Void> resetarSenha(@PathVariable Long id, @Valid @RequestBody ResetarSenhaRequestDTO dto) {
        profissionalService.resetarSenha(id, dto);
        return ResponseEntity.noContent().build();
    }
}
