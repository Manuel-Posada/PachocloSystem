package com.pachoclosystem.pachoclosystem.security;

import com.pachoclosystem.pachoclosystem.dto.ErrorResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * Serializa el {@link ErrorResponse} uniforme de la API para los manejadores de
 * seguridad (401/403), que escriben el cuerpo directamente en el servlet. Usa el
 * mismo JSON que producen los conversores de la aplicación (Jackson 3).
 */
final class EscritorErrorApi {

    private static final ObjectMapper JSON = new ObjectMapper();

    private EscritorErrorApi() {
    }

    static String jsonDe(ErrorResponse error) {
        try {
            return JSON.writeValueAsString(error);
        } catch (Exception fallo) {
            throw new IllegalStateException("No se pudo serializar la respuesta de error.", fallo);
        }
    }
}