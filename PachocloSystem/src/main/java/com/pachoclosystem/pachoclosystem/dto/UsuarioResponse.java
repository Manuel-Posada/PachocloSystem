package com.pachoclosystem.pachoclosystem.dto;

import com.pachoclosystem.pachoclosystem.model.Usuario;

/**
 * Usuario autenticado (GET /api/auth/me). Nunca incluye el hash de la
 * contraseña.
 */
public record UsuarioResponse(String idUsuario, String username, String rol, String idTrabajador) {

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(usuario.getIdUsuario(), usuario.getUsername(),
                usuario.getRol().name(), usuario.getIdTrabajador());
    }
}