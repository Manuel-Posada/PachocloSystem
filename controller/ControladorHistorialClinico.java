package controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import model.TrabajadorHospital;
import model.IPacienteRepository;
import model.Paciente;
import model.RegistroClinico;
import model.RegistroConPaciente;
import model.TipoRegistro;

public class ControladorHistorialClinico {
    private final IPacienteRepository repositorio;

    public ControladorHistorialClinico(IPacienteRepository repositorio) {
        this.repositorio = repositorio;
    }

    public boolean agregarRegistroPaciente(String idPaciente, TipoRegistro tipo, String contenido, TrabajadorHospital autor) {
        Paciente paciente = repositorio.buscarPorId(idPaciente);
        if (paciente == null) return false;

        RegistroClinico nuevoRegistro = new RegistroClinico(tipo, contenido, autor);
        paciente.agregarRegistro(nuevoRegistro);
        return repositorio.guardarPaciente(paciente);
    }

    public List<RegistroClinico> obtenerHistorialPaciente(String idPaciente) {
        Paciente paciente = repositorio.buscarPorId(idPaciente);
        if (paciente == null) return null;

        return paciente.obtenerHistorial();
    }

    //recorrer todos los pacientes y juntar sus registros
    public List<RegistroConPaciente> obtenerTodosLosRegistros() {
        List<RegistroConPaciente> resultado = new ArrayList<>();
        for (Paciente paciente : repositorio.obtenerTodos()) {
            for (RegistroClinico registro : paciente.obtenerHistorial()) {
                resultado.add(new RegistroConPaciente(
                        paciente.getIdPaciente(), paciente.getNombre(), registro));
            }
        }
        resultado.sort(Comparator.comparing(rc -> rc.getRegistro().getFecha()));
        return resultado;
    }
}