package com.pachoclosystem.pachoclosystem.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Alta de usuario por un administrador. El nuevo usuario nace con el cambio de
 * contraseña obligatorio (debe cambiarla en su primer acceso).
 */
public record CrearUsuarioRequest(
        @NotBlank(message = "El username es obligatorio.") String username,
        @NotBlank(message = "La contraseña es obligatoria.") String password,
        String rol,
        String idTrabajador) {
}