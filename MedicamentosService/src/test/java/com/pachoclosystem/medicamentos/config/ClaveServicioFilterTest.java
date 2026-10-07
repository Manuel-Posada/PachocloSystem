package com.pachoclosystem.medicamentos.config;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    void laSalidaIdempotenteExigeLaClaveDeServicioYConEllaFunciona() throws Exception {
        String clave = "clave-de-prueba-123";
        MvcResult alta = mockMvc.perform(post("/api/medicamentos")
                        .header("X-Api-Key", clave)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Dolex","principioActivo":"Paracetamol","presentacion":"TABLETA",
                                 "concentracion":"500 mg","laboratorio":"Genfar","lote":"L-%s",
                                 "cantidadStock":20,"stockMinimo":2,"fechaVencimiento":"2099-01-31",
                                 "ubicacion":"Estante A3"}""".formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn();
        String id = JsonPath.read(alta.getResponse().getContentAsString(), "$.idMedicamento");
        String idempotencia = UUID.randomUUID().toString();

        // Sin X-Api-Key: 401 antes de llegar a la salida, que no descuenta ni guarda la clave.
        mockMvc.perform(post("/api/medicamentos/{id}/salidas", id)
                        .header("Idempotency-Key", idempotencia)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":3}"))
                .andExpect(status().isUnauthorized());

        for (int intento = 0; intento < 2; intento++) {
            mockMvc.perform(post("/api/medicamentos/{id}/salidas", id)
                            .header("X-Api-Key", clave)
                            .header("Idempotency-Key", idempotencia)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"cantidad\":3}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cantidadStock").value(17));
        }
        mockMvc.perform(get("/api/medicamentos/{id}", id).header("X-Api-Key", clave))
                .andExpect(jsonPath("$.cantidadStock").value(17));
    }
}
