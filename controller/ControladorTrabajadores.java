package controller;

import model.Doctor;
import model.Enfermero;
import model.ITrabajadoresRepository;
import model.NivelExperiencia;
import model.TrabajadorHospital;

import java.util.List;
import java.util.regex.Pattern;

public class ControladorTrabajadores {

    private final ITrabajadoresRepository repositorio;

    private static final Pattern PATRON_ID =
            Pattern.compile("^[A-Za-z0-9\\-]{1,20}$");
    private static final Pattern PATRON_NOMBRE =
            Pattern.compile("^[A-Za-zÁÉÍÓÚÑÜáéíóúñü][A-Za-zÁÉÍÓÚÑÜáéíóúñü\\s]{2,59}$");
    private static final Pattern PATRON_TEXTO_LIBRE =
            Pattern.compile(".*[A-Za-zÁÉÍÓÚÑÜáéíóúñü].*");

    public ControladorTrabajadores(ITrabajadoresRepository repositorio) {
        this.repositorio = repositorio;
    }

    /** Valida y registra un trabajador nuevo. Construye el Doctor/Enfermero internamente. */
    public ResultadoOperacion registrarTrabajador(String idTexto, String nombreTexto, String rol,
            String especialidadTexto, NivelExperiencia nivel) {

        StringBuilder errores = new StringBuilder();
        String id = validarId(idTexto, errores);
        String nombre = validarNombre(nombreTexto, errores);
        TrabajadorHospital nuevo = construirTrabajador(id, nombre, rol, especialidadTexto, nivel, errores);

        if (errores.length() > 0) {
            return ResultadoOperacion.error(errores.toString());
        }
        if (repositorio.buscarPorId(id) != null) {
            return ResultadoOperacion.error("• Ya existe un trabajador registrado con ese ID.");
        }

        boolean ok = repositorio.guardarTrabajador(nuevo);
        return ok
                ? ResultadoOperacion.ok("Trabajador registrado correctamente.")
                : ResultadoOperacion.error("• No se pudo registrar el trabajador.");
    }

    /** Valida y actualiza un trabajador existente. El ID no cambia; el rol tampoco. */
    public ResultadoOperacion editarTrabajador(String idTexto, String nombreTexto, String rol,
            String especialidadTexto, NivelExperiencia nivel) {

        StringBuilder errores = new StringBuilder();
        String id = validarId(idTexto, errores);
        String nombre = validarNombre(nombreTexto, errores);

        if (id != null && repositorio.buscarPorId(id) == null) {
            errores.append("• No se encontró un trabajador con ese ID.<br>");
        }

        TrabajadorHospital actualizado = construirTrabajador(id, nombre, rol, especialidadTexto, nivel, errores);

        if (errores.length() > 0) {
            return ResultadoOperacion.error(errores.toString());
        }

        repositorio.eliminarTrabajador(id);
        boolean ok = repositorio.guardarTrabajador(actualizado);
        return ok
                ? ResultadoOperacion.ok("Trabajador actualizado correctamente.")
                : ResultadoOperacion.error("• No se pudo actualizar el trabajador.");
    }

    /** Valida el ID y elimina al trabajador correspondiente. */
    public ResultadoOperacion eliminarTrabajador(String idTexto) {
        String id = idTexto == null ? "" : idTexto.trim();
        if (id.isEmpty()) {
            return ResultadoOperacion.error("• Debe indicar el ID del trabajador a eliminar.");
        }
        boolean ok = repositorio.eliminarTrabajador(id);
        return ok
                ? ResultadoOperacion.ok("Trabajador eliminado correctamente.")
                : ResultadoOperacion.error("• No se encontró un trabajador con ese ID.");
    }

    public List<TrabajadorHospital> listarTrabajadores() {
        return repositorio.obtenerTodos();
    }

    /** Búsqueda de trabajador por ID exacto (usada por la pantalla "Ver / Buscar"). */
    public TrabajadorHospital buscarTrabajadorPorId(String idTexto) {
        if (idTexto == null || idTexto.isBlank()) {
            return null;
        }
        return repositorio.buscarPorId(idTexto.trim());
    }

    // ------------------------------------------------------------------
    // Validaciones internas: parte del Controlador (capa de negocio),
    // nunca de la Vista.
    // ------------------------------------------------------------------

    private String validarId(String idTexto, StringBuilder errores) {
        String id = idTexto == null ? "" : idTexto.trim();
        if (id.isEmpty()) {
            errores.append("• El ID del trabajador es obligatorio.<br>");
            return null;
        }
        if (!PATRON_ID.matcher(id).matches()) {
            errores.append("• El ID solo puede tener letras, números y guiones (sin espacios).<br>");
            return null;
        }
        return id;
    }

    private String validarNombre(String nombreTexto, StringBuilder errores) {
        String nombre = nombreTexto == null ? "" : nombreTexto.trim();
        if (nombre.isEmpty()) {
            errores.append("• El nombre completo es obligatorio.<br>");
            return null;
        }
        if (!PATRON_NOMBRE.matcher(nombre).matches()) {
            errores.append("• El nombre debe tener solo letras y espacios (3 a 60 caracteres).<br>");
            return null;
        }
        return nombre;
    }

    private TrabajadorHospital construirTrabajador(String id, String nombre, String rol,
            String especialidadTexto, NivelExperiencia nivel, StringBuilder errores) {

        if (rol == null) {
            errores.append("• Debe seleccionar un rol.<br>");
            return null;
        }
        if (id == null || nombre == null) {
            // Ya se registraron los errores de ID/nombre; no seguimos validando el resto.
            return null;
        }

        if ("Doctor".equals(rol)) {
            String especialidad = especialidadTexto == null ? "" : especialidadTexto.trim();
            if (especialidad.isEmpty() || especialidad.length() < 3
                    || !PATRON_TEXTO_LIBRE.matcher(especialidad).matches()) {
                errores.append("• La especialidad debe ser un texto descriptivo (mínimo 3 caracteres).<br>");
                return null;
            }
            return new Doctor(id, nombre, especialidad);
        }

        if ("Enfermero".equals(rol)) {
            if (nivel == null) {
                errores.append("• Debe seleccionar un nivel de experiencia.<br>");
                return null;
            }
            return new Enfermero(id, nombre, nivel);
        }

        errores.append("• Rol de trabajador no soportado.<br>");
        return null;
    }
}