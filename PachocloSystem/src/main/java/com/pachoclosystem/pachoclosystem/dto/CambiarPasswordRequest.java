package com.pachoclosystem.pachoclosystem.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Cambio de la contraseña propia ({@code POST /api/auth/password}): exige la
 * actual y la nueva, que debe cumplir la política. Los tokens emitidos antes
 * quedan revocados.
 */
public record CambiarPasswordRequest(
        @NotBlank(message = "La contraseña actual es obligatoria.") String passwordActual,
        @NotBlank(message = "La nueva contraseña es obligatoria.") String passwordNueva) {

    /** Las contraseñas nunca aparecen en {@code toString()} (logs, errores). */
    @Override
    public String toString() {
        return "CambiarPasswordRequest{passwordActual=***, passwordNueva=***}";
    }
}
