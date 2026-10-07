package com.pachoclosystem.pachoclosystem.service;

import com.pachoclosystem.pachoclosystem.exception.NotFoundException;
import com.pachoclosystem.pachoclosystem.exception.SolicitudInvalidaException;
import com.pachoclosystem.pachoclosystem.model.Doctor;
import com.pachoclosystem.pachoclosystem.model.Enfermero;
import com.pachoclosystem.pachoclosystem.model.Rol;
import com.pachoclosystem.pachoclosystem.model.TrabajadorHospital;
import com.pachoclosystem.pachoclosystem.model.Usuario;
import com.pachoclosystem.pachoclosystem.repository.IUsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Reglas de negocio de los usuarios.
 *
 * <p>La contraseña se guarda <strong>solo</strong> como hash BCrypt: el valor
 * en claro nunca se almacena ni aparece en mensajes de error ni en logs.</p>
 *
 * <p>Las operaciones que pueden violar el invariante «siempre queda al menos un
 * ADMIN activo» (cambio de rol y cambio de estado) se ejecutan dentro de una
 * sección crítica con un {@link ReentrantLock}: la comprobación y la
 * modificación son atómicas entre sí. Se usa un lock explícito (y no
 * {@code synchronized} sobre el repositorio) para no mezclar el bloqueo con las
 * estructuras concurrentes del repositorio, que ya resuelven la unicidad de
 * username/trabajador por su cuenta.</p>
 */
@Service
public class UsuarioService {

    public static final int LONGITUD_MINIMA_PASSWORD = 10;

    /** BCrypt solo considera los primeros 72 bytes de la contraseña. */
    public static final int LONGITUD_MAXIMA_PASSWORD_BYTES = 72;

    private final IUsuarioRepository repositorio;
    private final TrabajadorService trabajadorService;
    private final PasswordEncoder passwordEncoder;

    /** Serializa el cambio de rol y de estado para mantener el último ADMIN activo. */
    private final ReentrantLock cerrojoAdministracion = new ReentrantLock();

    public UsuarioService(IUsuarioRepository repositorio, TrabajadorService trabajadorService,
                          PasswordEncoder passwordEncoder) {
        this.repositorio = repositorio;
        this.trabajadorService = trabajadorService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Crea un usuario con la contraseña hasheada con BCrypt. Las altas directas
     * (bootstrap y tests) no obligan a cambiar la contraseña; las altas por API
     * usan la sobrecarga con {@code debeCambiarPassword=true}.
     */
    public Usuario crearUsuario(String username, String passwordEnClaro, Rol rol, String idTrabajador) {
        return crearUsuario(username, passwordEnClaro, rol, idTrabajador, false);
    }

    /**
     * Crea un usuario con la contraseña hasheada con BCrypt.
     *
     * <p>Reglas: username válido y único (case-insensitive), contraseña de entre
     * 10 caracteres y 72 bytes UTF-8, los administradores no se vinculan a un
     * trabajador y los doctores/enfermeros sí, a un trabajador existente del
     * tipo correcto y que todavía no tenga usuario.</p>
     */
    public Usuario crearUsuario(String username, String passwordEnClaro, Rol rol, String idTrabajador,
                                boolean debeCambiarPassword) {
        String usernameNormalizado = Usuario.normalizarUsername(username);
        String trabajador = (idTrabajador == null || idTrabajador.isBlank()) ? null : idTrabajador.trim();

        List<String> errores = new ArrayList<>();
        if (usernameNormalizado == null || !Usuario.PATRON_USERNAME.matcher(usernameNormalizado).matches()) {
            errores.add("El username debe tener entre 3 y 30 caracteres y solo puede contener "
                    + "minúsculas, dígitos, punto, guion bajo o guion.");
        }
        validarPoliticaPassword(passwordEnClaro, errores);
        if (rol == null) {
            errores.add("Debe seleccionar un rol (ADMIN, DOCTOR o ENFERMERO).");
        } else if (rol == Rol.ADMIN && trabajador != null) {
            errores.add("El usuario administrador no puede estar vinculado a un trabajador.");
        } else if (trabajador == null && rol != Rol.ADMIN) {
            errores.add("El usuario con rol " + rol + " debe estar vinculado a un trabajador.");
        }
        if (!errores.isEmpty()) {
            throw new SolicitudInvalidaException(errores);
        }

        if (rol != Rol.ADMIN) {
            validarTrabajador(rol, trabajador, null);
        }

        // Aviso temprano para el caso habitual; la garantía de unicidad sigue siendo
        // atómica dentro de repositorio.guardar() (putIfAbsent), que es quien decide.
        if (repositorio.existePorUsername(usernameNormalizado)) {
            throw usuarioDuplicado(usernameNormalizado);
        }

        String id = repositorio.generarNuevoId();
        Usuario usuario = new Usuario(id, usernameNormalizado, passwordEncoder.encode(passwordEnClaro),
                rol, trabajador, debeCambiarPassword);
        if (!repositorio.guardar(usuario)) {
            throw usuarioDuplicado(usernameNormalizado);
        }
        return usuario;
    }

    /** Todos los usuarios, ordenados por ID. */
    public List<Usuario> listar() {
        return repositorio.listarTodos();
    }

    /** Devuelve el usuario; si no existe lanza {@link NotFoundException}. */
    public Usuario buscarPorId(String id) {
        Usuario usuario = repositorio.buscarPorId(id);
        if (usuario == null) {
            throw new NotFoundException("No se encontró el usuario " + id + ".");
        }
        return usuario;
    }

    /** Devuelve el usuario; si no existe lanza {@link NotFoundException}. */
    public Usuario buscarPorUsername(String username) {
        Usuario usuario = repositorio.buscarPorUsername(username);
        if (usuario == null) {
            throw new NotFoundException("No se encontró el usuario "
                    + Usuario.normalizarUsername(username) + ".");
        }
        return usuario;
    }

    /**
     * Cambia el rol y el trabajador vinculado de un usuario, con las mismas
     * reglas de vínculo que el alta. Un ADMIN no puede degradar al último ADMIN
     * activo. La comprobación y el cambio son atómicos (sección crítica).
     */
    public Usuario cambiarRol(String idUsuario, Rol nuevoRol, String idTrabajador) {
        cerrojoAdministracion.lock();
        try {
            Usuario usuario = buscarPorId(idUsuario);
            String trabajador = (idTrabajador == null || idTrabajador.isBlank()) ? null : idTrabajador.trim();

            List<String> errores = new ArrayList<>();
            if (nuevoRol == null) {
                errores.add("Debe seleccionar un rol (ADMIN, DOCTOR o ENFERMERO).");
            } else if (nuevoRol == Rol.ADMIN && trabajador != null) {
                errores.add("El usuario administrador no puede estar vinculado a un trabajador.");
            } else if (nuevoRol != Rol.ADMIN && trabajador == null) {
                errores.add("El usuario con rol " + nuevoRol + " debe estar vinculado a un trabajador.");
            }
            if (!errores.isEmpty()) {
                throw new SolicitudInvalidaException(errores);
            }

            if (nuevoRol != Rol.ADMIN) {
                validarTrabajador(nuevoRol, trabajador, idUsuario);
            }
            if (usuario.getRol() == Rol.ADMIN && nuevoRol != Rol.ADMIN
                    && esElUltimoAdminActivo(idUsuario)) {
                throw new SolicitudInvalidaException(
                        "No se puede degradar al último administrador activo.");
            }
            if (!repositorio.cambiarRol(idUsuario, nuevoRol, trabajador)) {
                throw new SolicitudInvalidaException(
                        "El trabajador " + trabajador + " ya tiene un usuario.");
            }
            return usuario;
        } finally {
            cerrojoAdministracion.unlock();
        }
    }

    /**
     * Activa o desactiva un usuario. Un ADMIN no puede desactivarse a sí mismo
     * ni dejar al sistema sin ningún ADMIN activo. Desactivar incrementa la
     * versión del token (revocación inmediata y permanente: reactivar no la
     * deshace) y reactivar una cuenta cuyo trabajador ya no existe se rechaza.
     * La operación es idempotente.
     */
    public Usuario cambiarEstado(String idUsuario, boolean activo, String idActor) {
        cerrojoAdministracion.lock();
        try {
            Usuario usuario = buscarPorId(idUsuario);
            if (activo) {
                if (usuario.isActivo()) {
                    return usuario;
                }
                if (usuario.getIdTrabajador() != null && !existeTrabajador(usuario.getIdTrabajador())) {
                    throw new SolicitudInvalidaException("No se puede reactivar la cuenta: el trabajador "
                            + usuario.getIdTrabajador() + " ya no existe.");
                }
                usuario.reactivar();
                return usuario;
            }
            if (!usuario.isActivo()) {
                return usuario;
            }
            if (usuario.getIdUsuario().equals(idActor)) {
                throw new SolicitudInvalidaException("Un administrador no puede desactivarse a sí mismo.");
            }
            if (usuario.getRol() == Rol.ADMIN && esElUltimoAdminActivo(idUsuario)) {
                throw new SolicitudInvalidaException(
                        "No se puede desactivar al último administrador activo.");
            }
            usuario.desactivar();
            return usuario;
        } finally {
            cerrojoAdministracion.unlock();
        }
    }

    /**
     * Cambio de contraseña del propio usuario: verifica la actual, exige que la
     * nueva difiera, aplica la política e incrementa la versión del token
     * (revocando los tokens anteriores) sin dejar cambio pendiente.
     */
    public Usuario cambiarPasswordPropia(String idUsuario, String passwordActual, String passwordNueva) {
        Usuario usuario = buscarPorId(idUsuario);
        if (passwordActual == null || !passwordEncoder.matches(passwordActual, usuario.getPasswordHash())) {
            // Mensaje genérico: nunca reproduce la contraseña.
            throw new SolicitudInvalidaException("La contraseña actual no es correcta.");
        }
        if (passwordNueva != null && passwordNueva.equals(passwordActual)) {
            throw new SolicitudInvalidaException("La nueva contraseña debe ser diferente de la actual.");
        }
        List<String> errores = new ArrayList<>();
        validarPoliticaPassword(passwordNueva, errores);
        if (!errores.isEmpty()) {
            throw new SolicitudInvalidaException(errores);
        }
        usuario.cambiarPassword(passwordEncoder.encode(passwordNueva), false);
        return usuario;
    }

    /**
     * Reset administrativo de la contraseña de cualquier usuario: aplica la
     * política e incrementa la versión del token, dejando activado el cambio
     * obligatorio.
     */
    public Usuario resetearPassword(String idUsuario, String passwordNueva) {
        Usuario usuario = buscarPorId(idUsuario);
        List<String> errores = new ArrayList<>();
        validarPoliticaPassword(passwordNueva, errores);
        if (!errores.isEmpty()) {
            throw new SolicitudInvalidaException(errores);
        }
        usuario.cambiarPassword(passwordEncoder.encode(passwordNueva), true);
        return usuario;
    }

    private SolicitudInvalidaException usuarioDuplicado(String usernameNormalizado) {
        return new SolicitudInvalidaException(
                "Ya existe un usuario con el username " + usernameNormalizado + ".");
    }

    /**
     * Añade a {@code errores} los que incumplen la política de contraseña:
     * mínimo 10 caracteres y máximo 72 bytes UTF-8, sin reglas de composición.
     * Nunca incluye la contraseña en el mensaje.
     */
    private static void validarPoliticaPassword(String password, List<String> errores) {
        if (password == null || password.length() < LONGITUD_MINIMA_PASSWORD) {
            errores.add("La contraseña debe tener al menos " + LONGITUD_MINIMA_PASSWORD + " caracteres.");
        } else if (password.getBytes(StandardCharsets.UTF_8).length > LONGITUD_MAXIMA_PASSWORD_BYTES) {
            errores.add("La contraseña no puede superar los "
                    + LONGITUD_MAXIMA_PASSWORD_BYTES + " bytes (caracteres UTF-8).");
        }
    }

    /**
     * Valida que el trabajador vinculado exista y sea del tipo que exige el rol,
     * y que no esté ya vinculado a <em>otro</em> usuario.
     *
     * @param idUsuarioPropietario usuario que puede quedarse con el trabajador
     *                             (en el alta es {@code null}, porque aún no existe)
     */
    private void validarTrabajador(Rol rol, String idTrabajador, String idUsuarioPropietario) {
        // Reutiliza el servicio existente: si no existe, lanza NotFoundException
        // con su mensaje habitual ("No se encontró el trabajador X.").
        TrabajadorHospital trabajador = trabajadorService.obtenerTrabajador(idTrabajador);

        if (rol == Rol.DOCTOR && !(trabajador instanceof Doctor)) {
            throw new SolicitudInvalidaException(
                    "El trabajador " + idTrabajador + " no es un Doctor.");
        }
        if (rol == Rol.ENFERMERO && !(trabajador instanceof Enfermero)) {
            throw new SolicitudInvalidaException(
                    "El trabajador " + idTrabajador + " no es un Enfermero.");
        }
        Usuario ocupante = repositorio.buscarPorIdTrabajador(idTrabajador);
        if (ocupante != null && !ocupante.getIdUsuario().equals(idUsuarioPropietario)) {
            throw new SolicitudInvalidaException(
                    "El trabajador " + idTrabajador + " ya tiene un usuario.");
        }
    }

    /** ¿Es el único ADMIN activo que queda (excluyendo al indicado)? */
    private boolean esElUltimoAdminActivo(String idUsuarioExcluido) {
        return repositorio.listarTodos().stream()
                .filter(u -> u.getRol() == Rol.ADMIN && u.isActivo())
                .noneMatch(u -> !u.getIdUsuario().equals(idUsuarioExcluido));
    }

    private boolean existeTrabajador(String idTrabajador) {
        try {
            trabajadorService.obtenerTrabajador(idTrabajador);
            return true;
        } catch (NotFoundException noExiste) {
            return false;
        }
    }
}
