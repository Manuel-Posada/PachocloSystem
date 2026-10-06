package com.pachoclosystem.pachoclosystem.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

/**
 * Conexión con MedicamentosService (otro proceso). Todo sale de
 * {@code application.properties} ({@code medicamentos.*}); la URL nunca está fija
 * en el código.
 *
 * @param url             URL base del servicio, sin barra final (p. ej. http://localhost:8081)
 * @param timeoutConexion tiempo máximo para establecer la conexión
 * @param timeoutLectura  tiempo máximo de espera de la respuesta
 * @param apiKey          clave compartida que se envía en {@code X-Api-Key}; vacía = no se envía
 */
@Validated
@ConfigurationProperties("medicamentos")
public record MedicamentosProperties(
        @NotNull URI url,
        @NotNull Duration timeoutConexion,
        @NotNull Duration timeoutLectura,
        String apiKey) {

    public boolean tieneApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
