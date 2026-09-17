package com.PachocloSystem.PachocloSystem.model;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroClinico {

    @NotBlank(message = "El ID del registro es obligatorio")
    private String idRegistro;

    @NotBlank(message = "La descripción no puede estar vacía")
    private String descripcion;

    @NotBlank(message = "La fecha es obligatoria")
    private String fecha;
}