package com.PachocloSystem.PachocloSystem.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.PachocloSystem.PachocloSystem.model.Paciente;

@Repository
public class PacienteRepositoryImpl implements IPacienteRepository {

    private final List<Paciente> listaPacientes = new ArrayList<>();

    @Override
    public Paciente guardar(Paciente paciente) {
        listaPacientes.add(paciente);
        return paciente;
    }

    @Override
    public Optional<Paciente> obtenerPorId(String id) {
        return listaPacientes.stream()
                .filter(p -> p.getIdPaciente().equalsIgnoreCase(id))
                .findFirst();
    }

    @Override
    public Paciente actualizar(Paciente paciente) {
        obtenerPorId(paciente.getIdPaciente()).ifPresent(p -> {
            p.setNombre(paciente.getNombre());
            p.setEdad(paciente.getEdad());
            p.setHabitacion(paciente.getHabitacion());
        });
        return paciente;
    }

    @Override
    public List<Paciente> obtenerTodos() {
        return listaPacientes;
    }

    @Override
    public boolean eliminarPorId(String id) {
        return listaPacientes.removeIf(p -> p.getIdPaciente().equalsIgnoreCase(id));
    }
}
