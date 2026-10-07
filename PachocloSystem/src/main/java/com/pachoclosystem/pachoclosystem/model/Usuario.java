package com.pachoclosystem.pachoclosystem.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Usuario de acceso a la API.
 *
 * <p>Guarda <strong>únicamente</strong> el hash BCrypt de la contraseña; la
 * contraseña en claro nunca se almacena ni se muestra. Ni {@code toString()}
 * ni la serialización JSON incluyen el hash.</p>
 *
 * <p>El hash, la versión de token y el cambio pendiente viven juntos en un
 * {@link Credenciales} inmutable que se sustituye entero: el login lo lee una
 * sola vez, comprueba la contraseña contra ese hash y pone esa versión en el
 * token, así un cambio de contraseña simultáneo nunca produce un token con la
 * versión nueva validado con la contraseña anterior. Cambiar la contraseña y
 * desactivar incrementan la versión (revocan los tokens emitidos antes);
 * {@link #reactivar()} no la restaura.</p>
 *
 * <p>El rol y el trabajador vinculado solo cambian con
 * {@link #cambiarRol(Rol, String)}, que valida su coherencia; no hay setters
 * genéricos.</p>
 */
public class Usuario {

    /** Formato admitido de username: minúsculas, dígitos, punto, guion bajo o guion. */
    public static final Pattern PATRON_USERNAME = Pattern.compile("^[a-z0-9._-]{3,30}$");

    private final String idUsuario;
    private final String username;
    private volatile Credenciales credenciales;
    private volatile Rol rol;
    private volatile String idTrabajador;
    private volatile boolean activo;

    public Usuario(String idUsuario, String username, String passwordHash, Rol rol, String idTrabajador) {
        this(idUsuario, username, passwordHash, rol, idTrabajador, false);
    }

    public Usuario(String idUsuario, String username, String passwordHash, Rol rol, String idTrabajador,
                   boolean debeCambiarPassword) {
        this.idUsuario = idUsuario;
        this.username = normalizarUsername(username);
        this.credenciales = new Credenciales(passwordHash, 0, debeCambiarPassword);
        this.rol = rol;
        this.idTrabajador = idTrabajador;
        this.activo = true;
    }

    /**
     * Reconstruye un usuario ya guardado, con su estado completo (lo usa el
     * repositorio al leer de la base de datos).
     */
    public static Usuario restaurar(String idUsuario, String username, Credenciales credenciales, Rol rol,
                                    String idTrabajador, boolean activo) {
        Usuario usuario = new Usuario(idUsuario, username, credenciales.hash(), rol, idTrabajador);
        usuario.credenciales = credenciales;
        usuario.activo = activo;
        return usuario;
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
        return credenciales.hash();
    }

    /**
     * Instantánea del hash, la versión de token y el cambio pendiente, leídos a
     * la vez (ver la descripción de la clase).
     */
    @JsonIgnore
    public Credenciales getCredenciales() {
        return credenciales;
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

    /** Si es {@code true}, el usuario debe cambiar la contraseña antes de operar. */
    public boolean isDebeCambiarPassword() {
        return credenciales.debeCambiarPassword();
    }

    /** Versión de token vigente: los tokens con otra versión no valen. */
    public long getVersionToken() {
        return credenciales.version();
    }

    /**
     * Desactiva el usuario y revoca sus tokens. Idempotente: solo la primera
     * desactivación incrementa la versión. Las reglas (no autodesactivarse, no
     * dejar sin administradores) las aplica {@code UsuarioService}.
     */
    public synchronized void desactivar() {
        if (activo) {
            activo = false;
            credenciales = credenciales.conNuevaVersion();
        }
    }

    /**
     * Reactiva el usuario (idempotente). No restaura la versión: los tokens
     * emitidos antes de la desactivación siguen revocados. La comprobación del
     * trabajador vigente la hace {@code UsuarioService}.
     */
    public synchronized void reactivar() {
        activo = true;
    }

    /**
     * Cambia rol y trabajador vinculado comprobando la coherencia del modelo:
     * un ADMIN no puede tener trabajador y el resto de roles sí requieren uno.
     * La existencia, el tipo y la unicidad del trabajador los valida el
     * servicio (y el repositorio, el índice por trabajador).
     *
     * @throws IllegalArgumentException si la combinación rol/trabajador es incoherente
     */
    public synchronized void cambiarRol(Rol nuevoRol, String idTrabajador) {
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
     * Sustituye el hash (nunca la contraseña en claro), incrementa la versión de
     * token y fija si el usuario debe cambiarla en el siguiente acceso.
     *
     * @param nuevoHash    hash BCrypt de la nueva contraseña
     * @param exigirCambio {@code true} para obligar al usuario a cambiarla
     */
    public synchronized void cambiarPassword(String nuevoHash, boolean exigirCambio) {
        if (nuevoHash == null || nuevoHash.isBlank()) {
            throw new IllegalArgumentException("El hash de la contraseña es obligatorio.");
        }
        credenciales = new Credenciales(nuevoHash, credenciales.version() + 1, exigirCambio);
    }

    /**
     * Hash BCrypt, versión de token (claim {@code ver}) y si hay un cambio de
     * contraseña pendiente. Inmutable: cada cambio crea uno nuevo.
     */
    public record Credenciales(String hash, long version, boolean debeCambiarPassword) {

        Credenciales conNuevaVersion() {
            return new Credenciales(hash, version + 1, debeCambiarPassword);
        }

        @Override
        public String toString() {
            return "Credenciales{version=" + version + ", debeCambiarPassword=" + debeCambiarPassword + "}";
        }
    }

    @Override
    public String toString() {
        Credenciales actuales = credenciales;
        return "Usuario{idUsuario='" + idUsuario + "', username='" + username + "', rol=" + rol
                + ", idTrabajador=" + (idTrabajador == null ? "null" : "'" + idTrabajador + "'")
                + ", activo=" + activo + ", debeCambiarPassword=" + actuales.debeCambiarPassword()
                + ", versionToken=" + actuales.version() + "}";
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
