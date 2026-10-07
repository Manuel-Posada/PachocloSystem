package com.pachoclosystem.pachoclosystem.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Usuario de acceso a la API (fase 1: solo modelo, sin autenticación).
 *
 * <p>Guarda <strong>únicamente</strong> el hash BCrypt de la contraseña; la
 * contraseña en claro nunca se almacena ni se muestra. Ni {@code toString()}
 * ni la serialización JSON incluyen el hash.</p>
 *
 * <p>El rol y el trabajador vinculado son mutables, pero solo a través de
 * operaciones con nombre propio ({@link #cambiarRol(Rol, String)}, que valida su
 * coherencia); no hay setters genéricos. {@link #cambiarPassword(String, boolean)}
 * y {@link #desactivar()} incrementan {@link #getVersionToken()}, que es lo que
 * permite revocar los tokens emitidos antes de un cambio de contraseña o de una
 * desactivación. {@link #reactivar()} nunca restaura esa versión: un token
 * emitido antes de la desactivación no vuelve a valer.</p>
 */
public class Usuario {

    /** Formato admitido de username: minúsculas, dígitos, punto, guion bajo o guion. */
    public static final Pattern PATRON_USERNAME = Pattern.compile("^[a-z0-9._-]{3,30}$");

    private final String idUsuario;
    private final String username;
    private volatile String passwordHash;
    private volatile Rol rol;
    private volatile String idTrabajador;
    private volatile boolean activo;
    /** Si es {@code true}, el usuario debe cambiar la contraseña antes de operar. */
    private volatile boolean debeCambiarPassword;
    /** Incrementa en cada cambio de contraseña; los tokens llevan esta versión. */
    private volatile int versionToken;

    public Usuario(String idUsuario, String username, String passwordHash, Rol rol, String idTrabajador) {
        this(idUsuario, username, passwordHash, rol, idTrabajador, false);
    }

    public Usuario(String idUsuario, String username, String passwordHash, Rol rol, String idTrabajador,
                   boolean debeCambiarPassword) {
        this.idUsuario = idUsuario;
        this.username = normalizarUsername(username);
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.idTrabajador = idTrabajador;
        this.activo = true;
        this.debeCambiarPassword = debeCambiarPassword;
        this.versionToken = 0;
    }

    /** El username se guarda siempre en minúsculas (la unicidad es case-insensitive). */
    public static String normalizarUsername(String username) {
        return username == null ? null : username.trim().toLowerCase(Locale.ROOT);
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public String getUsername() {
        return username;
    }

    /**
     * Hash BCrypt de la contraseña. No aparece en {@code toString()} y está
     * excluido de la serialización JSON: no debe salir nunca por la API ni por
     * los logs.
     */
    @JsonIgnore
    public String getPasswordHash() {
        return passwordHash;
    }

    public Rol getRol() {
        return rol;
    }

    /** ID del trabajador vinculado; {@code null} para usuarios administradores. */
    public String getIdTrabajador() {
        return idTrabajador;
    }

    public boolean isActivo() {
        return activo;
    }

    public boolean isDebeCambiarPassword() {
        return debeCambiarPassword;
    }

    public int getVersionToken() {
        return versionToken;
    }

    /**
     * Desactiva el usuario y revoca sus tokens. Idempotente: solo la primera
     * desactivación incrementa {@link #getVersionToken()}; desactivar a un
     * usuario ya inactivo no cambia nada.
     */
    public void desactivar() {
        if (this.activo) {
            this.activo = false;
            this.versionToken = this.versionToken + 1;
        }
    }

    /**
     * Reactiva el usuario. Idempotente: reactivar un usuario activo no hace
     * nada. No restaura la versión del token: los tokens emitidos antes de la
     * desactivación siguen revocados.
     */
    public void reactivar() {
        this.activo = true;
    }

    /**
     * Cambia rol y trabajador vinculado comprobando la coherencia del modelo:
     * un ADMIN no puede tener trabajador y el resto de roles sí requieren uno.
     *
     * <p>La existencia y el tipo del trabajador los valida el servicio: aquí solo
     * se protege el invariante rol/vínculo del propio objeto.</p>
     *
     * @throws IllegalArgumentException si la combinación rol/trabajador es incoherente
     */
    public void cambiarRol(Rol nuevoRol, String idTrabajador) {
        if (nuevoRol == null) {
            throw new IllegalArgumentException("El rol es obligatorio.");
        }
        String trabajador = (idTrabajador == null || idTrabajador.isBlank()) ? null : idTrabajador.trim();
        if (nuevoRol == Rol.ADMIN && trabajador != null) {
            throw new IllegalArgumentException(
                    "El usuario administrador no puede estar vinculado a un trabajador.");
        }
        if (nuevoRol != Rol.ADMIN && trabajador == null) {
            throw new IllegalArgumentException(
                    "El usuario con rol " + nuevoRol + " debe estar vinculado a un trabajador.");
        }
        this.rol = nuevoRol;
        this.idTrabajador = trabajador;
    }

    /**
     * Reemplaza el hash de la contraseña, incrementa la versión de token y fija
     * si el usuario debe cambiarla en el siguiente acceso.
     *
     * @param nuevoHash    hash BCrypt de la nueva contraseña (nunca en claro)
     * @param exigirCambio {@code true} para obligar al usuario a cambiarla
     */
    public void cambiarPassword(String nuevoHash, boolean exigirCambio) {
        if (nuevoHash == null || nuevoHash.isBlank()) {
            throw new IllegalArgumentException("El hash de la contraseña es obligatorio.");
        }
        this.passwordHash = nuevoHash;
        this.versionToken = this.versionToken + 1;
        this.debeCambiarPassword = exigirCambio;
    }

    @Override
    public String toString() {
        return "Usuario{idUsuario='" + idUsuario + "', username='" + username + "', rol=" + rol
                + ", idTrabajador=" + (idTrabajador == null ? "null" : "'" + idTrabajador + "'")
                + ", activo=" + activo + ", debeCambiarPassword=" + debeCambiarPassword
                + ", versionToken=" + versionToken + "}";
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Usuario usuario)) {
            return false;
        }
        return Objects.equals(idUsuario, usuario.idUsuario);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(idUsuario);
    }
}
