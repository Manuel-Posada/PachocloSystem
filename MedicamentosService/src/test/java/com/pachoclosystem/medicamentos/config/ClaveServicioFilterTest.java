package com.pachoclosystem.medicamentos.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Con clave configurada, /api/** exige la cabecera X-Api-Key correcta. */
@SpringBootTest(properties = "medicamentos.api-key=clave-de-prueba-123")
@AutoConfigureMockMvc
class ClaveServicioFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sinCabeceraDevuelve401ConErrorResponse() throws Exception {
        mockMvc.perform(get("/api/medicamentos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.mensajes[0]").value("Falta la clave de servicio o no es válida."));
    }

    @Test
    void claveIncorrectaDevuelve401() throws Exception {
        mockMvc.perform(get("/api/medicamentos").header("X-Api-Key", "otra-clave"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void claveCorrectaDejaPasarLaPeticion() throws Exception {
        mockMvc.perform(get("/api/medicamentos").header("X-Api-Key", "clave-de-prueba-123"))
                .andExpect(status().isOk());
    }
}
