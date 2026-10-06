package com.pachoclosystem.medicamentos.dto;

import com.pachoclosystem.medicamentos.model.DatosMedicamento;
import com.pachoclosystem.medicamentos.model.Presentacion;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Edición de los datos de un medicamento. El stock no se edita aquí: solo cambia
 * con entradas y salidas. La fecha puede ser pasada para corregir un registro.
 */
public record ActualizarMedicamentoRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres.")
        String nombre,

        @NotBlank(message = "El principio activo es obligatorio.")
        @Size(max = 80, message = "El principio activo no puede superar 80 caracteres.")
        String principioActivo,

        @NotNull(message = "La presentación es obligatoria.")
        Presentacion presentacion,

        @NotBlank(message = "La concentración es obligatoria.")
        @Size(max = 40, message = "La concentración no puede superar 40 caracteres.")
        String concentracion,

        @NotBlank(message = "El laboratorio es obligatorio.")
        @Size(max = 80, message = "El laboratorio no puede superar 80 caracteres.")
        String laboratorio,

        @NotBlank(message = "El lote es obligatorio.")
        @Size(max = 40, message = "El lote no puede superar 40 caracteres.")
        String lote,

        @NotNull(message = "El stock mínimo es obligatorio.")
        @Min(value = 0, message = "El stock mínimo debe estar entre 0 y 1000000.")
        @Max(value = 1_000_000, message = "El stock mínimo debe estar entre 0 y 1000000.")
        Integer stockMinimo,

        @NotNull(message = "La fecha de vencimiento es obligatoria.")
        LocalDate fechaVencimiento,

        @NotBlank(message = "La ubicación de almacenamiento es obligatoria.")
        @Size(max = 80, message = "La ubicación no puede superar 80 caracteres.")
        String ubicacion) {

    public DatosMedicamento datos() {
        return new DatosMedicamento(nombre, principioActivo, presentacion, concentracion,
                laboratorio, lote, stockMinimo, fechaVencimiento, ubicacion);
    }
}
