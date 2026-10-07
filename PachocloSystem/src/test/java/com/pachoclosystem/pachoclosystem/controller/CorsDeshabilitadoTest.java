package com.pachoclosystem.pachoclosystem.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sin {@code app.cors.origenes} no se emite ninguna cabecera CORS: ni en una
 * petición real con header {@code Origin} ni en una preflight OPTIONS, sea cual
 * sea el origen.
 */
class CorsDeshabilitadoTest extends MockMvcBaseTest {

    @Test
    void sinConfiguracionNoSeEmiteNingunaCabeceraCors() throws Exception {
        mockMvc.perform(get("/api/pacientes").header(HttpHeaders.ORIGIN, "http://localhost:3000"))
                .andExpect(status().is4xxClientError())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));

        mockMvc.perform(options("/api/pacientes")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Methods"));
    }
}