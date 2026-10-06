package com.pachoclosystem.pachoclosystem.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PacienteRequest(
        @NotBlank(message = "El nombre completo es obligatorio.")
        @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÑÜáéíóúñü][A-Za-zÁÉÍÓÚÑÜáéíóúñü\\s]{2,59}$",
                message = "El nombre debe tener letras y espacios (3 a 60 caracteres).")
        String nombre,

        @NotNull(message = "La edad es obligatoria.")
        @Min(value = 0, message = "La edad debe ser un número entero entre 0 y 120.")
        @Max(value = 120, message = "La edad debe ser un número entero entre 0 y 120.")
        Integer edad,

        @NotNull(message = "El número de habitación es obligatorio.")
        @Min(value = 1, message = "El número de habitación debe ser un entero entre 1 y 999.")
        @Max(value = 999, message = "El número de habitación debe ser un entero entre 1 y 999.")
        Integer habitacion) {
}
