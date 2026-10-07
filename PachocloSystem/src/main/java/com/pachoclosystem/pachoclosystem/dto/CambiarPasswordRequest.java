package com.pachoclosystem.pachoclosystem.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Cambio de la contraseña propia: exige la actual (que deja de valer de
 * inmediato: los tokens emitidos quedan revocados) y la nueva, que debe cumplir
 * la política.
 */
public record CambiarPasswordRequest(
        @NotBlank(message = "La contraseña actual es obligatoria.") String passwordActual,
        @NotBlank(message = "La nueva contraseña es obligatoria.") String passwordNueva) {
}