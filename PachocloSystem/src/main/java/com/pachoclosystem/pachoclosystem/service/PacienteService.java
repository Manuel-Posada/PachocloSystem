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

    public Paciente editarPaciente(String id, String nuevoNombre, int nuevaEdad, int nuevaHabitacion) {
        Paciente paciente = obtenerPaciente(id);
        paciente.actualizarDatos(nuevoNombre.trim(), nuevaEdad, nuevaHabitacion);
        repositorio.guardarPaciente(paciente);
        return paciente;
    }

    public Paciente editarHabitacion(String id, int nuevaHabitacion) {
        Paciente paciente = obtenerPaciente(id);
        paciente.actualizarDatos(nuevaHabitacion);
        repositorio.guardarPaciente(paciente);
        return paciente;
    }

    public void eliminarPaciente(String id) {
        Paciente paciente = repositorio.buscarPorId(id);
        if (paciente == null || !paciente.isActivo()) {
            throw noEncontrado(id);
        }
        // Soft delete: el paciente y su historial se conservan en memoria; solo se
        // marca inactivo (idempotente: un segundo DELETE da 404, igual que hoy).
        paciente.desactivar();
        repositorio.guardarPaciente(paciente);
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
