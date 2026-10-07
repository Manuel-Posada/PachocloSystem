package com.pachoclosystem.pachoclosystem.controller;

import com.pachoclosystem.pachoclosystem.dto.CambiarPasswordRequest;
import com.pachoclosystem.pachoclosystem.dto.LoginRequest;
import com.pachoclosystem.pachoclosystem.dto.LoginResponse;
import com.pachoclosystem.pachoclosystem.dto.UsuarioResponse;
import com.pachoclosystem.pachoclosystem.model.Usuario;
import com.pachoclosystem.pachoclosystem.security.AuthService;
import com.pachoclosystem.pachoclosystem.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autenticación: {@code POST /api/auth/login} (público) emite el token JWT,
 * {@code GET /api/auth/me} (autenticado) devuelve el usuario actual releyendo
 * el repositorio (nunca los claims del token) y {@code POST /api/auth/password}
 * (autenticado) cambia la contraseña del usuario, revocando de inmediato los
 * tokens emitidos antes del cambio.
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
    public LoginResponse login(@Valid @RequestBody LoginRequest solicitud, HttpServletRequest peticionHttp) {
        // La IP sale SOLO de HttpServletRequest.getRemoteAddr(). No se leen las
        // cabeceras X-Forwarded-For ni similares: no hay ningún proxy inverso de
        // confianza delante de la aplicación y confiar en ellas permitiría a un
        // cliente falsificar su IP de origen para esquivar (o provocar) el
        // bloqueo de terceros.
        return authService.iniciarSesion(solicitud.username(), solicitud.password(),
                peticionHttp.getRemoteAddr());
    }

    @GetMapping("/me")
    public UsuarioResponse me(Authentication autenticacion) {
        // El nombre de la autenticación es el username validado y activo que
        // fija JwtUsuarioAuthenticationConverter al releer el repositorio.
        Usuario usuario = usuarioService.buscarPorUsername(autenticacion.getName());
        return UsuarioResponse.from(usuario);
    }

    /**
     * Cambio de contraseña del propio usuario. Tras el cambio, el token que
     * acaba de usarse queda revocado (la versión del token se incrementa): el
     * cliente debe iniciar sesión de nuevo con la nueva contraseña.
     */
    @PostMapping("/password")
    public ResponseEntity<Void> cambiarPassword(@Valid @RequestBody CambiarPasswordRequest solicitud,
                                                Authentication autenticacion) {
        Usuario usuario = usuarioService.buscarPorUsername(autenticacion.getName());
        usuarioService.cambiarPasswordPropia(usuario.getIdUsuario(),
                solicitud.passwordActual(), solicitud.passwordNueva());
        return ResponseEntity.noContent().build();
    }
}