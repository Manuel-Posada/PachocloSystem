package com.pachoclosystem.medicamentos.controller;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Health checks para la plataforma de despliegue: sin clave de servicio, sin
 * detalles internos y sin exponer ningún otro endpoint de Actuator.
 */
class HealthCheckTest extends MockMvcBaseTest {

    @Test
    void healthRespondeUpSinClaveYSinDetalles() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(content().string(not(containsString("components"))))
                .andExpect(content().string(not(containsString("db"))));
    }

    @Test
    void livenessYReadinessRespondenUp() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void ningunOtroEndpointDeActuatorEstaExpuesto() throws Exception {
        for (String ruta : new String[]{"/actuator", "/actuator/env", "/actuator/info", "/actuator/beans",
                "/actuator/configprops", "/actuator/heapdump"}) {
            mockMvc.perform(get(ruta)).andExpect(status().isNotFound());
        }
    }
}
