package com.pachoclosystem.pachoclosystem.controller;

import com.pachoclosystem.pachoclosystem.model.Rol;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CORS con {@code app.cors.origenes=http://localhost:3000}: la preflight OPTIONS
 * de un origen permitido se responde sin token, la de un origen no permitido no
 * recibe cabeceras {@code Access-Control-Allow-*}, y los 401/403/429 de un
 * origen permitido llevan {@code Access-Control-Allow-Origin}. En ningún caso
 * se emiten credenciales CORS ({@code allowCredentials=false}) y las cabeceras
 * expuestas incluyen {@code Retry-After} y {@code Location}.
 */
@SpringBootTest(properties = "app.cors.origenes=http://localhost:3000")
class CorsIntegrationTest extends MockMvcBaseTest {

    private static final String ORIGEN_PERMITIDO = "http://localhost:3000";
    private static final String ORIGEN_NO_PERMITIDO = "http://villano.example.com";

    @Test
    void laPreflightDeUnOrigenPermitidoSeRespondeSinTokenYConCabecerasCors() throws Exception {
        mockMvc.perform(options("/api/pacientes")
                        .header(HttpHeaders.ORIGIN, ORIGEN_PERMITIDO)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGEN_PERMITIDO))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("GET")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("Authorization")))
                .andExpect(header().string("Access-Control-Max-Age", "3600"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }

    @Test
    void laPreflightDelAltaIdempotenteDelHistorialPermiteLaClaveYExponeLaRepeticion() throws Exception {
        mockMvc.perform(options("/api/pacientes/PAC-0001/historial")
                        .header(HttpHeaders.ORIGIN, ORIGEN_PERMITIDO)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type, Idempotency-Key"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("Idempotency-Key")));
        mockMvc.perform(get("/api/pacientes").header(HttpHeaders.ORIGIN, ORIGEN_PERMITIDO))
                .andExpect(header().string("Access-Control-Expose-Headers", containsString("Idempotency-Replayed")));
    }

    @Test
    void laPreflightDeUnOrigenNoPermitidoNoRecibeCabecerasCors() throws Exception {
        mockMvc.perform(options("/api/pacientes")
                        .header(HttpHeaders.ORIGIN, ORIGEN_NO_PERMITIDO)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Methods"));
    }

    @Test
    void el401DeOrigenPermitidoLlevaCabecerasCorsYExponeRetryAfterYLocation() throws Exception {
        mockMvc.perform(get("/api/pacientes").header(HttpHeaders.ORIGIN, ORIGEN_PERMITIDO))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGEN_PERMITIDO))
                .andExpect(header().string("Access-Control-Expose-Headers", containsString("Retry-After")))
                .andExpect(header().string("Access-Control-Expose-Headers", containsString("Location")))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }

    @Test
    void el403DeOrigenPermitidoLlevaCabecerasCors() throws Exception {
        // DOCTOR no puede borrar pacientes: 403 de la cadena de autorización.
        MockHttpServletRequestBuilder delPaciente = delete("/api/pacientes/PAC-0001")
                .header(HttpHeaders.ORIGIN, ORIGEN_PERMITIDO);
        mockMvc.perform(autenticadoComo(Rol.DOCTOR, delPaciente))
                .andExpect(status().isForbidden())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGEN_PERMITIDO));
    }

    @Test
    void el429DeOrigenPermitidoLlevaCabecerasCors() throws Exception {
        // 5 fallos: el 6º intento (ya bloqueado) responde 429 con Retry-After y, al
        // venir de un origen permitido, también con Access-Control-Allow-Origin.
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"admin\",\"password\":\"clave-inventada\"}"))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, ORIGEN_PERMITIDO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"clave-inventada\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGEN_PERMITIDO))
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
    }
}