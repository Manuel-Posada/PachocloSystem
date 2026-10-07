package com.pachoclosystem.pachoclosystem.dto;

import com.pachoclosystem.pachoclosystem.model.TipoRegistro;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Para SIGNOS_VITALES se envían {@code signosVitales}; para los demás tipos, {@code contenido}.
 *
 * <p>Opcional, solo para MEDICACION: {@code idMedicamento} y {@code cantidad} (los dos
 * juntos) descuentan ese stock en MedicamentosService al crear el registro.</p>
 */
public record RegistroRequest(
        @NotNull(message = "Debe seleccionar un tipo de registro.")
        TipoRegistro tipo,

        // Opcional: el autor es siempre el trabajador del usuario autenticado. Si
        // viene, debe coincidir con él (si no, 400). Se acepta por compatibilidad.
        @Pattern(regexp = "^[A-Za-z0-9\\-]{1,20}$",
                message = "El ID del autor solo puede tener letras, números y guiones.")
        String idAutor,

        String contenido,

        @Valid
        SignosVitalesRequest signosVitales,

        @Pattern(regexp = "^[A-Za-z0-9\\-]{1,20}$",
                message = "El ID del medicamento solo puede tener letras, números y guiones.")
        String idMedicamento,

        @Min(value = 1, message = "La cantidad administrada debe ser un entero entre 1 y 1000000.")
        @Max(value = 1_000_000, message = "La cantidad administrada debe ser un entero entre 1 y 1000000.")
        Integer cantidad) {
}
