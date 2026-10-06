package com.pachoclosystem.pachoclosystem.dto;

import java.time.LocalDate;

/**
 * Edición de los datos de un medicamento (sin stock). Se reenvía tal cual a
 * MedicamentosService, que valida los campos.
 */
public record ActualizarMedicamentoRequest(
        String nombre,
        String principioActivo,
        String presentacion,
        String concentracion,
        String laboratorio,
        String lote,
        Integer stockMinimo,
        LocalDate fechaVencimiento,
        String ubicacion) {
}
