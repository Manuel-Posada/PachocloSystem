package com.pachoclosystem.pachoclosystem.dto;

import java.time.LocalDate;

/** Medicamento tal como lo devuelve MedicamentosService. */
public record MedicamentoResponse(
        String idMedicamento,
        String nombre,
        String principioActivo,
        String presentacion,
        String concentracion,
        String laboratorio,
        String lote,
        int cantidadStock,
        int stockMinimo,
        LocalDate fechaVencimiento,
        String ubicacion,
        boolean stockBajo,
        boolean vencido) {
}
