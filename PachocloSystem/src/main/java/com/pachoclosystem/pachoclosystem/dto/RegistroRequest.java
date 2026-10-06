package com.pachoclosystem.pachoclosystem.dto;

import com.pachoclosystem.pachoclosystem.model.TipoRegistro;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Para SIGNOS_VITALES se envían {@code signosVitales}; para los demás tipos, {@code contenido}.
 * El autor del registro no viaja en el cuerpo: se toma del usuario autenticado.
 */
public record RegistroRequest(
        @NotNull(message = "Debe seleccionar un tipo de registro.")
        TipoRegistro tipo,

        String contenido,

        @Valid
        SignosVitalesRequest signosVitales) {
}
