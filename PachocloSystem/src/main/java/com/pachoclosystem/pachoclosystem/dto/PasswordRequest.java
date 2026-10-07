package com.pachoclosystem.pachoclosystem.dto;

/** Nueva contraseña al restablecerla ({@code PATCH /api/usuarios/{id}/password}). */
public record PasswordRequest(String password) {

    /** La contraseña nunca aparece en {@code toString()} (logs, errores). */
    @Override
    public String toString() {
        return "PasswordRequest{password=***}";
    }
}
