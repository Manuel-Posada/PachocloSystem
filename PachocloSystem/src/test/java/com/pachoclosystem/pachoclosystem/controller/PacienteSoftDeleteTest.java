package com.pachoclosystem.pachoclosystem.controller;

import com.pachoclosystem.pachoclosystem.model.Paciente;
import com.pachoclosystem.pachoclosystem.model.Rol;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Baja lógica de pacientes: el DELETE (solo ADMIN) marca al paciente como
 * inactivo, pero el paciente y su historial se conservan en memoria. Un paciente
 * dado de baja se comporta como inexistente (404) y no se reactiva.
 */
class PacienteSoftDeleteTest extends MockMvcBaseTest {

    @Test
    void elAdminDaDeBajaUnPacienteYEsteDejaDeSerVisible() throws Exception {
        String id = registrarPaciente("Ana Torres", 30, 101);

        performComo(Rol.DOCTOR, get("/api/pacientes/{id}", id))
                .andExpect(status().isOk());

        perform(delete("/api/pacientes/{id}", id))
                .andExpect(status().isNoContent());

        performComo(Rol.DOCTOR, get("/api/pacientes/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensajes[0]").value("No se encontró el paciente " + id + "."));

        performComo(Rol.DOCTOR, get("/api/pacientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // El paciente sigue en memoria, solo marcado como inactivo.
        Paciente paciente = repositorioPacientes.buscarPorId(id);
        org.assertj.core.api.Assertions.assertThat(paciente).isNotNull();
        org.assertj.core.api.Assertions.assertThat(paciente.isActivo()).isFalse();
    }

    @Test
    void unPacienteDadoDeBajaNoSePuedeEditarNiCambiarDeHabitacion() throws Exception {
        String id = registrarPaciente("Ana Torres", 30, 101);
        perform(delete("/api/pacientes/{id}", id)).andExpect(status().isNoContent());

        performComo(Rol.DOCTOR, put("/api/pacientes/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana Maria Torres\",\"edad\":31,\"habitacion\":205}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensajes[0]").value("No se encontró el paciente " + id + "."));

        performComo(Rol.DOCTOR, patch("/api/pacientes/{id}/habitacion", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"habitacion\":205}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void noSePuedeDarDeBajaDosVecesAlMismoPaciente() throws Exception {
        String id = registrarPaciente("Ana Torres", 30, 101);

        perform(delete("/api/pacientes/{id}", id)).andExpect(status().isNoContent());
        perform(delete("/api/pacientes/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensajes[0]").value("No se encontró el paciente " + id + "."));
    }

    @Test
    void laBusquedaNoDevuelvePacientesDadosDeBaja() throws Exception {
        registrarPaciente("Ana Torres", 30, 101);
        String dadoDeBaja = registrarPaciente("Bruno Diaz", 45, 202);
        perform(delete("/api/pacientes/{id}", dadoDeBaja)).andExpect(status().isNoContent());

        performComo(Rol.DOCTOR, get("/api/pacientes").param("q", "bruno"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void elHistorialDeUnPacienteDadoDeBajaSeVuelveInaccesibleYElGlobalLoExcluye() throws Exception {
        String activo = registrarPaciente("Ana Torres", 30, 101);
        String dadoDeBaja = registrarPaciente("Bruno Diaz", 45, 202);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        registrarRegistro(dadoDeBaja, """
                {"tipo":"DIAGNOSTICO","idAutor":"%s","contenido":"Cuadro inicial"}""".formatted(doctor));
        registrarRegistro(activo, """
                {"tipo":"DIAGNOSTICO","idAutor":"%s","contenido":"Hipertension leve"}""".formatted(doctor));

        perform(delete("/api/pacientes/{id}", dadoDeBaja)).andExpect(status().isNoContent());

        performComo(Rol.DOCTOR, get("/api/pacientes/{id}/historial", dadoDeBaja))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensajes[0]").value("No se encontró el paciente " + dadoDeBaja + "."));

        performComo(Rol.DOCTOR, get("/api/historial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].idPaciente").value(activo));
    }
}