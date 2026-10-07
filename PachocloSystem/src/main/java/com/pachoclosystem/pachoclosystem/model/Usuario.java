package com.pachoclosystem.pachoclosystem.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Usuario de acceso a la API.
 *
 * <p>Guarda <strong>únicamente</strong> el hash BCrypt de la contraseña; la
 * contraseña en claro nunca se almacena ni se muestra. Ni {@code toString()}
 * ni la serialización JSON incluyen el hash.</p>
 */
public class Usuario {

    /** Formato admitido de username: minúsculas, dígitos, punto, guion bajo o guion. */
    public static final Pattern PATRON_USERNAME = Pattern.compile("^[a-z0-9._-]{3,30}$");

    private final String idUsuario;
    private final String username;
    /** Hash y momento del último cambio, siempre juntos: cambian solo al restablecer la contraseña. */
    private volatile Credenciales credenciales;
    private final Rol rol;
    private final String idTrabajador;
    private volatile boolean activo;

    public Usuario(String idUsuario, String username, String passwordHash, Rol rol, String idTrabajador) {
        this.idUsuario = idUsuario;
        this.username = normalizarUsername(username);
        this.credenciales = new Credenciales(passwordHash, null);
        this.rol = rol;
        this.idTrabajador = idTrabajador;
        this.activo = true;
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
     * Instantánea del hash y de su marca, leídas a la vez: el login comprueba la
     * contraseña contra este hash y pone esta marca en el token, de modo que un
     * login con la contraseña anterior nunca produce un token con la marca nueva.
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

    /**
     * Desactivar y activar son los únicos cambios de estado. Las reglas (no
     * autodesactivarse, no dejar sin administradores, trabajador vigente al
     * activar) las aplica {@code UsuarioService}.
     */
    public void desactivar() {
        this.activo = false;
    }

    public void activar() {
        this.activo = true;
    }

    /**
     * Sustituye el hash BCrypt (restablecer contraseña) y anota cuándo, al
     * milisegundo. La marca es estrictamente creciente aunque dos cambios caigan
     * en el mismo milisegundo o el reloj retroceda, porque los tokens se validan
     * por igualdad con ella. Nunca recibe la contraseña en claro.
     */
    public synchronized void cambiarPasswordHash(String nuevoHash, Instant ahora) {
        Instant anterior = credenciales.cambiadasEn();
        Instant cuando = ahora.truncatedTo(ChronoUnit.MILLIS);
        if (anterior != null && !cuando.isAfter(anterior)) {
            cuando = anterior.plusMillis(1);
        }
        this.credenciales = new Credenciales(Objects.requireNonNull(nuevoHash), cuando);
    }

    /**
     * Hash BCrypt y momento de su último cambio ({@code null} si nunca se ha
     * restablecido). {@link #marca()} identifica esta versión de las
     * credenciales dentro del token JWT.
     */
    public record Credenciales(String hash, Instant cambiadasEn) {

        /** Milisegundos del último cambio, o 0 si la contraseña es la del alta. */
        public long marca() {
            return cambiadasEn == null ? 0L : cambiadasEn.toEpochMilli();
        }

        @Override
        public String toString() {
            return "Credenciales{cambiadasEn=" + cambiadasEn + "}";
        }
    }

    @Override
    public String toString() {
        return "Usuario{idUsuario='" + idUsuario + "', username='" + username + "', rol=" + rol
                + ", idTrabajador=" + (idTrabajador == null ? "null" : "'" + idTrabajador + "'")
                + ", activo=" + activo + "}";
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
