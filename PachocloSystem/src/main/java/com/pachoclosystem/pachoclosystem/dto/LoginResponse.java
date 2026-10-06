package com.pachoclosystem.pachoclosystem.dto;

/**
 * Respuesta del login: el token JWT bearer, el tipo de token, el tiempo de
 * validez en segundos y el rol del usuario autenticado.
 */
public record LoginResponse(String token, String tipo, long expiraEnSegundos, String rol) {
}