package controller;

import model.Doctor;
import model.Enfermero;
import model.ITrabajadoresRepository;
import model.NivelExperiencia;
import model.TrabajadorHospital;

import java.util.List;


public class ControladorTrabajadores {

    private ITrabajadoresRepository repositorio;

    public ControladorTrabajadores(ITrabajadoresRepository repositorio) {
        this.repositorio = repositorio;
    }

    // Ahora el controlador genera el ID (con prefijo según el rol) y construye
    // el objeto correcto (Doctor/Enfermero). Devuelve el id generado, o null si falló.
    public String registrarTrabajador(String nombre, String rol, String especialidad, NivelExperiencia nivel) {
        if (nombre == null || rol == null) return null;

        String prefijo = "Doctor".equals(rol) ? "DOC" : "ENF";
        String id = repositorio.generarNuevoId(prefijo);

        TrabajadorHospital trabajador = "Doctor".equals(rol)
                ? new Doctor(id, nombre, especialidad)
                : new Enfermero(id, nombre, nivel);

        boolean ok = repositorio.guardarTrabajador(trabajador);
        return ok ? id : null;
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