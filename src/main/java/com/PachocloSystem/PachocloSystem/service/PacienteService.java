package com.PachocloSystem.PachocloSystem.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.PachocloSystem.PachocloSystem.model.Paciente;
import com.PachocloSystem.PachocloSystem.repository.IPacienteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PacienteService {

    private final IPacienteRepository pacienteRepository;

    public Paciente registrarPaciente(Paciente paciente) {
        if (pacienteRepository.obtenerPorId(paciente.getIdPaciente()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un paciente con el ID: " + paciente.getIdPaciente());
        }
        return pacienteRepository.guardar(paciente);
    }
    
    public List<Paciente> listarPacientes() {
        return pacienteRepository.obtenerTodos();
    }

    public Paciente buscarPorId(String id) {
        return pacienteRepository.obtenerPorId(id)
            .orElseThrow(() -> new RuntimeException("Paciente no encontrado con el ID: " + id));
    }

    public Paciente actualizarPaciente(String id, Paciente datosNuevos) {
        Paciente pacienteExistente = buscarPorId(id);
    
        pacienteExistente.setNombre(datosNuevos.getNombre());
        pacienteExistente.setEdad(datosNuevos.getEdad());
        pacienteExistente.setHabitacion(datosNuevos.getHabitacion());
    
        return pacienteRepository.actualizar(pacienteExistente);
    }

    public void eliminarPaciente(String id) {
        Paciente paciente = buscarPorId(id); // Si no existe, lanza la excepción
        pacienteRepository.eliminarPorId(paciente.getIdPaciente());
    }
}