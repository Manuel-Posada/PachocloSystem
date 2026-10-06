package com.pachoclosystem.pachoclosystem.service;

import com.pachoclosystem.pachoclosystem.exception.NotFoundException;
import com.pachoclosystem.pachoclosystem.exception.SolicitudInvalidaException;
import com.pachoclosystem.pachoclosystem.model.Doctor;
import com.pachoclosystem.pachoclosystem.model.Enfermero;
import com.pachoclosystem.pachoclosystem.model.NivelExperiencia;
import com.pachoclosystem.pachoclosystem.model.TrabajadorHospital;
import com.pachoclosystem.pachoclosystem.model.Usuario;
import com.pachoclosystem.pachoclosystem.repository.ITrabajadoresRepository;
import com.pachoclosystem.pachoclosystem.repository.IUsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class TrabajadorService {

    private static final Pattern PATRON_CONTIENE_TEXTO = Pattern.compile(".*[A-Za-zÁÉÍÓÚÑÜáéíóúñü].*");

    private final ITrabajadoresRepository repositorio;

    /**
     * Repositorio de usuarios para la cascada de desactivación. Se inyecta por
     * setter (no por constructor) para no acoplar {@link TrabajadorService} con
     * {@link UsuarioService} (el servicio de usuarios ya depende de este
     * servicio) y para conservar el constructor de un solo argumento que usan
     * los tests unitarios; Spring inyecta el repositorio en la aplicación.
     */
    private IUsuarioRepository usuarioRepository;

    public TrabajadorService(ITrabajadoresRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Autowired
    public void setUsuarioRepository(IUsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // El servicio genera el ID (con prefijo según el rol) y construye el objeto correcto.
    public TrabajadorHospital registrarTrabajador(String nombre, String rol, String especialidad,
                                                  NivelExperiencia nivel) {
        validarDatosDeRol(rol, especialidad, nivel);

        String prefijo = "Doctor".equals(rol) ? "DOC" : "ENF";
        String id = repositorio.generarNuevoId(prefijo);

        TrabajadorHospital trabajador = "Doctor".equals(rol)
                ? new Doctor(id, nombre.trim(), especialidad.trim())
                : new Enfermero(id, nombre.trim(), nivel);

        repositorio.guardarTrabajador(trabajador);
        return trabajador;
    }

    // En edición el id y el rol no cambian; se actualiza el mismo objeto.
    public TrabajadorHospital editarTrabajador(String id, String nombre, String rol, String especialidad,
                                               NivelExperiencia nivel) {
        TrabajadorHospital existente = obtenerTrabajador(id);

        String rolActual = existente instanceof Doctor ? "Doctor" : "Enfermero";
        if (!rolActual.equals(rol)) {
            throw new SolicitudInvalidaException("El rol de un trabajador no puede cambiar (actual: " + rolActual + ").");
        }
        validarDatosDeRol(rol, especialidad, nivel);

        existente.setNombreCompleto(nombre.trim());
        if (existente instanceof Doctor d) {
            d.setEspecialidad(especialidad.trim());
        } else if (existente instanceof Enfermero e) {
            e.setNivelExperiencia(nivel);
        }
        repositorio.guardarTrabajador(existente);
        return existente;
    }

    public void eliminarTrabajador(String id) {
        if (!repositorio.eliminarTrabajador(id)) {
            throw noEncontrado(id);
        }
        desactivarUsuarioVinculado(id);
    }

    /**
     * Cascada de desactivación: al eliminar un trabajador, su usuario
     * vinculado (si lo tiene) queda {@code activo=false} para que sus tokens
     * dejen de ser válidos. Si el trabajador no tiene usuario, no se toca nada.
     */
    private void desactivarUsuarioVinculado(String idTrabajador) {
        if (usuarioRepository == null) {
            // Construcción unitaria sin Spring (tests): sin repositorio de
            // usuarios no hay cascada que aplicar.
            return;
        }
        Usuario usuario = usuarioRepository.buscarPorIdTrabajador(idTrabajador);
        if (usuario != null) {
            // desactivar() es idempotente; el objeto vive en el mapa del
            // repositorio, así que la desactivación queda persistida en memoria.
            usuario.desactivar();
        }
    }

    public TrabajadorHospital obtenerTrabajador(String id) {
        TrabajadorHospital t = repositorio.buscarPorId(id);
        if (t == null) {
            throw noEncontrado(id);
        }
        return t;
    }

    /** Lista trabajadores; si hay texto, filtra por id o nombre (sin distinguir mayúsculas). */
    public List<TrabajadorHospital> listarTrabajadores(String texto) {
        String filtro = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        return repositorio.obtenerTodos().stream()
                .filter(t -> filtro.isEmpty()
                        || t.getNombreCompleto().toLowerCase(Locale.ROOT).contains(filtro)
                        || t.getIdTrabajador().toLowerCase(Locale.ROOT).contains(filtro))
                .toList();
    }

    private void validarDatosDeRol(String rol, String especialidad, NivelExperiencia nivel) {
        List<String> errores = new ArrayList<>();
        if ("Doctor".equals(rol)) {
            String esp = especialidad == null ? "" : especialidad.trim();
            if (esp.isEmpty()) {
                errores.add("La especialidad es obligatoria.");
            } else if (esp.length() < 3 || !PATRON_CONTIENE_TEXTO.matcher(esp).matches()) {
                errores.add("La especialidad debe ser un texto descriptivo (mínimo 3 caracteres).");
            }
        } else if ("Enfermero".equals(rol)) {
            if (nivel == null) {
                errores.add("Debe seleccionar un nivel de experiencia.");
            }
        } else {
            errores.add("Debe seleccionar un rol (Doctor o Enfermero).");
        }
        if (!errores.isEmpty()) {
            throw new SolicitudInvalidaException(errores);
        }
    }

    private NotFoundException noEncontrado(String id) {
        return new NotFoundException("No se encontró el trabajador " + id + ".");
    }
}
