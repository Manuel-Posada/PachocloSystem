package com.pachoclosystem.medicamentos.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Fija el comportamiento HTTP del controlador de medicamentos. */
class MedicamentoControllerTest extends MockMvcBaseTest {

    private static final String CUERPO_EDICION = """
            {"nombre":"Dolex Forte","principioActivo":"Paracetamol","presentacion":"TABLETA",
             "concentracion":"500 mg","laboratorio":"Genfar","lote":"%s","stockMinimo":20,
             "fechaVencimiento":"%s","ubicacion":"Bodega 2"}
            """;

    private static String movimiento(int cantidad) {
        return "{\"cantidad\":" + cantidad + "}";
    }

    // --- CRUD -------------------------------------------------------------

    @Test
    void listarSinMedicamentosDevuelve200YListaVacia() throws Exception {
        mockMvc.perform(get("/api/medicamentos"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void registrarDevuelve201ConLocationYBody() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/medicamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoAlta("Dolex", "L-1", 100, 10, HOY.plusYears(1))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/medicamentos/MED-")))
                .andExpect(jsonPath("$.nombre").value("Dolex"))
                .andExpect(jsonPath("$.principioActivo").value("Paracetamol"))
                .andExpect(jsonPath("$.presentacion").value("TABLETA"))
                .andExpect(jsonPath("$.concentracion").value("500 mg"))
                .andExpect(jsonPath("$.laboratorio").value("Genfar"))
                .andExpect(jsonPath("$.lote").value("L-1"))
                .andExpect(jsonPath("$.cantidadStock").value(100))
                .andExpect(jsonPath("$.stockMinimo").value(10))
                .andExpect(jsonPath("$.fechaVencimiento").value(HOY.plusYears(1).toString()))
                .andExpect(jsonPath("$.ubicacion").value("Farmacia - Estante A3"))
                .andExpect(jsonPath("$.stockBajo").value(false))
                .andExpect(jsonPath("$.vencido").value(false))
                .andReturn();

        String id = leer(resultado, "$.idMedicamento");
        assertThat(id).matches("MED-\\d{4}");
        assertThat(resultado.getResponse().getHeader("Location")).isEqualTo("/api/medicamentos/" + id);
    }

    @Test
    void registrarConCamposInvalidosDevuelve400ConTodosLosMensajes() throws Exception {
        mockMvc.perform(post("/api/medicamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":" ","principioActivo":"Paracetamol","presentacion":"TABLETA",
                                 "concentracion":"500 mg","laboratorio":"Genfar","lote":"L-1",
                                 "cantidadStock":-1,"stockMinimo":10,"ubicacion":"A3"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.mensajes", contains(
                        "El nombre es obligatorio.",
                        "La cantidad en stock debe estar entre 0 y 1000000.",
                        "La fecha de vencimiento es obligatoria.")));
    }

    @Test
    void registrarConPresentacionDesconocidaDevuelve400() throws Exception {
        mockMvc.perform(post("/api/medicamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoAlta("Dolex", "L-1", 1, 1, HOY.plusYears(1))
                                .replace("TABLETA", "PILDORA")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]")
                        .value("El cuerpo de la petición es inválido o tiene valores no reconocidos."));
    }

    @Test
    void registrarLoteYaVencidoDevuelve201ConVencidoVerdadero() throws Exception {
        mockMvc.perform(post("/api/medicamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoAlta("Dolex", "L-1", 1, 1, HOY.minusDays(1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vencido").value(true));
    }

    @Test
    void registrarDuplicadoDevuelve409() throws Exception {
        registrar("Dolex", "L-1", 100, 10, HOY.plusYears(1));

        mockMvc.perform(post("/api/medicamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoAlta("dolex", "l-1", 5, 1, HOY.plusYears(1))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensajes[0]").value(
                        "Ya existe un medicamento con el mismo nombre, concentración, presentación y lote."));
    }

    @Test
    void obtenerExistenteDevuelve200YInexistenteDevuelve404() throws Exception {
        String id = registrar("Dolex", "L-1", 100, 10, HOY.plusYears(1));

        mockMvc.perform(get("/api/medicamentos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idMedicamento").value(id));

        mockMvc.perform(get("/api/medicamentos/MED-9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensajes[0]").value("No se encontró el medicamento MED-9999."));
    }

    @Test
    void listarFiltraPorNombrePrincipioActivoEId() throws Exception {
        String id = registrar("Dolex", "L-1", 100, 10, HOY.plusYears(1));
        registrar("Acetaminofen MK", "L-2", 100, 10, HOY.plusYears(1));

        mockMvc.perform(get("/api/medicamentos").param("q", "DOLEX"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].idMedicamento").value(id));
        mockMvc.perform(get("/api/medicamentos").param("q", "paracetamol"))
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/api/medicamentos").param("q", id.toLowerCase()))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void editarActualizaDatosYConservaElStock() throws Exception {
        String id = registrar("Dolex", "L-1", 100, 10, HOY.plusYears(1));

        mockMvc.perform(put("/api/medicamentos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_EDICION.formatted("L-9", HOY.plusMonths(6))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idMedicamento").value(id))
                .andExpect(jsonPath("$.nombre").value("Dolex Forte"))
                .andExpect(jsonPath("$.lote").value("L-9"))
                .andExpect(jsonPath("$.ubicacion").value("Bodega 2"))
                .andExpect(jsonPath("$.stockMinimo").value(20))
                .andExpect(jsonPath("$.cantidadStock").value(100));
    }

    @Test
    void editarInexistenteDevuelve404YHaciaClaveOcupadaDevuelve409() throws Exception {
        registrar("Dolex Forte", "L-1", 100, 10, HOY.plusYears(1));
        String otro = registrar("Dolex", "L-2", 100, 10, HOY.plusYears(1));

        mockMvc.perform(put("/api/medicamentos/MED-9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_EDICION.formatted("L-5", HOY.plusYears(1))))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/medicamentos/{id}", otro)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_EDICION.formatted("L-1", HOY.plusYears(1))))
                .andExpect(status().isConflict());
    }

    @Test
    void eliminarDevuelve204YLuego404() throws Exception {
        String id = registrar("Dolex", "L-1", 100, 10, HOY.plusYears(1));

        mockMvc.perform(delete("/api/medicamentos/{id}", id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/medicamentos/{id}", id))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/medicamentos/{id}", id))
                .andExpect(status().isNotFound());
    }

    // --- Movimientos de stock --------------------------------------------

    @Test
    void entradaSumaAlStock() throws Exception {
        String id = registrar("Dolex", "L-1", 10, 5, HOY.plusYears(1));

        mockMvc.perform(post("/api/medicamentos/{id}/entradas", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movimiento(15)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadStock").value(25));
    }

    @Test
    void salidaRestaDelStockYMarcaStockBajo() throws Exception {
        String id = registrar("Dolex", "L-1", 10, 5, HOY.plusYears(1));

        mockMvc.perform(post("/api/medicamentos/{id}/salidas", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movimiento(5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadStock").value(5))
                .andExpect(jsonPath("$.stockBajo").value(true));
    }

    @Test
    void salidaMayorAlStockDevuelve400YNoDescuenta() throws Exception {
        String id = registrar("Dolex", "L-1", 10, 5, HOY.plusYears(1));

        mockMvc.perform(post("/api/medicamentos/{id}/salidas", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movimiento(11)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("Stock insuficiente: disponible 10, solicitado 11."));

        mockMvc.perform(get("/api/medicamentos/{id}", id))
                .andExpect(jsonPath("$.cantidadStock").value(10));
    }

    @Test
    void salidaDeMedicamentoVencidoDevuelve400() throws Exception {
        String id = registrar("Dolex", "L-1", 10, 5, HOY.plusYears(1));
        mockMvc.perform(put("/api/medicamentos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_EDICION.formatted("L-1", HOY.minusDays(1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vencido").value(true));

        mockMvc.perform(post("/api/medicamentos/{id}/salidas", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movimiento(1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("No se puede dar salida a un medicamento vencido."));
    }

    @Test
    void movimientoConCantidadInvalidaDevuelve400() throws Exception {
        String id = registrar("Dolex", "L-1", 10, 5, HOY.plusYears(1));

        mockMvc.perform(post("/api/medicamentos/{id}/entradas", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movimiento(0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("La cantidad debe ser un entero entre 1 y 1000000."));

        mockMvc.perform(post("/api/medicamentos/{id}/salidas", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("La cantidad es obligatoria."));
    }

    @Test
    void movimientoSobreInexistenteDevuelve404() throws Exception {
        mockMvc.perform(post("/api/medicamentos/MED-9999/entradas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movimiento(1)))
                .andExpect(status().isNotFound());
    }

    // --- Consultas --------------------------------------------------------

    @Test
    void stockBajoDevuelveSoloLosQueEstanEnOPorDebajoDelMinimo() throws Exception {
        String enMinimo = registrar("Dolex", "L-1", 10, 10, HOY.plusYears(1));
        String porDebajo = registrar("Dolex", "L-2", 2, 10, HOY.plusYears(1));
        registrar("Dolex", "L-3", 50, 10, HOY.plusYears(1));

        mockMvc.perform(get("/api/medicamentos/stock-bajo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].idMedicamento", contains(enMinimo, porDebajo)));
    }

    @Test
    void porVencerUsa30DiasPorDefectoYOrdenaPorFecha() throws Exception {
        String en30 = registrar("Dolex", "L-1", 1, 0, HOY.plusDays(30));
        String en5 = registrar("Dolex", "L-2", 1, 0, HOY.plusDays(5));
        String en60 = registrar("Dolex", "L-3", 1, 0, HOY.plusDays(60));

        mockMvc.perform(get("/api/medicamentos/por-vencer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].idMedicamento", contains(en5, en30)));

        mockMvc.perform(get("/api/medicamentos/por-vencer").param("dias", "90"))
                .andExpect(jsonPath("$[*].idMedicamento", contains(en5, en30, en60)));
    }

    @Test
    void porVencerConDiasInvalidosDevuelve400() throws Exception {
        mockMvc.perform(get("/api/medicamentos/por-vencer").param("dias", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("El parámetro dias debe ser un entero entre 1 y 365."));

        mockMvc.perform(get("/api/medicamentos/por-vencer").param("dias", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("El valor de un parámetro de la petición no es válido."));
    }

    @Test
    void vencidosDevuelveSoloLosDeFechaAnteriorAHoy() throws Exception {
        String id = registrar("Dolex", "L-1", 1, 0, HOY.plusDays(10));
        registrar("Dolex", "L-2", 1, 0, HOY);
        mockMvc.perform(put("/api/medicamentos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_EDICION.formatted("L-1", HOY.minusDays(3))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/medicamentos/vencidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].idMedicamento").value(id))
                .andExpect(jsonPath("$[0].vencido").value(true));
    }

    // --- Errores genéricos -----------------------------------------------

    @Test
    void jsonMalformadoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/medicamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]")
                        .value("El cuerpo de la petición es inválido o tiene valores no reconocidos."));
    }

    @Test
    void metodoNoPermitidoDevuelve405ConCabeceraAllow() throws Exception {
        mockMvc.perform(patch("/api/medicamentos/MED-0001"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"))
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void rutaInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensajes[0]").value("No se encontró el recurso solicitado."));
    }

    @Test
    void contentTypeNoSoportadoDevuelve415() throws Exception {
        mockMvc.perform(post("/api/medicamentos")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hola"))
                .andExpect(status().isUnsupportedMediaType());
    }
}
