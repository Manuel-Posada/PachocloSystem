package controller;

import model.Paciente;
import model.IPacienteRepository;

import java.util.List;

public class ControladorPaciente {

    private IPacienteRepository repositorio;

    public ControladorPaciente(IPacienteRepository repositorio) {
        this.repositorio = repositorio;
    }

    // Antes recibía el id por parámetro; ahora lo genera el repositorio.
    // Devuelve el id generado (o null si falló), para que la Vista pueda mostrarlo.
    public String registrarPaciente(String nombre, int edad, int habitacion) {
        if (nombre == null) return null;
        String id = repositorio.generarNuevoId();
        Paciente paciente = new Paciente(id, nombre, edad, habitacion);
        boolean ok = repositorio.guardarPaciente(paciente);
        return ok ? id : null;
    }

    //edición completa: nombre, edad y habitación
    public boolean editarPaciente(String id, String nuevoNombre, int nuevaEdad, int nuevaHabitacion) {
        Paciente paciente = repositorio.buscarPorId(id);
        if (paciente == null) return false;

        paciente.actualizarDatos(nuevoNombre, nuevaEdad, nuevaHabitacion);
        return repositorio.guardarPaciente(paciente);
    }

    public boolean editarPaciente(String id, int nuevaHabitacion) {
        Paciente paciente = repositorio.buscarPorId(id);
        if (paciente == null) return false;

        paciente.actualizarDatos(nuevaHabitacion);
        return repositorio.guardarPaciente(paciente);
    }

    public boolean eliminarPaciente(String id) {
    return repositorio.eliminar(id);
}

    public boolean buscarPorId(String id) {
        return repositorio.buscarPorId(id) != null;
    }

    public List<Paciente> listarPacientes() {
        return repositorio.obtenerTodos();
    }
}