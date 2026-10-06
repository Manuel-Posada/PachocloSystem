package com.pachoclosystem.pachoclosystem.controller;

import com.jayway.jsonpath.JsonPath;
import com.pachoclosystem.pachoclosystem.model.NivelExperiencia;
import com.pachoclosystem.pachoclosystem.repository.IPacienteRepository;
import com.pachoclosystem.pachoclosystem.repository.ITrabajadoresRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base común de los tests MockMvc: arranca la aplicación completa y vacía los
 * repositorios en memoria antes de cada test, de modo que cada prueba es
 * independiente del orden de ejecución y de los datos creados por otras pruebas.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class MockMvcBaseTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected IPacienteRepository repositorioPacientes;

    @Autowired
    protected ITrabajadoresRepository repositorioTrabajadores;

    @BeforeEach
    void limpiarRepositorios() {
        repositorioPacientes.obtenerTodos()
                .forEach(paciente -> repositorioPacientes.eliminar(paciente.getIdPaciente()));
        repositorioTrabajadores.obtenerTodos()
                .forEach(trabajador -> repositorioTrabajadores.eliminarTrabajador(trabajador.getIdTrabajador()));
    }

    /** Lee un valor del cuerpo de la respuesta (siempre en UTF-8). */
    protected String leer(MvcResult resultado, String expresion) throws Exception {
        return JsonPath.read(
                resultado.getResponse().getContentAsString(StandardCharsets.UTF_8), expresion);
    }

    /** Registra un paciente válido y devuelve su ID (p. ej. PAC-0001). */
    protected String registrarPaciente(String nombre, int edad, int habitacion) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s","edad":%d,"habitacion":%d}
                                """.formatted(nombre, edad, habitacion)))
                .andExpect(status().isCreated())
                .andReturn();
        return leer(resultado, "$.idPaciente");
    }

    /** Registra un doctor válido y devuelve su ID (p. ej. DOC-0001). */
    protected String registrarDoctor(String nombre, String especialidad) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/trabajadores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s","rol":"Doctor","especialidad":"%s"}
                                """.formatted(nombre, especialidad)))
                .andExpect(status().isCreated())
                .andReturn();
        return leer(resultado, "$.idTrabajador");
    }

    /** Registra un enfermero válido y devuelve su ID (p. ej. ENF-0001). */
    protected String registrarEnfermero(String nombre, NivelExperiencia nivel) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/trabajadores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s","rol":"Enfermero","nivelExperiencia":"%s"}
                                """.formatted(nombre, nivel)))
                .andExpect(status().isCreated())
                .andReturn();
        return leer(resultado, "$.idTrabajador");
    }

    /** Agrega un registro al historial de un paciente y devuelve su ID. */
    protected String registrarRegistro(String idPaciente, String cuerpo) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/pacientes/{id}/historial", idPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn();
        return leer(resultado, "$.idRegistro");
    }
}
