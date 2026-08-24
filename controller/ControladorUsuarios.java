package controller;

import model.IUsuarioRepository;
import model.TrabajadorHospital;

import java.util.List;


public class ControladorUsuarios {

    private IUsuarioRepository repositorio;

    public ControladorUsuarios(IUsuarioRepository repositorio) {
        this.repositorio = repositorio;
    }


    public boolean registrarUsuario(String id, String nombre, TrabajadorHospital rolEspecifico) {
        if (rolEspecifico == null || id == null || nombre == null) {
            return false;
        }
        if (!id.equals(rolEspecifico.getIdTrabajador()) || !nombre.equals(rolEspecifico.getNombreCompleto())) {
            return false;
        }
        if (repositorio.buscarPorId(id) != null) {
            return false;
        }
        return repositorio.guardarUsuario(rolEspecifico);
    }

    public boolean editarUsuario(String id, String nombre, TrabajadorHospital rolEspecifico) {
        if (rolEspecifico == null || id == null) {
            return false;
        }
        TrabajadorHospital existente = repositorio.buscarPorId(id);
        if (existente == null) {
            return false;
        }
        repositorio.eliminarUsuario(id);
        return repositorio.guardarUsuario(rolEspecifico);
    }

    public boolean eliminarUsuario(String id) {
        if (id == null) {
            return false;
        }
        return repositorio.eliminarUsuario(id);
    }

    public List<TrabajadorHospital> listarUsuarios() {
        return repositorio.obtenerTodos();
    }

    public TrabajadorHospital buscarUsuarioPorId(String id) {
        if (id == null) {
            return null;
        }
        return repositorio.buscarPorId(id);
    }
}