package com.PachocloSystem.PachocloSystem.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Paciente {

    @NotBlank(message = "El ID del paciente no puede estar vacío")
    private String idPaciente;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @Min(value = 0, message = "La edad no puede ser negativa")
    private int edad;

    @Min(value = 0, message = "La habitación no puede ser negativa")
    private int habitacion;

    @Builder.Default
    private List<RegistroClinico> registros = new ArrayList<>();
}