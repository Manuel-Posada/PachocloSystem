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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class TrabajadorService {

    private static final Pattern PATRON_CONTIENE_TEXTO = Pattern.compile(".*[A-Za-zÁÉÍÓÚÑÜáéíóúñü].*");

    private final ITrabajadoresRepository repositorio;

    /**
     * Repositorio de usuarios para la cascada de desactivación. Se inyecta por
     * constructor y es obligatorio: sin él no hay cascada posible y el servicio
     * no debe poder construirse.
     */
    private final IUsuarioRepository usuarioRepository;

    public TrabajadorService(ITrabajadoresRepository repositorio, IUsuarioRepository usuarioRepository) {
        this.repositorio = Objects.requireNonNull(repositorio,
                "El repositorio de trabajadores es obligatorio.");
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository,
                "El repositorio de usuarios es obligatorio para la cascada de desactivación.");
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
        // La cascada se ejecuta ANTES del borrado: si desactivar falla, el
        // trabajador no se elimina y la excepción se propaga (no hay borrado
        // parcial). Si el trabajador no existe, la cascada no encuentra usuario
        // y el borrado devuelve false, que se traduce en el 404 de siempre.
        desactivarUsuarioVinculado(id);
        if (!repositorio.eliminarTrabajador(id)) {
            throw noEncontrado(id);
        }
    }

    /**
     * Cascada de desactivación: al eliminar un trabajador, su usuario
     * vinculado (si lo tiene) queda {@code activo=false} y su versión de
     * token se incrementa, de modo que sus tokens dejan de ser válidos de
     * inmediato y no vuelven a valer aunque alguien intente reactivar la
     * cuenta (el trabajador ya no existe, así que la reactivación se
     * rechaza). Si el trabajador no tiene usuario, no se toca nada.
     */
    private void desactivarUsuarioVinculado(String idTrabajador) {
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
