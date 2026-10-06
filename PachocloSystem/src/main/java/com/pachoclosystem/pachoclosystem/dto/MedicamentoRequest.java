package com.pachoclosystem.pachoclosystem.dto;

import java.time.LocalDate;

/**
 * Alta de un medicamento. Se reenvía tal cual a MedicamentosService, que es quien
 * valida los campos y devuelve los mensajes de error (se trasladan al cliente).
 */
public record MedicamentoRequest(
        String nombre,
        String principioActivo,
        String presentacion,
        String concentracion,
        String laboratorio,
        String lote,
        Integer cantidadStock,
        Integer stockMinimo,
        LocalDate fechaVencimiento,
        String ubicacion) {
}
