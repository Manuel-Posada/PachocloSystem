package com.pachoclosystem.medicamentos.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code POST /api/medicamentos/{id}/salidas} con y sin {@code Idempotency-Key}.
 * El almacén de claves se comparte entre tests (mismo contexto): cada test usa
 * claves nuevas.
 */
class SalidasIdempotentesHttpTest extends MockMvcBaseTest {

    private static String claveNueva() {
        return UUID.randomUUID().toString();
    }

    private ResultActions salida(String id, int cantidad, String clave) throws Exception {
        var peticion = post("/api/medicamentos/{id}/salidas", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":" + cantidad + "}");
        if (clave != null) {
            peticion.header("Idempotency-Key", clave);
        }
        return mockMvc.perform(peticion);
    }

    private int stock(String id) throws Exception {
        Integer stock = leer(mockMvc.perform(get("/api/medicamentos/{id}", id)).andReturn(), "$.cantidadStock");
        return stock;
    }

    private String nuevo(int stock) throws Exception {
        return registrar("Dolex " + UUID.randomUUID().toString().substring(0, 6), "L-1", stock, 2,
                HOY.plusYears(1));
    }

    @Test
    void sinCabeceraCadaSalidaDescuentaComoHastaAhora() throws Exception {
        String id = nuevo(20);

        salida(id, 3, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadStock").value(17))
                .andExpect(header().doesNotExist("Idempotency-Replayed"));
        salida(id, 3, null).andExpect(status().isOk()).andExpect(jsonPath("$.cantidadStock").value(14));

        assertThat(stock(id)).isEqualTo(14);
    }

    @Test
    void conLaMismaClaveLaSegundaDevuelveLaMismaRespuestaSinDescontar() throws Exception {
        String id = nuevo(20);
        String clave = claveNueva();

        MvcResult primera = salida(id, 3, clave)
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Idempotency-Replayed"))
                .andReturn();
        MvcResult segunda = salida(id, 3, clave)
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andReturn();

        assertThat(segunda.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .isEqualTo(primera.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(stock(id)).isEqualTo(17);
    }

    @Test
    void laMismaClaveConOtraCantidadUOtroMedicamentoDa409() throws Exception {
        String id = nuevo(20);
        String otro = nuevo(20);
        String clave = claveNueva();
        salida(id, 3, clave).andExpect(status().isOk());

        salida(id, 4, clave)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensajes[0]").value("La clave de idempotencia ya se usó para "
                        + "otra salida de stock con otro medicamento o cantidad."));
        salida(otro, 3, clave).andExpect(status().isConflict());

        assertThat(stock(id)).isEqualTo(17);
        assertThat(stock(otro)).isEqualTo(20);
    }

    @Test
    void unaClaveMalFormadaDa400SinDescontar() throws Exception {
        String id = nuevo(20);

        for (String clave : new String[] {"corta", "con espacios dentro 123", "x".repeat(101), ""}) {
            salida(id, 1, clave)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensajes[0]").value("La cabecera Idempotency-Key debe tener "
                            + "entre 16 y 100 caracteres: letras sin tilde, dígitos, guion o guion bajo "
                            + "(por ejemplo, un UUID)."));
        }

        assertThat(stock(id)).isEqualTo(20);
    }

    @Test
    void unaSalidaFallidaNoGuardaLaClaveYElReintentoSeReevalua() throws Exception {
        String id = nuevo(5);
        String clave = claveNueva();

        salida(id, 8, clave)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("Stock insuficiente: disponible 5, solicitado 8."));
        mockMvc.perform(post("/api/medicamentos/{id}/entradas", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":10}"))
                .andExpect(status().isOk());

        salida(id, 8, clave)
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Idempotency-Replayed"))
                .andExpect(jsonPath("$.cantidadStock").value(7));
    }

    @Test
    void laCantidadSeValidaAntesQueLaClave() throws Exception {
        String id = nuevo(5);

        salida(id, 0, claveNueva())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("La cantidad debe ser un entero entre 1 y 1000000."));
    }
}
