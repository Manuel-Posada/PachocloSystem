package com.pachoclosystem.pachoclosystem.controller;

import com.pachoclosystem.pachoclosystem.model.Rol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Matriz de permisos sobre los endpoints reales, ejercitada a través de la
 * cadena de filtros con tokens JWT reales de cada rol (sin {@code addFilters=false}
 * ni usuarios simulados). Cubre, rol a rol:
 *
 * <ul>
 *   <li>{@code POST /api/auth/login} es el único endpoint público; el resto
 *       responde 401 sin token.</li>
 *   <li>La lectura (identidad, pacientes, historial y trabajadores) es para
 *       cualquier usuario autenticado.</li>
 *   <li>El alta y la edición de pacientes son solo de DOCTOR; la baja, solo de
 *       ADMIN.</li>
 *   <li>La gestión de trabajadores es solo de ADMIN.</li>
 *   <li>El alta de registros clínicos es de DOCTOR (cualquier tipo) y de
 *       ENFERMERO (solo {@code SIGNOS_VITALES}); ADMIN no puede.</li>
 * </ul>
 *
 * <p>Los 403 de ruta se producen antes de validar el cuerpo o buscar el recurso,
 * y todos usan el {@code ErrorResponse} uniforme sin detalles internos.</p>
 */
class AutorizacionPorRutaTest extends MockMvcBaseTest {

    private static final String MENSAJE_403 = "No tiene permisos para realizar esta operación.";
    private static final String PACIENTE_VALIDO =
            "{\"nombre\":\"Ana Torres\",\"edad\":30,\"habitacion\":101}";
    private static final String TRABAJADOR_VALIDO =
            "{\"nombre\":\"Carlos Mena\",\"rol\":\"Doctor\",\"especialidad\":\"Cardiologia\"}";
    private static final String DIAGNOSTICO =
            "{\"tipo\":\"DIAGNOSTICO\",\"contenido\":\"Hipertension leve\"}";
    private static final String SIGNOS_VITALES = """
            {"tipo":"SIGNOS_VITALES","signosVitales":{
            "temperatura":36.5,"frecCardiaca":80,"presionSistolica":120,
            "presionDiastolica":80,"frecRespiratoria":16,"saturacion":98}}""";

    // ------------------------------------------------------------------
    // Autenticación: login público y 401 sin token
    // ------------------------------------------------------------------

    @Test
    void elLoginEsElUnicoEndpointPublico() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"pwd-de-prueba-solo-tests-12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @ParameterizedTest(name = "sin token: {0}")
    @MethodSource("endpointsProtegidos")
    void sinTokenElEndpointProtegidoDevuelve401(MockHttpServletRequestBuilder peticion) throws Exception {
        mockMvc.perform(peticion)
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]")
                        .value("Debe autenticarse para acceder a este recurso."));
    }

    static Stream<MockHttpServletRequestBuilder> endpointsProtegidos() {
        return Stream.of(
                get("/api/auth/me"),
                get("/api/pacientes"),
                get("/api/pacientes/PAC-0001"),
                post("/api/pacientes").contentType(MediaType.APPLICATION_JSON).content(PACIENTE_VALIDO),
                put("/api/pacientes/PAC-0001").contentType(MediaType.APPLICATION_JSON).content(PACIENTE_VALIDO),
                patch("/api/pacientes/PAC-0001/habitacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"habitacion\":200}"),
                delete("/api/pacientes/PAC-0001"),
                get("/api/historial"),
                get("/api/pacientes/PAC-0001/historial"),
                post("/api/pacientes/PAC-0001/historial").contentType(MediaType.APPLICATION_JSON)
                        .content(DIAGNOSTICO),
                get("/api/trabajadores"),
                get("/api/trabajadores/DOC-0001"),
                post("/api/trabajadores").contentType(MediaType.APPLICATION_JSON).content(TRABAJADOR_VALIDO),
                put("/api/trabajadores/DOC-0001").contentType(MediaType.APPLICATION_JSON)
                        .content(TRABAJADOR_VALIDO),
                delete("/api/trabajadores/DOC-0001"));
    }

    // ------------------------------------------------------------------
    // Lectura permitida a todos los roles autenticados
    // ------------------------------------------------------------------

    @ParameterizedTest(name = "{0} puede verse a sí mismo")
    @EnumSource(Rol.class)
    void todosLosRolesPuedenConsultarSuIdentidad(Rol rol) throws Exception {
        performComo(rol, get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").isString());
    }

    @ParameterizedTest(name = "{0} puede consultar pacientes")
    @EnumSource(Rol.class)
    void todosLosRolesPuedenConsultarPacientes(Rol rol) throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);

        performComo(rol, get("/api/pacientes"))
                .andExpect(status().isOk());
        performComo(rol, get("/api/pacientes/{id}", paciente))
                .andExpect(status().isOk());
    }

    @ParameterizedTest(name = "{0} puede consultar trabajadores")
    @EnumSource(Rol.class)
    void todosLosRolesPuedenConsultarTrabajadores(Rol rol) throws Exception {
        String trabajador = registrarDoctor("Carlos Mena", "Cardiologia");

        performComo(rol, get("/api/trabajadores"))
                .andExpect(status().isOk());
        performComo(rol, get("/api/trabajadores/{id}", trabajador))
                .andExpect(status().isOk());
    }

    @ParameterizedTest(name = "{0} puede consultar el historial")
    @EnumSource(Rol.class)
    void todosLosRolesPuedenConsultarElHistorial(Rol rol) throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        registrarRegistro(paciente, Rol.DOCTOR, doctor, DIAGNOSTICO);

        performComo(rol, get("/api/historial"))
                .andExpect(status().isOk());
        performComo(rol, get("/api/pacientes/{id}/historial", paciente))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------
    // Pacientes: alta/edición solo DOCTOR, baja solo ADMIN
    // ------------------------------------------------------------------

    @ParameterizedTest(name = "{0} ante POST /api/pacientes")
    @EnumSource(Rol.class)
    void soloElDoctorPuedeDarDeAltaPacientes(Rol rol) throws Exception {
        ResultActions resultado = performComo(rol, post("/api/pacientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(PACIENTE_VALIDO));

        if (rol == Rol.DOCTOR) {
            resultado.andExpect(status().isCreated());
        } else {
            espera403Uniforme(resultado);
        }
    }

    @ParameterizedTest(name = "{0} ante PUT/PATCH /api/pacientes")
    @EnumSource(Rol.class)
    void soloElDoctorPuedeEditarPacientes(Rol rol) throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);

        ResultActions edicionCompleta = performComo(rol, put("/api/pacientes/{id}", paciente)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Ana Maria Torres\",\"edad\":31,\"habitacion\":205}"));
        ResultActions edicionHabitacion = performComo(rol, patch("/api/pacientes/{id}/habitacion", paciente)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"habitacion\":206}"));

        if (rol == Rol.DOCTOR) {
            edicionCompleta.andExpect(status().isOk());
            edicionHabitacion.andExpect(status().isOk());
        } else {
            espera403Uniforme(edicionCompleta);
            espera403Uniforme(edicionHabitacion);
        }
    }

    @ParameterizedTest(name = "{0} ante DELETE /api/pacientes")
    @EnumSource(Rol.class)
    void soloElAdminPuedeDarDeBajaPacientes(Rol rol) throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);

        ResultActions baja = performComo(rol, delete("/api/pacientes/{id}", paciente));

        if (rol == Rol.ADMIN) {
            baja.andExpect(status().isNoContent());
        } else {
            espera403Uniforme(baja);
        }
    }

    // ------------------------------------------------------------------
    // Trabajadores: gestión solo ADMIN
    // ------------------------------------------------------------------

    @ParameterizedTest(name = "{0} ante la gestión de trabajadores")
    @EnumSource(Rol.class)
    void soloElAdminPuedeGestionarTrabajadores(Rol rol) throws Exception {
        ResultActions alta = performComo(rol, post("/api/trabajadores")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TRABAJADOR_VALIDO));
        if (rol == Rol.ADMIN) {
            alta.andExpect(status().isCreated());
        } else {
            espera403Uniforme(alta);
        }

        // Para editar y borrar hace falta un trabajador existente.
        String trabajador = registrarDoctor("Carlos Mena", "Cardiologia");

        ResultActions edicion = performComo(rol, put("/api/trabajadores/{id}", trabajador)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Carlos Mena Ruiz\",\"rol\":\"Doctor\",\"especialidad\":\"Neurologia\"}"));
        ResultActions baja = performComo(rol, delete("/api/trabajadores/{id}", trabajador));

        if (rol == Rol.ADMIN) {
            edicion.andExpect(status().isOk());
            baja.andExpect(status().isNoContent());
        } else {
            espera403Uniforme(edicion);
            espera403Uniforme(baja);
        }
    }

    // ------------------------------------------------------------------
    // Historial: alta según rol y tipo
    // ------------------------------------------------------------------

    @ParameterizedTest(name = "{0} ante el alta de registros clínicos")
    @EnumSource(Rol.class)
    void elDoctorPuedeRegistrarCualquierTipoYElEnfermeroSoloSignosVitales(Rol rol) throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);

        ResultActions diagnostico = performComo(rol, post("/api/pacientes/{id}/historial", paciente)
                .contentType(MediaType.APPLICATION_JSON)
                .content(DIAGNOSTICO));
        ResultActions signos = performComo(rol, post("/api/pacientes/{id}/historial", paciente)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SIGNOS_VITALES));

        switch (rol) {
            case DOCTOR -> {
                diagnostico.andExpect(status().isCreated());
                signos.andExpect(status().isCreated());
            }
            case ENFERMERO -> {
                // La ruta lo permite; el servicio niega los tipos distintos de
                // SIGNOS_VITALES con el mismo 403 uniforme.
                espera403Uniforme(diagnostico);
                signos.andExpect(status().isCreated());
            }
            case ADMIN -> {
                espera403Uniforme(diagnostico);
                espera403Uniforme(signos);
            }
        }
    }

    // ------------------------------------------------------------------
    // Precedencia de la denegación y forma del error
    // ------------------------------------------------------------------

    @Test
    void laRutaNiegaElAltaDePacientesAntesDeValidarElCuerpo() throws Exception {
        // El ENFERMERO no puede dar de alta pacientes: aunque el cuerpo sea inválido,
        // la denegación (403) precede a la validación (400).
        performComo(Rol.ENFERMERO, post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void laRutaNiegaLaBajaDePacientesAntesDeBuscarElRecurso() throws Exception {
        // Un DOCTOR no puede borrar pacientes: el 403 precede al 404 por id inexistente.
        performComo(Rol.DOCTOR, delete("/api/pacientes/{id}", "PAC-9999"))
                .andExpect(status().isForbidden());
    }

    @Test
    void laRutaNiegaElAltaDeRegistroDelAdminAntesDeValidarElCuerpo() throws Exception {
        // ADMIN no puede registrar historial: el 403 de ruta precede al 400 por cuerpo vacío.
        performComo(Rol.ADMIN, post("/api/pacientes/{id}/historial", "PAC-9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    /** Comprueba el 403 uniforme de la API (sin trazas ni nombres de clases). */
    private void espera403Uniforme(ResultActions resultado) throws Exception {
        resultado
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
}