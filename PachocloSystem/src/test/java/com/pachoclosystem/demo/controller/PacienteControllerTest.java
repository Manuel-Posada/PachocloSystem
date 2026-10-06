package com.pachoclosystem.demo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
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

/** Fija el comportamiento actual del controlador de pacientes. */
class PacienteControllerTest extends MockMvcBaseTest {

    @Test
    void listarSinPacientesDevuelve200YListaVacia() throws Exception {
        mockMvc.perform(get("/api/pacientes"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void registrarPacienteDevuelve201ConLocationYBody() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana Torres","edad":30,"habitacion":101}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/pacientes/PAC-")))
                .andExpect(jsonPath("$.idPaciente").isString())
                .andExpect(jsonPath("$.nombre").value("Ana Torres"))
                .andExpect(jsonPath("$.edad").value(30))
                .andExpect(jsonPath("$.habitacion").value(101))
                .andReturn();

        String id = leer(resultado, "$.idPaciente");
        assertThat(id).matches("PAC-\\d{4}");
    }

    @Test
    void listarPacientesFiltraPorNombreEIdSinImportarMayusculas() throws Exception {
        registrarPaciente("Ana Torres", 30, 101);
        registrarPaciente("Bruno Diaz", 45, 202);

        mockMvc.perform(get("/api/pacientes").param("q", "ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Ana Torres"));

        mockMvc.perform(get("/api/pacientes").param("q", "BRUNO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Bruno Diaz"));

        mockMvc.perform(get("/api/pacientes").param("q", "zzz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void obtenerPacientePorIdDevuelve200() throws Exception {
        String id = registrarPaciente("Ana Torres", 30, 101);

        mockMvc.perform(get("/api/pacientes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPaciente").value(id))
                .andExpect(jsonPath("$.nombre").value("Ana Torres"))
                .andExpect(jsonPath("$.edad").value(30))
                .andExpect(jsonPath("$.habitacion").value(101));
    }

    @Test
    void editarPacienteDevuelve200ConDatosActualizados() throws Exception {
        String id = registrarPaciente("Ana Torres", 30, 101);

        mockMvc.perform(put("/api/pacientes/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana Maria Torres","edad":31,"habitacion":205}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPaciente").value(id))
                .andExpect(jsonPath("$.nombre").value("Ana Maria Torres"))
                .andExpect(jsonPath("$.edad").value(31))
                .andExpect(jsonPath("$.habitacion").value(205));
    }

    @Test
    void editarHabitacionDevuelve200YElRestoNoCambia() throws Exception {
        String id = registrarPaciente("Ana Torres", 30, 101);

        mockMvc.perform(patch("/api/pacientes/{id}/habitacion", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"habitacion":310}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPaciente").value(id))
                .andExpect(jsonPath("$.nombre").value("Ana Torres"))
                .andExpect(jsonPath("$.edad").value(30))
                .andExpect(jsonPath("$.habitacion").value(310));
    }

    @Test
    void eliminarPacienteDevuelve204YDespuesDevuelve404() throws Exception {
        String id = registrarPaciente("Ana Torres", 30, 101);

        mockMvc.perform(delete("/api/pacientes/{id}", id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(get("/api/pacientes/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtenerPacienteInexistenteDevuelve404ConErrorResponse() throws Exception {
        mockMvc.perform(get("/api/pacientes/{id}", "PAC-9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]").value("No se encontró el paciente PAC-9999."));
    }

    @Test
    void editarPacienteInexistenteDevuelve404ConErrorResponse() throws Exception {
        mockMvc.perform(put("/api/pacientes/{id}", "PAC-9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana Torres","edad":30,"habitacion":101}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensajes[0]").value("No se encontró el paciente PAC-9999."));
    }

    @Test
    void editarHabitacionDePacienteInexistenteDevuelve404ConErrorResponse() throws Exception {
        mockMvc.perform(patch("/api/pacientes/{id}/habitacion", "PAC-9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"habitacion":310}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensajes[0]").value("No se encontró el paciente PAC-9999."));
    }

    @Test
    void eliminarPacienteInexistenteDevuelve404ConErrorResponse() throws Exception {
        mockMvc.perform(delete("/api/pacientes/{id}", "PAC-9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensajes[0]").value("No se encontró el paciente PAC-9999."));
    }

    @Test
    void registrarPacienteConNombreInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"12345","edad":30,"habitacion":101}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]")
                        .value("El nombre debe tener letras y espacios (3 a 60 caracteres)."));
    }

    @Test
    void registrarPacienteConEdadFueraDeRangoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana Torres","edad":200,"habitacion":101}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]").value("La edad debe ser un número entero entre 0 y 120."));
    }

    @Test
    void registrarPacienteConHabitacionFueraDeRangoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana Torres","edad":30,"habitacion":0}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]").value("El número de habitación debe ser un entero entre 1 y 999."));
    }

    @Test
    void registrarPacienteSinCamposObligatoriosDevuelve400ConMensajesOrdenados() throws Exception {
        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.mensajes", hasSize(3)))
                .andExpect(jsonPath("$.mensajes[0]").value("El nombre completo es obligatorio."))
                .andExpect(jsonPath("$.mensajes[1]").value("El número de habitación es obligatorio."))
                .andExpect(jsonPath("$.mensajes[2]").value("La edad es obligatoria."));
    }

    @Test
    void editarHabitacionInvalidaDevuelve400() throws Exception {
        String id = registrarPaciente("Ana Torres", 30, 101);

        mockMvc.perform(patch("/api/pacientes/{id}/habitacion", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"habitacion":1000}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]").value("El número de habitación debe ser un entero entre 1 y 999."));
    }

}
