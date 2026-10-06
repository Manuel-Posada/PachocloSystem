package com.pachoclosystem.pachoclosystem.controller;

import com.pachoclosystem.pachoclosystem.model.Rol;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autorización por rol en la cadena de filtros: los 403 de ruta se producen antes
 * de validar el cuerpo o buscar el recurso, y usan el {@code ErrorResponse}
 * uniforme sin detalles internos.
 */
class AutorizacionPorRutaTest extends MockMvcBaseTest {

    private static final String MENSAJE_403 = "No tiene permisos para realizar esta operación.";
    private static final String PACIENTE_VALIDO =
            "{\"nombre\":\"Ana Torres\",\"edad\":30,\"habitacion\":101}";

    @Test
    void elEnfermeroNoPuedeCrearPacienteYRecibeEl403Uniforme() throws Exception {
        performComo(Rol.ENFERMERO, post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PACIENTE_VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]").value(MENSAJE_403))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("com.pachoclosystem"))));
    }

    @Test
    void elAdminNoPuedeCrearPacientePeroElDoctorSi() throws Exception {
        performComo(Rol.ADMIN, post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PACIENTE_VALIDO))
                .andExpect(status().isForbidden());

        performComo(Rol.DOCTOR, post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PACIENTE_VALIDO))
                .andExpect(status().isCreated());
    }

    @Test
    void elDoctorNoPuedeBorrarPacientePeroElAdminSi() throws Exception {
        String id = registrarPaciente("Ana Torres", 30, 101);

        performComo(Rol.DOCTOR, delete("/api/pacientes/{id}", id))
                .andExpect(status().isForbidden());

        perform(delete("/api/pacientes/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void ningunRolSalvoElAdminPuedeGestionarTrabajadores() throws Exception {
        String cuerpo = "{\"nombre\":\"Carlos Mena\",\"rol\":\"Doctor\",\"especialidad\":\"Cardiologia\"}";

        performComo(Rol.DOCTOR, post("/api/trabajadores")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
        performComo(Rol.ENFERMERO, post("/api/trabajadores")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
    }

    @Test
    void elAdminNoPuedeCrearRegistroClinico() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);

        performComo(Rol.ADMIN, post("/api/pacientes/{id}/historial", paciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"DIAGNOSTICO","idAutor":"DOC-9999","contenido":"Hipertension leve"}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensajes[0]").value(MENSAJE_403));
    }

    @Test
    void laRutaNiegaAntesDeValidarElCuerpo() throws Exception {
        // El ENFERMERO no puede dar de alta pacientes: aunque el cuerpo sea inválido,
        // la denegación (403) precede a la validación (400).
        performComo(Rol.ENFERMERO, post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void laRutaNiegaAntesDeBuscarElRecurso() throws Exception {
        // Un DOCTOR no puede borrar pacientes: el 403 precede al 404 por id inexistente.
        performComo(Rol.DOCTOR, delete("/api/pacientes/{id}", "PAC-9999"))
                .andExpect(status().isForbidden());
    }

    @Test
    void elAdminNoPuedeModificarPacientesYElDoctorSi() throws Exception {
        String id = registrarPaciente("Ana Torres", 30, 101);

        performComo(Rol.ADMIN, put("/api/pacientes/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana Maria Torres\",\"edad\":31,\"habitacion\":205}"))
                .andExpect(status().isForbidden());

        performComo(Rol.DOCTOR, get("/api/pacientes/{id}", id))
                .andExpect(status().isOk());
    }
}