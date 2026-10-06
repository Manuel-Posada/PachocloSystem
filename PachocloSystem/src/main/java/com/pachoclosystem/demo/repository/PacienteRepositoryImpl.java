package com.pachoclosystem.demo.repository;

import com.pachoclosystem.demo.model.Paciente;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class PacienteRepositoryImpl implements IPacienteRepository {

    private final Map<String, Paciente> pacientes = new LinkedHashMap<>();
    private int contadorId = 1;

    @Override
    public synchronized String generarNuevoId() {
        return String.format("PAC-%04d", contadorId++);
    }

    @Override
    public synchronized Paciente buscarPorId(String id) {
        return pacientes.get(id);
    }

    @Override
    public synchronized boolean guardarPaciente(Paciente p) {
        pacientes.put(p.getIdPaciente(), p);
        return true;
    }

    @Override
    public synchronized List<Paciente> obtenerTodos() {
        return new ArrayList<>(pacientes.values());
    }

    @Override
    public synchronized boolean eliminar(String idPaciente) {
        return pacientes.remove(idPaciente) != null;
    }
}
