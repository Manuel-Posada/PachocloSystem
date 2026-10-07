package com.pachoclosystem.pachoclosystem.dto;

import com.pachoclosystem.pachoclosystem.model.Usuario;

/**
 * Usuario de acceso ({@code GET /api/auth/me} y {@code /api/usuarios}). Nunca
 * incluye el hash de la contraseña ni la versión del token (un detalle interno
 * del JWT). {@code debeCambiarPassword} avisa de que solo puede cambiar su
 * contraseña antes de operar.
 */
public record UsuarioResponse(String idUsuario, String username, String rol, String idTrabajador,
                              boolean activo, boolean debeCambiarPassword) {

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(usuario.getIdUsuario(), usuario.getUsername(),
                usuario.getRol().name(), usuario.getIdTrabajador(), usuario.isActivo(),
                usuario.isDebeCambiarPassword());
    }
}
