package com.pachoclosystem.pachoclosystem.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Activación o desactivación de un usuario decidida por un administrador.
 */
public record CambiarEstadoRequest(
        @NotNull(message = "Debe indicar si el usuario debe quedar activo.") Boolean activo) {
}