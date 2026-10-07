package com.pachoclosystem.pachoclosystem.repository;

import com.pachoclosystem.pachoclosystem.model.Paciente;
import com.pachoclosystem.pachoclosystem.model.RegistroClinico;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Doble en memoria de {@link IRegistroClinicoRepository} para los tests
 * unitarios de servicios. Lee los pacientes de un {@link IPacienteRepository}
 * para el nombre y la baja, como el JOIN de la implementación JDBC.
 */
public class RegistroClinicoRepositoryEnMemoria implements IRegistroClinicoRepository {

    private record Fila(String idPaciente, RegistroClinico registro) {
    }

    private final IPacienteRepository pacientes;
    private final List<Fila> filas = new ArrayList<>();

    public RegistroClinicoRepositoryEnMemoria(IPacienteRepository pacientes) {
        this.pacientes = pacientes;
    }

    @Override
    public synchronized void insertar(String idPaciente, RegistroClinico registro) {
        filas.add(new Fila(idPaciente, registro));
    }

    @Override
    public synchronized List<RegistroClinico> listarPorPaciente(String idPaciente) {
        return filas.stream()
                .filter(f -> f.idPaciente().equals(idPaciente))
                .map(Fila::registro)
                .toList();
    }

    @Override
    public synchronized List<RegistroDePaciente> listarDePacientesActivos() {
        List<Paciente> orden = pacientes.obtenerTodos();
        List<RegistroDePaciente> resultado = new ArrayList<>();
        for (Paciente paciente : orden) {
            if (!paciente.isActivo()) {
                continue;
            }
            for (Fila fila : filas) {
                if (fila.idPaciente().equals(paciente.getIdPaciente())) {
                    resultado.add(new RegistroDePaciente(paciente.getIdPaciente(), paciente.getNombre(),
                            fila.registro()));
                }
            }
        }
        // Por fecha; a igual fecha, por orden del paciente y del registro (orden estable).
        resultado.sort(Comparator.comparing(r -> r.registro().getFecha()));
        return resultado;
    }
}
