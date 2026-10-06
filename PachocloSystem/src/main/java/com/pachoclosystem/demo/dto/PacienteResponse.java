package com.pachoclosystem.demo.dto;

import com.pachoclosystem.demo.model.Paciente;

public record PacienteResponse(String idPaciente, String nombre, int edad, int habitacion) {

    public static PacienteResponse from(Paciente p) {
        return new PacienteResponse(p.getIdPaciente(), p.getNombre(), p.getEdad(), p.getHabitacion());
    }
}
