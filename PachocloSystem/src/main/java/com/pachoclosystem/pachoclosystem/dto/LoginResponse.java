package com.pachoclosystem.pachoclosystem.dto;

/**
 * Respuesta del login: el token JWT bearer, el tipo de token, el tiempo de
 * validez en segundos, el rol del usuario autenticado y si tiene la contraseña
 * pendiente de cambio (en ese caso solo puede cambiarla).
 */
public record LoginResponse(String token, String tipo, long expiraEnSegundos, String rol,
                            boolean debeCambiarPassword) {
}