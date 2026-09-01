package model;

import java.util.ArrayList;
import java.util.List;

public class PacienteRepositoryImpl implements IPacienteRepository {

    private List<Paciente> listaPacientes;
    private int contadorId = 1;

    public PacienteRepositoryImpl() {
        this.listaPacientes = new ArrayList<>();
    }

    @Override
    public String generarNuevoId() {
        return String.format("PAC-%04d", contadorId++);
    }

    @Override
    public Paciente buscarPorId(String id) {
        for (Paciente p : listaPacientes) {
            if (p.getIdPaciente().equals(id)) {
                return p;
            }
        }
        return null;
    }

    @Override
    public boolean guardarPaciente(Paciente p) {
        Paciente existente = buscarPorId(p.getIdPaciente());
        if (existente != null) {
            listaPacientes.remove(existente);
        }
        listaPacientes.add(p);
        return true;
    }

    @Override
    public List<Paciente> obtenerTodos() {
        return listaPacientes;
    }

    @Override
    public boolean eliminar(String idPaciente) {
        Paciente paciente = buscarPorId(idPaciente);
        if (paciente == null) return false;
        return listaPacientes.remove(paciente);
    }
}