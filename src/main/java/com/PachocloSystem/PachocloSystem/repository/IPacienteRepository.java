package com.PachocloSystem.PachocloSystem.repository;

import com.PachocloSystem.PachocloSystem.model.Paciente;
import java.util.List;
import java.util.Optional;


public interface IPacienteRepository {
    Paciente guardar(Paciente paciente); //Metodo para crear un paciente nuevo
    Paciente actualizar(Paciente paciente); //Metodo para actualizar a un paciente ya creado
    Optional<Paciente> obtenerPorId(String id); //Metodo para mostrar por ID a un paciente ya creado
    List<Paciente> obtenerTodos(); //Metodo para listar a TODOS los pacientes ya creados
    boolean eliminarPorId(String id); //Metodo para eliminar por ID a un paciente ya creado
}