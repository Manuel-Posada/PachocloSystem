package com.pachoclosystem.demo.dto;

import com.pachoclosystem.demo.model.NivelExperiencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TrabajadorRequest(
        @NotBlank(message = "El nombre completo es obligatorio.")
        @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÑÜáéíóúñü][A-Za-zÁÉÍÓÚÑÜáéíóúñü\\s]{2,59}$",
                message = "El nombre debe tener solo letras y espacios (3 a 60 caracteres).")
        String nombre,

        @NotBlank(message = "Debe seleccionar un rol.")
        @Pattern(regexp = "Doctor|Enfermero", message = "El rol debe ser Doctor o Enfermero.")
        String rol,

        String especialidad,

        NivelExperiencia nivelExperiencia) {
}
