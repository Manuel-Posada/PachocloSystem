package com.pachoclosystem.demo.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SignosVitalesRequest(
        @NotNull(message = "Temperatura: es obligatoria.")
        @DecimalMin(value = "30.0", message = "Temperatura: debe ser un número entre 30.0 y 45.0 °C.")
        @DecimalMax(value = "45.0", message = "Temperatura: debe ser un número entre 30.0 y 45.0 °C.")
        Double temperatura,

        @NotNull(message = "Frecuencia Cardíaca: es obligatoria.")
        @Min(value = 20, message = "Frecuencia Cardíaca: debe ser entre 20 y 250 lpm.")
        @Max(value = 250, message = "Frecuencia Cardíaca: debe ser entre 20 y 250 lpm.")
        Integer frecCardiaca,

        @NotNull(message = "Presión Sistólica: es obligatoria.")
        @Min(value = 50, message = "Presión Sistólica: debe ser entre 50 y 250 mmHg.")
        @Max(value = 250, message = "Presión Sistólica: debe ser entre 50 y 250 mmHg.")
        Integer presionSistolica,

        @NotNull(message = "Presión Diastólica: es obligatoria.")
        @Min(value = 30, message = "Presión Diastólica: debe ser entre 30 y 150 mmHg.")
        @Max(value = 150, message = "Presión Diastólica: debe ser entre 30 y 150 mmHg.")
        Integer presionDiastolica,

        @NotNull(message = "Frecuencia Respiratoria: es obligatoria.")
        @Min(value = 5, message = "Frecuencia Respiratoria: debe ser entre 5 y 60 rpm.")
        @Max(value = 60, message = "Frecuencia Respiratoria: debe ser entre 5 y 60 rpm.")
        Integer frecRespiratoria,

        @NotNull(message = "Saturación de Oxígeno: es obligatoria.")
        @Min(value = 0, message = "Saturación de Oxígeno: debe ser entre 0 y 100%.")
        @Max(value = 100, message = "Saturación de Oxígeno: debe ser entre 0 y 100%.")
        Integer saturacion,

        String observaciones) {
}
