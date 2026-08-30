package model;

import java.util.ArrayList;
import java.util.List;

public class TrabajadorRepositoryImpl implements ITrabajadoresRepository {

    private List<TrabajadorHospital> listaTrabajadors;

    public TrabajadorRepositoryImpl() {
        this.listaTrabajadors = new ArrayList<>();
    }

    @Override
    public boolean guardarTrabajador(TrabajadorHospital u) {
        if (u == null) {
            return false;
        }
        return listaTrabajadors.add(u);
    }

    @Override
    public TrabajadorHospital buscarPorId(String id) {
        for (TrabajadorHospital u : listaTrabajadors) {
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
            return listaTrabajadors.remove(u);
        }
        return false;
    }

    @Override
    public List<TrabajadorHospital> obtenerTodos() {
        return listaTrabajadors;
    }
}