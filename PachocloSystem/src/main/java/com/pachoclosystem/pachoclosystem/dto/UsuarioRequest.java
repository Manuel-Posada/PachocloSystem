package com.pachoclosystem.pachoclosystem.dto;

/**
 * Alta de un usuario ({@code POST /api/usuarios}). Las reglas (formato del
 * username, política de contraseña, rol y vínculo con el trabajador) las valida
 * {@code UsuarioService}, que devuelve todos los errores juntos.
 *
 * @param rol {@code ADMIN}, {@code DOCTOR} o {@code ENFERMERO}
 * @param idTrabajador obligatorio para DOCTOR y ENFERMERO; vacío para ADMIN
 */
public record UsuarioRequest(String username, String password, String rol, String idTrabajador) {

    /** La contraseña nunca aparece en {@code toString()} (logs, errores). */
    @Override
    public String toString() {
        return "UsuarioRequest{username='" + username + "', rol=" + rol
                + ", idTrabajador=" + idTrabajador + "}";
    }
}
