package com.pachoclosystem.pachoclosystem.dto;

import com.pachoclosystem.pachoclosystem.model.Usuario;

/**
 * Usuario visto por la API (GET /api/auth/me y gestión de administradores).
 * Nunca incluye el hash de la contraseña ni ningún dato que lo permita
 * reconstruir, y tampoco expone la versión del token ({@code ver} es un
 * detalle interno del JWT que solo evalúa el conversor de autenticación).
 * {@code debeCambiarPassword} avisa de que el usuario solo puede cambiarla
 * antes de operar.
 */
public record UsuarioResponse(String idUsuario, String username, String rol, String idTrabajador,
                              boolean activo, boolean debeCambiarPassword) {

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(usuario.getIdUsuario(), usuario.getUsername(),
                usuario.getRol().name(), usuario.getIdTrabajador(), usuario.isActivo(),
                usuario.isDebeCambiarPassword());
    }
}