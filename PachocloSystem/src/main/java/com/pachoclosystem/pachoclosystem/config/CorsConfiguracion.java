package com.pachoclosystem.pachoclosystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * CORS configurable por orígenes explícitos.
 *
 * <p>La propiedad {@code app.cors.origenes} (variable de entorno
 * {@code CORS_ORIGENES}) es una lista separada por comas de orígenes
 * absolutos http(s), sin barra final y <strong>sin comodines {@code '*'}:
 * cualquier violación aborta el arranque con un mensaje claro</strong>. El
 * valor por defecto es vacío: sin orígenes permitidos, ninguna respuesta
 * lleva cabeceras {@code Access-Control-Allow-*}.</p>
 *
 * <p>La {@link CorsConfigurationSource} se aplica en la cadena de seguridad
 * con {@code http.cors(...)}: las preflight {@code OPTIONS} de un origen
 * permitido se responden con 200 y las cabeceras correctas sin exigir token.
 * Métodos permitidos: GET, POST, PUT, PATCH, DELETE y OPTIONS. Cabeceras
 * permitidas: Authorization y Content-Type. Cabeceras expuestas: Retry-After
 * y Location (para que el navegador pueda leer los 429/302). Nunca se usan
 * credenciales ({@code allowCredentials=false}); {@code maxAge} de 1 hora.</p>
 */
@Configuration
public class CorsConfiguracion {

    static final String PROP_ORIGENES = "app.cors.origenes";

    private static final List<String> METODOS_PERMITIDOS =
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    /** Incluye la clave de idempotencia de las altas del historial (la envía el frontend). */
    private static final List<String> CABECERAS_PERMITIDAS =
            List.of("Authorization", "Content-Type", "Idempotency-Key");
    /** {@code Idempotency-Replayed} indica que un alta del historial es la repetición de otra. */
    private static final List<String> CABECERAS_EXPUESTAS =
            List.of("Retry-After", "Location", "Idempotency-Replayed");

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.origenes:}") String origenesConfig) {
        List<String> origenes = normalizarOrigenes(origenesConfig);

        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(origenes);
        configuracion.setAllowedMethods(METODOS_PERMITIDOS);
        configuracion.setAllowedHeaders(CABECERAS_PERMITIDAS);
        configuracion.setExposedHeaders(CABECERAS_EXPUESTAS);
        configuracion.setAllowCredentials(false);
        configuracion.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", configuracion);
        return fuente;
    }

    /**
     * Normaliza {@code app.cors.origenes} / {@code CORS_ORIGENES}: lista
     * separada por comas, recortando espacios e ignorando entradas vacías.
     * Cada origen debe ser una URL absoluta http(s), sin comodín y sin barra
     * final; una violación lanza {@link IllegalStateException} (falla el
     * arranque) con un mensaje claro.
     */
    static List<String> normalizarOrigenes(String cruda) {
        List<String> origenes = new ArrayList<>();
        if (cruda == null || cruda.isBlank()) {
            return origenes;
        }
        for (String parte : cruda.split(",")) {
            String origen = parte.trim();
            if (origen.isEmpty()) {
                continue;
            }
            validar(origen);
            origenes.add(origen);
        }
        return origenes;
    }

    private static void validar(String origen) {
        if (origen.indexOf('*') >= 0) {
            throw new IllegalStateException("El valor de " + PROP_ORIGENES
                    + " (CORS_ORIGENES) no admite comodines '*': especifique orígenes "
                    + "concretos, p. ej. http://localhost:3000.");
        }
        URI uri;
        try {
            uri = URI.create(origen);
        } catch (IllegalArgumentException ex) {
            throw nuevoErrorDeOrigen(origen);
        }
        boolean esquemaHttp = uri.getScheme() != null
                && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()));
        if (!esquemaHttp || uri.getHost() == null || uri.getHost().isBlank()) {
            throw nuevoErrorDeOrigen(origen);
        }
        if (origen.endsWith("/")) {
            throw nuevoErrorDeOrigen(origen);
        }
    }

    private static IllegalStateException nuevoErrorDeOrigen(String origen) {
        return new IllegalStateException("El origen CORS '" + sanitizar(origen)
                + "' no es válido: debe ser una URL absoluta http(s), sin comodín y sin "
                + "barra final (" + PROP_ORIGENES + " / CORS_ORIGENES).");
    }

    /** Sustituye caracteres de control para que el mensaje no inyecte saltos de línea. */
    private static String sanitizar(String valor) {
        StringBuilder limpio = new StringBuilder(valor.length());
        for (int i = 0; i < valor.length(); i++) {
            char caracter = valor.charAt(i);
            limpio.append(Character.isISOControl(caracter) ? '?' : caracter);
        }
        return limpio.toString();
    }
}