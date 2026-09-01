package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrabajadorRepositoryImpl implements ITrabajadoresRepository {

    private List<TrabajadorHospital> listatrabajadores;
    private Map<String, Integer> contadoresPorPrefijo = new HashMap<>();

    public TrabajadorRepositoryImpl() {
        this.listatrabajadores = new ArrayList<>();
    }

    @Override
    public String generarNuevoId(String prefijo) {
        int siguiente = contadoresPorPrefijo.getOrDefault(prefijo, 1);
        contadoresPorPrefijo.put(prefijo, siguiente + 1);
        return String.format("%s-%04d", prefijo, siguiente);
    }

    @Override
    public boolean guardarTrabajador(TrabajadorHospital u) {
        if (u == null) {
            return false;
        }
        return listatrabajadores.add(u);
    }

    @Override
    public TrabajadorHospital buscarPorId(String id) {
        for (TrabajadorHospital u : listatrabajadores) {
            if (u.getIdTrabajador().equals(id)) {
                return u;
            }
        }
        return null;
    }

    @Override
    public boolean eliminarTrabajador(String id) {
        TrabajadorHospital u = buscarPorId(id);
        if (u != null) {
            return listatrabajadores.remove(u);
        }
        return false;
    }

    @Override
    public List<TrabajadorHospital> obtenerTodos() {
        return listatrabajadores;
    }
}