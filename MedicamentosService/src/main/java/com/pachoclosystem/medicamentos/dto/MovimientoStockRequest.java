package com.pachoclosystem.medicamentos.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Entrada o salida de unidades de stock. */
public record MovimientoStockRequest(
        @NotNull(message = "La cantidad es obligatoria.")
        @Min(value = 1, message = "La cantidad debe ser un entero entre 1 y 1000000.")
        @Max(value = 1_000_000, message = "La cantidad debe ser un entero entre 1 y 1000000.")
        Integer cantidad) {
}
