package com.pachoclosystem.pachoclosystem.controller;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Health checks para la plataforma de despliegue: públicos (sin token), sin
 * detalles internos y sin exponer ningún otro endpoint de Actuator.
 */
class HealthCheckTest extends MockMvcBaseTest {

    @Test
    void healthRespondeUpSinTokenYSinDetalles() throws Exception {
        sinToken(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(content().string(not(containsString("components"))))
                .andExpect(content().string(not(containsString("db"))));
    }

    @Test
    void livenessYReadinessRespondenUpSinToken() throws Exception {
        sinToken(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        sinToken(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void elRestoDeActuatorNoEsPublicoNiEstaExpuesto() throws Exception {
        for (String ruta : new String[]{"/actuator", "/actuator/env", "/actuator/info", "/actuator/beans",
                "/actuator/heapdump"}) {
            // Sin token: 401, como cualquier ruta no pública.
            sinToken(get(ruta)).andExpect(status().isUnauthorized());
            // Con token de ADMIN: no existe.
            perform(get(ruta)).andExpect(status().isNotFound());
        }
    }
}
