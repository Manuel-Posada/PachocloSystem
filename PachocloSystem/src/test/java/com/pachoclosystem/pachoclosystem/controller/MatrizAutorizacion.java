package com.pachoclosystem.pachoclosystem.controller;

import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fuente única de verdad de los endpoints reales de la API y de su política de
 * acceso. Cada entrada se identifica por {@code VERBO patrón}, con las
 * variables de ruta tal y como las expone {@code RequestMappingHandlerMapping}
 * (p. ej. {@code GET /api/pacientes/{id}}).
 *
 * <p>{@link CoberturaRutasTest} comprueba que toda ruta realmente mapeada figura
 * aquí; {@link AutorizacionPorRutaTest} ejercita el comportamiento observable
 * de esta misma matriz. Añadir un endpoint sin declararlo aquí hace fallar el
 * test estructural.</p>
 */
final class MatrizAutorizacion {

    /** Política de acceso de un endpoint, en el orden de la cadena de seguridad. */
    enum Acceso {
        PUBLICO,
        AUTENTICADO,
        ADMIN,
        DOCTOR,
        DOCTOR_O_ENFERMERO
    }

    record Endpoint(HttpMethod metodo, String patron, Acceso acceso) {

        String clave() {
            return metodo.name() + " " + patron;
        }
    }

    /** Todos los endpoints reales de la API. */
    static final List<Endpoint> ENDPOINTS = List.of(
            // Autenticación: el login es el único endpoint público.
            new Endpoint(HttpMethod.POST, "/api/auth/login", Acceso.PUBLICO),
            new Endpoint(HttpMethod.GET, "/api/auth/me", Acceso.AUTENTICADO),

            // Pacientes: lectura autenticada; alta/edición DOCTOR; baja ADMIN.
            new Endpoint(HttpMethod.GET, "/api/pacientes", Acceso.AUTENTICADO),
            new Endpoint(HttpMethod.GET, "/api/pacientes/{id}", Acceso.AUTENTICADO),
            new Endpoint(HttpMethod.POST, "/api/pacientes", Acceso.DOCTOR),
            new Endpoint(HttpMethod.PUT, "/api/pacientes/{id}", Acceso.DOCTOR),
            new Endpoint(HttpMethod.PATCH, "/api/pacientes/{id}/habitacion", Acceso.DOCTOR),
            new Endpoint(HttpMethod.DELETE, "/api/pacientes/{id}", Acceso.ADMIN),

            // Historial: lectura autenticada; alta DOCTOR o ENFERMERO.
            new Endpoint(HttpMethod.GET, "/api/historial", Acceso.AUTENTICADO),
            new Endpoint(HttpMethod.GET, "/api/pacientes/{id}/historial", Acceso.AUTENTICADO),
            new Endpoint(HttpMethod.POST, "/api/pacientes/{id}/historial", Acceso.DOCTOR_O_ENFERMERO),

            // Trabajadores: lectura autenticada; gestión ADMIN.
            new Endpoint(HttpMethod.GET, "/api/trabajadores", Acceso.AUTENTICADO),
            new Endpoint(HttpMethod.GET, "/api/trabajadores/{id}", Acceso.AUTENTICADO),
            new Endpoint(HttpMethod.POST, "/api/trabajadores", Acceso.ADMIN),
            new Endpoint(HttpMethod.PUT, "/api/trabajadores/{id}", Acceso.ADMIN),
            new Endpoint(HttpMethod.DELETE, "/api/trabajadores/{id}", Acceso.ADMIN));

    /** Claves {@code VERBO patrón} de todos los endpoints de la matriz. */
    static Set<String> claves() {
        return ENDPOINTS.stream().map(Endpoint::clave).collect(Collectors.toUnmodifiableSet());
    }

    private MatrizAutorizacion() {
    }
}
