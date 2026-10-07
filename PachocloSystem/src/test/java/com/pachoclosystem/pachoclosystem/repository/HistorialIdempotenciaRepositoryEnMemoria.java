package com.pachoclosystem.pachoclosystem.repository;

import com.pachoclosystem.pachoclosystem.dto.RegistroResponse;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Doble en memoria de {@link IHistorialIdempotenciaRepository} para los tests unitarios. */
public class HistorialIdempotenciaRepositoryEnMemoria implements IHistorialIdempotenciaRepository {

    private final Map<String, UsoGuardado> usos = new HashMap<>();

    @Override
    public synchronized Optional<UsoGuardado> buscarVigente(String clave, Instant limite) {
        return Optional.ofNullable(usos.get(clave)).filter(u -> u.usadaEn().isAfter(limite));
    }

    @Override
    public synchronized void guardar(String clave, String idUsuario, String idPaciente, String huella,
                                     RegistroResponse registro, Instant usadaEn) {
        usos.put(clave, new UsoGuardado(idUsuario, idPaciente, huella, registro, usadaEn));
    }

    @Override
    public synchronized int purgarCaducadas(Instant limite) {
        int antes = usos.size();
        usos.values().removeIf(u -> !u.usadaEn().isAfter(limite));
        return antes - usos.size();
    }

    @Override
    public synchronized long contarVigentes(Instant limite) {
        return usos.values().stream().filter(u -> u.usadaEn().isAfter(limite)).count();
    }
}
