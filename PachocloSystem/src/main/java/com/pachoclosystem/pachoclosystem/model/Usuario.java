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
 */
public class Usuario {

    /** Formato admitido de username: minúsculas, dígitos, punto, guion bajo o guion. */
    public static final Pattern PATRON_USERNAME = Pattern.compile("^[a-z0-9._-]{3,30}$");

    private final String idUsuario;
    private final String username;
    private final String passwordHash;
    private final Rol rol;
    private final String idTrabajador;
    private volatile boolean activo;

    public Usuario(String idUsuario, String username, String passwordHash, Rol rol, String idTrabajador) {
        this.idUsuario = idUsuario;
        this.username = normalizarUsername(username);
        this.passwordHash = passwordHash;
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

    /** Único cambio de estado permitido: desactivar el usuario. */
    public void desactivar() {
        this.activo = false;
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
