package controller;

import model.Paciente;
import model.RegistroClinico;
import model.TipoRegistro;
import model.IPacienteRepository;

import java.util.List;

public class ControladorPaciente {

    private IPacienteRepository repositorio;

    public ControladorPaciente(IPacienteRepository repositorio) {
        this.repositorio = repositorio;
    }

    public boolean registrarPaciente(String id, String nombre, int edad, int habitacion) {
        Paciente paciente = new Paciente(id, nombre, edad, habitacion);
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

    // Opcional: solo si necesitas validar existencia sin traer el historial completo
    public boolean buscarPorId(String id) {
        return repositorio.buscarPorId(id) != null;
    }

    public List<Paciente> listarPacientes() {
        return repositorio.obtenerTodos();
    }
}