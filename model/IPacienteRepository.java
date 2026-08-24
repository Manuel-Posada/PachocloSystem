package model;

import java.util.List;

public interface IPacienteRepository {
    Paciente buscarPorId(String id);
    boolean guardarPaciente(Paciente p);
    List<Paciente> obtenerTodos();
    boolean eliminar(String idPaciente);
}