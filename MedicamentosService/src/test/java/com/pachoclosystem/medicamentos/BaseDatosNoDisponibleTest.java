package com.pachoclosystem.medicamentos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.net.ConnectException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sin PostgreSQL: el servicio no arranca (Flyway necesita la base) y, si la base
 * cae con el servicio ya en marcha, cada petición responde 503 con el cuerpo de
 * error uniforme, sin detalles internos.
 *
 * <p>El contexto de este test apunta a un puerto donde no escucha nadie y no
 * ejecuta Flyway, para simular la caída con la aplicación ya arrancada.</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:1/medicamentos_test",
        "spring.datasource.hikari.connection-timeout=500",
        "spring.flyway.enabled=false"})
@AutoConfigureMockMvc
class BaseDatosNoDisponibleTest {

    private static final String MENSAJE_503 =
            "El servicio no puede acceder a sus datos en este momento. Vuelva a intentarlo más tarde.";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unaConsultaSinBaseDeDatosResponde503Uniforme() throws Exception {
        mockMvc.perform(get("/api/medicamentos"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"))
                .andExpect(jsonPath("$.mensajes[0]").value(MENSAJE_503))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("jdbc"))));
    }

    @Test
    void unaSalidaSinBaseDeDatosResponde503() throws Exception {
        mockMvc.perform(post("/api/medicamentos/MED-0001/salidas")
                        .header("Idempotency-Key", "8f14e45f-ceea-4672-a5b1-7a0c3c9e2f01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":1}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensajes[0]").value(MENSAJE_503));
    }

    @Test
    void sinBaseDeDatosElServicioNoArranca() {
        // Como argumentos de línea de comandos: tienen prioridad sobre la
        // configuración de los tests, que apunta a la base real.
        assertThatThrownBy(() -> new SpringApplicationBuilder(MedicamentosApplication.class)
                .web(WebApplicationType.NONE)
                .run("--spring.datasource.url=jdbc:postgresql://127.0.0.1:1/medicamentos_test",
                        "--spring.datasource.hikari.connection-timeout=500")
                .close())
                .hasRootCauseInstanceOf(ConnectException.class);
    }
}
