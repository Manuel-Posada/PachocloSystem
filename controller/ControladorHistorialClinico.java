package controller;

import java.util.List;
import model.TrabajadorHospital;
import model.IPacienteRepository;
import model.Paciente;
import model.RegistroClinico;
import model.TipoRegistro;

public class ControladorHistorialClinico {
    private IPacienteRepository repositorio;
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
}
