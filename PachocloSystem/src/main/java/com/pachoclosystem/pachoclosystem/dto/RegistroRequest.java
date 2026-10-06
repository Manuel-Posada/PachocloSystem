package com.pachoclosystem.pachoclosystem.dto;

import com.pachoclosystem.pachoclosystem.model.TipoRegistro;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Para SIGNOS_VITALES se envían {@code signosVitales}; para los demás tipos, {@code contenido}.
 */
public record RegistroRequest(
        @NotNull(message = "Debe seleccionar un tipo de registro.")
        TipoRegistro tipo,

        @NotBlank(message = "El ID del autor es obligatorio.")
        @Pattern(regexp = "^[A-Za-z0-9\\-]{1,20}$",
                message = "El ID del autor solo puede tener letras, números y guiones.")
        String idAutor,

        String contenido,

        @Valid
        SignosVitalesRequest signosVitales) {
}
