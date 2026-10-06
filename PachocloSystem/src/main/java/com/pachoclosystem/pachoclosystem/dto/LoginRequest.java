package com.pachoclosystem.pachoclosystem.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Credenciales del login. Username y contraseña son obligatorios.
 */
public record LoginRequest(
        @NotBlank(message = "El username es obligatorio.") String username,
        @NotBlank(message = "La contraseña es obligatoria.") String password) {
}