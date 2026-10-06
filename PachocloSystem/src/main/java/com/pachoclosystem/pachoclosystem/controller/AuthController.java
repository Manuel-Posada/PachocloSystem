package com.pachoclosystem.pachoclosystem.controller;

import com.pachoclosystem.pachoclosystem.dto.LoginRequest;
import com.pachoclosystem.pachoclosystem.dto.LoginResponse;
import com.pachoclosystem.pachoclosystem.dto.UsuarioResponse;
import com.pachoclosystem.pachoclosystem.model.Usuario;
import com.pachoclosystem.pachoclosystem.security.AuthService;
import com.pachoclosystem.pachoclosystem.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autenticación: {@code POST /api/auth/login} (público) emite el token JWT y
 * {@code GET /api/auth/me} (autenticado) devuelve el usuario actual releyendo
 * el repositorio (nunca los claims del token).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;

    public AuthController(AuthService authService, UsuarioService usuarioService) {
        this.authService = authService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest solicitud) {
        return authService.iniciarSesion(solicitud.username(), solicitud.password());
    }

    @GetMapping("/me")
    public UsuarioResponse me(Authentication autenticacion) {
        // El nombre de la autenticación es el username validado y activo que
        // fija JwtUsuarioAuthenticationConverter al releer el repositorio.
        Usuario usuario = usuarioService.buscarPorUsername(autenticacion.getName());
        return UsuarioResponse.from(usuario);
    }
}