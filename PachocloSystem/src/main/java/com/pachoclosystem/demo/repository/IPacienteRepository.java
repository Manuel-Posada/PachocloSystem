package com.pachoclosystem.demo.repository;

import com.pachoclosystem.demo.model.Paciente;

import java.util.List;


public interface IPacienteRepository {
    String generarNuevoId();

    Paciente buscarPorId(String id);

    boolean guardarPaciente(Paciente p);

    List<Paciente> obtenerTodos();
    
    boolean eliminar(String idPaciente);
}