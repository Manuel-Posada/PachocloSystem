package com.pachoclosystem.pachoclosystem.service;

import com.pachoclosystem.pachoclosystem.exception.ConflictoException;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reglas de negocio de los usuarios.
 *
 * <p>La contraseña se guarda <strong>solo</strong> como hash BCrypt: el valor
 * en claro nunca se almacena ni aparece en mensajes de error ni en logs.</p>
 */
@Service
public class UsuarioService {

    public static final int LONGITUD_MINIMA_PASSWORD = 10;
    /** BCrypt solo admite hasta 72 bytes: con más, {@code encode} lanza una excepción (500). */
    public static final int MAXIMO_BYTES_PASSWORD = 72;

    private final IUsuarioRepository repositorio;
    private final TrabajadorService trabajadorService;
    private final PasswordEncoder passwordEncoder;
    /**
     * Serializa los cambios de estado (activar/desactivar): sin él, dos
     * administradores podrían desactivarse a la vez el uno al otro y dejar el
     * sistema sin ninguno.
     */
    private final Object cerrojoEstados = new Object();

    public UsuarioService(IUsuarioRepository repositorio, TrabajadorService trabajadorService,
                          PasswordEncoder passwordEncoder) {
        this.repositorio = repositorio;
        this.trabajadorService = trabajadorService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Crea un usuario con la contraseña hasheada con BCrypt.
     *
     * <p>Reglas: username válido y único (case-insensitive), contraseña que
     * cumpla la política (ver {@link #validarPassword}), los administradores no se vinculan a un trabajador y
     * los doctores/enfermeros sí, a un trabajador existente del tipo correcto y
     * que todavía no tenga usuario.</p>
     */
    public Usuario crearUsuario(String username, String passwordEnClaro, Rol rol, String idTrabajador) {
        String usernameNormalizado = Usuario.normalizarUsername(username);
        String trabajador = (idTrabajador == null || idTrabajador.isBlank()) ? null : idTrabajador.trim();

        List<String> errores = new ArrayList<>();
        if (usernameNormalizado == null || !Usuario.PATRON_USERNAME.matcher(usernameNormalizado).matches()) {
            errores.add("El username debe tener entre 3 y 30 caracteres y solo puede contener "
                    + "minúsculas, dígitos, punto, guion bajo o guion.");
        }
        errores.addAll(validarPassword(passwordEnClaro, usernameNormalizado));
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
            validarTrabajador(rol, trabajador);
        }

        // Aviso temprano para el caso habitual; la garantía de unicidad sigue siendo
        // atómica dentro de repositorio.guardar() (putIfAbsent), que es quien decide.
        if (repositorio.existePorUsername(usernameNormalizado)) {
            throw usuarioDuplicado(usernameNormalizado);
        }

        String id = repositorio.generarNuevoId();
        Usuario usuario = new Usuario(id, usernameNormalizado, passwordEncoder.encode(passwordEnClaro),
                rol, trabajador);
        if (!repositorio.guardar(usuario)) {
            throw usuarioDuplicado(usernameNormalizado);
        }
        return usuario;
    }

    /**
     * Política de contraseña: de {@value #LONGITUD_MINIMA_PASSWORD} caracteres
     * a {@value #MAXIMO_BYTES_PASSWORD} bytes en UTF-8, y distinta del username
     * (sin distinguir mayúsculas). Sin reglas de composición. Los mensajes
     * nunca incluyen la contraseña.
     */
    static List<String> validarPassword(String passwordEnClaro, String usernameNormalizado) {
        List<String> errores = new ArrayList<>();
        if (passwordEnClaro == null || passwordEnClaro.length() < LONGITUD_MINIMA_PASSWORD) {
            errores.add("La contraseña debe tener al menos " + LONGITUD_MINIMA_PASSWORD + " caracteres.");
            return errores;
        }
        if (passwordEnClaro.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES_PASSWORD) {
            errores.add("La contraseña no puede ocupar más de " + MAXIMO_BYTES_PASSWORD
                    + " bytes (las letras con tilde y la ñ ocupan 2).");
        }
        if (usernameNormalizado != null
                && passwordEnClaro.trim().toLowerCase(Locale.ROOT).equals(usernameNormalizado)) {
            errores.add("La contraseña no puede ser igual al username.");
        }
        return errores;
    }

    /** 409: el username ya pertenece a otro usuario (activo o no). */
    private ConflictoException usuarioDuplicado(String usernameNormalizado) {
        return new ConflictoException(
                "Ya existe un usuario con el username " + usernameNormalizado + ".");
    }

    /** Usuarios activos e inactivos, por ID; {@code texto} filtra por ID, username o trabajador. */
    public List<Usuario> listar(String texto) {
        String filtro = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        return repositorio.listarTodos().stream()
                .filter(u -> filtro.isEmpty()
                        || u.getIdUsuario().toLowerCase(Locale.ROOT).contains(filtro)
                        || u.getUsername().contains(filtro)
                        || (u.getIdTrabajador() != null
                        && u.getIdTrabajador().toLowerCase(Locale.ROOT).contains(filtro)))
                .toList();
    }

    /** Devuelve el usuario; si no existe lanza {@link NotFoundException}. */
    public Usuario obtener(String idUsuario) {
        Usuario usuario = repositorio.buscarPorId(idUsuario);
        if (usuario == null) {
            throw new NotFoundException("No se encontró el usuario " + idUsuario + ".");
        }
        return usuario;
    }

    /**
     * Desactiva un usuario (idempotente). 409 si es el propio usuario que lo
     * pide o el último administrador activo. Sus tokens dejan de valer en la
     * siguiente petición: la autenticación relee el usuario cada vez.
     */
    public Usuario desactivar(String idUsuario, String usernameSolicitante) {
        Usuario usuario = obtener(idUsuario);
        synchronized (cerrojoEstados) {
            if (!usuario.isActivo()) {
                return usuario;
            }
            if (usuario.getUsername().equals(Usuario.normalizarUsername(usernameSolicitante))) {
                throw new ConflictoException("No puede desactivar su propio usuario.");
            }
            if (usuario.getRol() == Rol.ADMIN && contarAdministradoresActivos() <= 1) {
                throw new ConflictoException("No se puede desactivar al último administrador activo.");
            }
            usuario.desactivar();
            return usuario;
        }
    }

    /**
     * Reactiva un usuario (idempotente) con las comprobaciones de consistencia
     * del alta: un doctor o enfermero necesita que su trabajador siga existiendo,
     * sea de su tipo y siga vinculado a él. Si no, 409.
     */
    public Usuario activar(String idUsuario) {
        Usuario usuario = obtener(idUsuario);
        synchronized (cerrojoEstados) {
            if (usuario.isActivo()) {
                return usuario;
            }
            if (usuario.getRol() != Rol.ADMIN) {
                comprobarTrabajadorVigente(usuario);
            }
            usuario.activar();
            return usuario;
        }
    }

    /**
     * Sustituye la contraseña de un usuario (activo o no) por una nueva que
     * cumpla la política. La contraseña en claro no se guarda ni aparece en
     * los mensajes.
     */
    public Usuario restablecerPassword(String idUsuario, String nuevaPasswordEnClaro) {
        Usuario usuario = obtener(idUsuario);
        List<String> errores = validarPassword(nuevaPasswordEnClaro, usuario.getUsername());
        if (!errores.isEmpty()) {
            throw new SolicitudInvalidaException(errores);
        }
        // Los tokens emitidos con la contraseña anterior dejan de valer (ver
        // JwtUsuarioAuthenticationConverter), también los del propio solicitante.
        usuario.cambiarPasswordHash(passwordEncoder.encode(nuevaPasswordEnClaro), Instant.now());
        return usuario;
    }

    private long contarAdministradoresActivos() {
        return repositorio.listarTodos().stream()
                .filter(u -> u.getRol() == Rol.ADMIN && u.isActivo())
                .count();
    }

    private void comprobarTrabajadorVigente(Usuario usuario) {
        String idTrabajador = usuario.getIdTrabajador();
        String motivo = null;
        TrabajadorHospital trabajador = null;
        try {
            trabajador = trabajadorService.obtenerTrabajador(idTrabajador);
        } catch (NotFoundException noExiste) {
            motivo = "su trabajador " + idTrabajador + " ya no existe";
        }
        if (trabajador != null) {
            boolean tipoCorrecto = usuario.getRol() == Rol.DOCTOR
                    ? trabajador instanceof Doctor
                    : trabajador instanceof Enfermero;
            if (!tipoCorrecto) {
                motivo = "su trabajador " + idTrabajador + " ya no es de su rol";
            } else if (repositorio.buscarPorIdTrabajador(idTrabajador) != usuario) {
                motivo = "su trabajador " + idTrabajador + " está vinculado a otro usuario";
            }
        }
        if (motivo != null) {
            throw new ConflictoException(
                    "No se puede activar el usuario " + usuario.getUsername() + ": " + motivo + ".");
        }
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

    /** Valida que el trabajador vinculado exista y sea del tipo que exige el rol. */
    private void validarTrabajador(Rol rol, String idTrabajador) {
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
        if (repositorio.buscarPorIdTrabajador(idTrabajador) != null) {
            // 409: el vínculo es único y no se libera al desactivar (ver activar()).
            throw new ConflictoException(
                    "El trabajador " + idTrabajador + " ya tiene un usuario.");
        }
    }
}
