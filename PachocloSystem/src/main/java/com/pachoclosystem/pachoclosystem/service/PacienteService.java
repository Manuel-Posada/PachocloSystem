package com.pachoclosystem.pachoclosystem.service;

import com.pachoclosystem.pachoclosystem.exception.NotFoundException;
import com.pachoclosystem.pachoclosystem.model.Paciente;
import com.pachoclosystem.pachoclosystem.repository.IPacienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class PacienteService {

    private final IPacienteRepository repositorio;

    public PacienteService(IPacienteRepository repositorio) {
        this.repositorio = repositorio;
    }

    public Paciente registrarPaciente(String nombre, int edad, int habitacion) {
        String id = repositorio.generarNuevoId();
        Paciente paciente = new Paciente(id, nombre.trim(), edad, habitacion);
        repositorio.guardarPaciente(paciente);
        return paciente;
    }

    /** Cada modificación es atómica en el repositorio y solo se aplica a pacientes activos (si no, 404). */
    public Paciente editarPaciente(String id, String nuevoNombre, int nuevaEdad, int nuevaHabitacion) {
        if (!repositorio.actualizarDatos(id, nuevoNombre.trim(), nuevaEdad, nuevaHabitacion)) {
            throw noEncontrado(id);
        }
        return obtenerPaciente(id);
    }

    public Paciente editarHabitacion(String id, int nuevaHabitacion) {
        if (!repositorio.actualizarHabitacion(id, nuevaHabitacion)) {
            throw noEncontrado(id);
        }
        return obtenerPaciente(id);
    }

    /**
     * Soft delete: el paciente y su historial se conservan; solo se marca
     * inactivo. Un segundo DELETE, también si llega a la vez, da 404.
     */
    public void eliminarPaciente(String id) {
        if (!repositorio.darDeBaja(id)) {
            throw noEncontrado(id);
        }
    }

    public Paciente obtenerPaciente(String id) {
        Paciente paciente = repositorio.buscarPorId(id);
        if (paciente == null || !paciente.isActivo()) {
            throw noEncontrado(id);
        }
        return paciente;
    }

    /** Lista pacientes activos; si hay texto, filtra por id o nombre (sin distinguir mayúsculas). */
    public List<Paciente> listarPacientes(String texto) {
        String filtro = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        return repositorio.obtenerTodos().stream()
                .filter(Paciente::isActivo)
                .filter(p -> filtro.isEmpty()
                        || p.getIdPaciente().toLowerCase(Locale.ROOT).contains(filtro)
                        || p.getNombre().toLowerCase(Locale.ROOT).contains(filtro))
                .toList();
    }

    private NotFoundException noEncontrado(String id) {
        return new NotFoundException("No se encontró el paciente " + id + ".");
    }
}
