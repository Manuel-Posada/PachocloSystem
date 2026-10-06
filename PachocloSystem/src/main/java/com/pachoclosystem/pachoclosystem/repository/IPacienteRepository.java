package com.pachoclosystem.pachoclosystem.repository;

import com.pachoclosystem.pachoclosystem.model.Paciente;

import java.util.List;


public interface IPacienteRepository {
    String generarNuevoId();

    Paciente buscarPorId(String id);

    boolean guardarPaciente(Paciente p);

    List<Paciente> obtenerTodos();
    
    boolean eliminar(String idPaciente);
}