package com.azizaid.hub.controller;

import com.azizaid.hub.dto.request.FichaPublicaRequestDTO;
import com.azizaid.hub.dto.response.FichaPublicaStatusResponseDTO;
import com.azizaid.hub.dto.response.FichaPublicaSubmissaoResponseDTO;
import com.azizaid.hub.service.FichaPublicaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ficha-publica")
public class FichaPublicaController {

    private final FichaPublicaService fichaPublicaService;

    public FichaPublicaController(FichaPublicaService fichaPublicaService) {
        this.fichaPublicaService = fichaPublicaService;
    }

    @GetMapping("/{token}/status")
    public FichaPublicaStatusResponseDTO status(@PathVariable String token) {
        return fichaPublicaService.status(token);
    }

    @PostMapping("/{token}")
    public ResponseEntity<FichaPublicaSubmissaoResponseDTO> submeter(
            @PathVariable String token,
            @Valid @RequestBody FichaPublicaRequestDTO dto,
            HttpServletRequest request) {
        fichaPublicaService.submeter(token, dto, request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new FichaPublicaSubmissaoResponseDTO(
                        "Recebido com sucesso. Nossa equipe entrará em contato em breve."));
    }
}
