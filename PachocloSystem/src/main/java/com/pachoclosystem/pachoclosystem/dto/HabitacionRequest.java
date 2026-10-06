package com.pachoclosystem.pachoclosystem.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record HabitacionRequest(
        @NotNull(message = "El número de habitación es obligatorio.")
        @Min(value = 1, message = "El número de habitación debe ser un entero entre 1 y 999.")
        @Max(value = 999, message = "El número de habitación debe ser un entero entre 1 y 999.")
        Integer habitacion) {
}
