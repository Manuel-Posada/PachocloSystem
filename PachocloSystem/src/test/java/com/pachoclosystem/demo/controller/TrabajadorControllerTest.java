package com.pachoclosystem.demo.controller;

import com.pachoclosystem.demo.model.NivelExperiencia;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Fija el comportamiento actual del controlador de trabajadores. */
class TrabajadorControllerTest extends MockMvcBaseTest {

    @Test
    void listarSinTrabajadoresDevuelve200YListaVacia() throws Exception {
        mockMvc.perform(get("/api/trabajadores"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void registrarDoctorDevuelve201ConIdPrefijadoDoc() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/trabajadores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Carlos Mena","rol":"Doctor","especialidad":"Cardiologia"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/trabajadores/DOC-")))
                .andExpect(jsonPath("$.nombreCompleto").value("Carlos Mena"))
                .andExpect(jsonPath("$.rol").value("Doctor"))
                .andExpect(jsonPath("$.especialidad").value("Cardiologia"))
                .andExpect(jsonPath("$.nivelExperiencia", nullValue()))
                .andReturn();

        String id = leer(resultado, "$.idTrabajador");
        assertThat(id).matches("DOC-\\d{4}");
    }

    @Test
    void registrarEnfermeroDevuelve201ConIdPrefijadoEnf() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/trabajadores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Maria Lopez","rol":"Enfermero","nivelExperiencia":"AVANZADO"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/trabajadores/ENF-")))
                .andExpect(jsonPath("$.nombreCompleto").value("Maria Lopez"))
                .andExpect(jsonPath("$.rol").value("Enfermero"))
                .andExpect(jsonPath("$.especialidad", nullValue()))
                .andExpect(jsonPath("$.nivelExperiencia").value("AVANZADO"))
                .andReturn();

        String id = leer(resultado, "$.idTrabajador");
        assertThat(id).matches("ENF-\\d{4}");
    }

    @Test
    void listarTrabajadoresFiltraPorNombreEIdSinImportarMayusculas() throws Exception {
        registrarDoctor("Carlos Mena", "Cardiologia");
        registrarEnfermero("Maria Lopez", NivelExperiencia.AVANZADO);

        mockMvc.perform(get("/api/trabajadores").param("q", "carlos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombreCompleto").value("Carlos Mena"));

        mockMvc.perform(get("/api/trabajadores").param("q", "ENF-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].rol").value("Enfermero"));

        mockMvc.perform(get("/api/trabajadores").param("q", "zzz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void obtenerTrabajadorPorIdDevuelve200() throws Exception {
        String id = registrarDoctor("Carlos Mena", "Cardiologia");

        mockMvc.perform(get("/api/trabajadores/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idTrabajador").value(id))
                .andExpect(jsonPath("$.nombreCompleto").value("Carlos Mena"))
                .andExpect(jsonPath("$.rol").value("Doctor"))
                .andExpect(jsonPath("$.especialidad").value("Cardiologia"));
    }

    @Test
    void editarDoctorDevuelve200ConDatosActualizados() throws Exception {
        String id = registrarDoctor("Carlos Mena", "Cardiologia");

        mockMvc.perform(put("/api/trabajadores/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Carlos Alberto Mena","rol":"Doctor","especialidad":"Neurologia"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idTrabajador").value(id))
                .andExpect(jsonPath("$.nombreCompleto").value("Carlos Alberto Mena"))
                .andExpect(jsonPath("$.rol").value("Doctor"))
                .andExpect(jsonPath("$.especialidad").value("Neurologia"));
    }

    @Test
    void eliminarTrabajadorDevuelve204YDespuesDevuelve404() throws Exception {
        String id = registrarEnfermero("Maria Lopez", NivelExperiencia.AVANZADO);

        mockMvc.perform(delete("/api/trabajadores/{id}", id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(get("/api/trabajadores/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtenerTrabajadorInexistenteDevuelve404ConErrorResponse() throws Exception {
        mockMvc.perform(get("/api/trabajadores/{id}", "DOC-9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]").value("No se encontró el trabajador DOC-9999."));
    }

    @Test
    void editarTrabajadorInexistenteDevuelve404ConErrorResponse() throws Exception {
        mockMvc.perform(put("/api/trabajadores/{id}", "DOC-9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Carlos Mena","rol":"Doctor","especialidad":"Cardiologia"}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensajes[0]").value("No se encontró el trabajador DOC-9999."));
    }

    @Test
    void eliminarTrabajadorInexistenteDevuelve404ConErrorResponse() throws Exception {
        mockMvc.perform(delete("/api/trabajadores/{id}", "ENF-9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensajes[0]").value("No se encontró el trabajador ENF-9999."));
    }

    @Test
    void registrarTrabajadorConRolInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/trabajadores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Carlos Mena","rol":"Medico"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]").value("El rol debe ser Doctor o Enfermero."));
    }

    @Test
    void registrarTrabajadorConNombreInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/trabajadores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"12345","rol":"Doctor","especialidad":"Cardiologia"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]")
                        .value("El nombre debe tener solo letras y espacios (3 a 60 caracteres)."));
    }

    @Test
    void registrarDoctorSinEspecialidadDevuelve400() throws Exception {
        mockMvc.perform(post("/api/trabajadores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Carlos Mena","rol":"Doctor"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]").value("La especialidad es obligatoria."));
    }

    @Test
    void registrarDoctorConEspecialidadCortaDevuelve400() throws Exception {
        mockMvc.perform(post("/api/trabajadores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Carlos Mena","rol":"Doctor","especialidad":"12"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]")
                        .value("La especialidad debe ser un texto descriptivo (mínimo 3 caracteres)."));
    }

    @Test
    void registrarDoctorConEspecialidadSoloNumerosDevuelve400() throws Exception {
        mockMvc.perform(post("/api/trabajadores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Carlos Mena","rol":"Doctor","especialidad":"12345678"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]")
                        .value("La especialidad debe ser un texto descriptivo (mínimo 3 caracteres)."));
    }

    @Test
    void registrarEnfermeroSinNivelDeExperienciaDevuelve400() throws Exception {
        mockMvc.perform(post("/api/trabajadores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Maria Lopez","rol":"Enfermero"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]").value("Debe seleccionar un nivel de experiencia."));
    }

    @Test
    void editarTrabajadorNoPermiteCambiarElRolDevuelve400() throws Exception {
        String id = registrarDoctor("Carlos Mena", "Cardiologia");

        mockMvc.perform(put("/api/trabajadores/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Carlos Mena","rol":"Enfermero","nivelExperiencia":"NOVATO"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]")
                        .value("El rol de un trabajador no puede cambiar (actual: Doctor)."));
    }

    @Test
    void registrarTrabajadorSinCamposObligatoriosDevuelve400ConMensajesOrdenados() throws Exception {
        mockMvc.perform(post("/api/trabajadores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.mensajes", hasSize(2)))
                .andExpect(jsonPath("$.mensajes[0]").value("Debe seleccionar un rol."))
                .andExpect(jsonPath("$.mensajes[1]").value("El nombre completo es obligatorio."));
    }
}
