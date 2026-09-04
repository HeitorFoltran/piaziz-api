package com.azizaid.hub.controller;

import com.azizaid.hub.dto.request.LoginRequestDTO;
import com.azizaid.hub.dto.response.LoginResponseDTO;
import com.azizaid.hub.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponseDTO login(@Valid @RequestBody LoginRequestDTO dto, HttpServletRequest request) {
        return authService.login(dto, request.getRemoteAddr());
    }
}
