package com.pachoclosystem.pachoclosystem.model;

/**
 * Roles que un usuario puede tener en la API.
 * Los permisos de cada rol están en {@code SecurityConfig} (tabla "Permisos por
 * rol" del README) y en las reglas de negocio de los servicios.
 */
public enum Rol {
    ADMIN,
    DOCTOR,
    ENFERMERO
}
