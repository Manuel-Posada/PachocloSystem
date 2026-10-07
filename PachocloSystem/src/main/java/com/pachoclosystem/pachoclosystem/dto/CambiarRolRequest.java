package com.pachoclosystem.pachoclosystem.dto;

/**
 * Cambio de rol (y del trabajador vinculado) decidido por un administrador.
 */
public record CambiarRolRequest(String rol, String idTrabajador) {
}