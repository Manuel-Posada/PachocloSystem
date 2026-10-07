package com.pachoclosystem.pachoclosystem.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Reset administrativo de la contraseña de un usuario: la nueva contraseña se
 * considera temporal y el usuario debe cambiarla en su siguiente acceso.
 */
public record ResetPasswordRequest(
        @NotBlank(message = "La contraseña es obligatoria.") String password) {
}