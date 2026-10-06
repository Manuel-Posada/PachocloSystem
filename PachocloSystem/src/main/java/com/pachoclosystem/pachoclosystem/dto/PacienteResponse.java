package com.pachoclosystem.pachoclosystem.dto;

import com.pachoclosystem.pachoclosystem.model.Paciente;

public record PacienteResponse(String idPaciente, String nombre, int edad, int habitacion) {

    public static PacienteResponse from(Paciente p) {
        return new PacienteResponse(p.getIdPaciente(), p.getNombre(), p.getEdad(), p.getHabitacion());
    }
}
