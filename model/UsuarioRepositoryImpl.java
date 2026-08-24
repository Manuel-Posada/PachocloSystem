package model;

import java.util.ArrayList;
import java.util.List;

public class UsuarioRepositoryImpl implements IUsuarioRepository {

    private List<TrabajadorHospital> listaUsuarios;

    public UsuarioRepositoryImpl() {
        this.listaUsuarios = new ArrayList<>();
    }

    @Override
    public boolean guardarUsuario(TrabajadorHospital u) {
        if (u == null) {
            return false;
        }
        return listaUsuarios.add(u);
    }

    @Override
    public TrabajadorHospital buscarPorId(String id) {
        for (TrabajadorHospital u : listaUsuarios) {
            if (u.getIdTrabajador().equals(id)) {
                return u;
            }
        }
        return null;
    }

    @Override
    public boolean eliminarUsuario(String id) {
        TrabajadorHospital u = buscarPorId(id);
        if (u != null) {
            return listaUsuarios.remove(u);
        }
        return false;
    }

    @Override
    public List<TrabajadorHospital> obtenerTodos() {
        return listaUsuarios;
    }
}