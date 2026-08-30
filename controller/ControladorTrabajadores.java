package controller;

import model.ITrabajadoresRepository;
import model.TrabajadorHospital;

import java.util.List;


public class ControladorTrabajadores {

    private ITrabajadoresRepository repositorio;

    public ControladorTrabajadores(ITrabajadoresRepository repositorio) {
        this.repositorio = repositorio;
    }


    public boolean registrarTrabajador(String id, String nombre, TrabajadorHospital rolEspecifico) {
        if (rolEspecifico == null || id == null || nombre == null) {
            return false;
        }
        if (!id.equals(rolEspecifico.getIdTrabajador()) || !nombre.equals(rolEspecifico.getNombreCompleto())) {
            return false;
        }
        if (repositorio.buscarPorId(id) != null) {
            return false;
        }
        return repositorio.guardarTrabajador(rolEspecifico);
    }

    public boolean editarTrabajador(String id, String nombre, TrabajadorHospital rolEspecifico) {
        if (rolEspecifico == null || id == null) {
            return false;
        }
        TrabajadorHospital existente = repositorio.buscarPorId(id);
        if (existente == null) {
            return false;
        }
        repositorio.eliminarTrabajador(id);
        return repositorio.guardarTrabajador(rolEspecifico);
    }

    public boolean eliminarTrabajador(String id) {
        if (id == null) {
            return false;
        }
        return repositorio.eliminarTrabajador(id);
    }

    public List<TrabajadorHospital> listarTrabajadores() {
        return repositorio.obtenerTodos();
    }

    public TrabajadorHospital buscarTrabajadorPorId(String id) {
        if (id == null) {
            return null;
        }
        return repositorio.buscarPorId(id);
    }
}