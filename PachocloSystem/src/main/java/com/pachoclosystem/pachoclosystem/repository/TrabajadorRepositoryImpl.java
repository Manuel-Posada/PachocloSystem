package com.pachoclosystem.pachoclosystem.repository;

import com.pachoclosystem.pachoclosystem.model.TrabajadorHospital;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class TrabajadorRepositoryImpl implements ITrabajadoresRepository {

    private final Map<String, TrabajadorHospital> trabajadores = new LinkedHashMap<>();
    private final Map<String, Integer> contadoresPorPrefijo = new HashMap<>();

    @Override
    public synchronized String generarNuevoId(String prefijo) {
        int siguiente = contadoresPorPrefijo.getOrDefault(prefijo, 1);
        contadoresPorPrefijo.put(prefijo, siguiente + 1);
        return String.format("%s-%04d", prefijo, siguiente);
    }

    @Override
    public synchronized boolean guardarTrabajador(TrabajadorHospital u) {
        if (u == null) {
            return false;
        }
        trabajadores.put(u.getIdTrabajador(), u);
        return true;
    }

    @Override
    public synchronized TrabajadorHospital buscarPorId(String id) {
        return trabajadores.get(id);
    }

    @Override
    public synchronized boolean eliminarTrabajador(String id) {
        return trabajadores.remove(id) != null;
    }

    @Override
    public synchronized List<TrabajadorHospital> obtenerTodos() {
        return new ArrayList<>(trabajadores.values());
    }
}
