package com.pachoclosystem.pachoclosystem.controller;

import com.pachoclosystem.pachoclosystem.model.NivelExperiencia;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Fija el comportamiento actual del controlador de historial clínico. */
class HistorialClinicoControllerTest extends MockMvcBaseTest {

    private static final String CUERPO_DIAGNOSTICO = """
            {"tipo":"DIAGNOSTICO","idAutor":"%s","contenido":"%s"}""";

    @Test
    void agregarDiagnosticoDevuelve201ConRegistroYSnapshotDelAutor() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");

        String cuerpo = CUERPO_DIAGNOSTICO.formatted(doctor, "Hipertension leve");
        mockMvc.perform(post("/api/pacientes/{id}/historial", paciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idRegistro").isString())
                .andExpect(jsonPath("$.idPaciente").value(paciente))
                .andExpect(jsonPath("$.nombrePaciente").value("Ana Torres"))
                .andExpect(jsonPath("$.fecha").isString())
                .andExpect(jsonPath("$.tipo").value("DIAGNOSTICO"))
                .andExpect(jsonPath("$.contenido").value("Hipertension leve"))
                .andExpect(jsonPath("$.autor.idTrabajador").value(doctor))
                .andExpect(jsonPath("$.autor.nombreCompleto").value("Carlos Mena"))
                .andExpect(jsonPath("$.autor.rol").value("Doctor"));
    }

    @Test
    void agregarSignosVitalesDevuelve201ConContenidoFormateado() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String enfermero = registrarEnfermero("Maria Lopez", NivelExperiencia.AVANZADO);

        mockMvc.perform(post("/api/pacientes/{id}/historial", paciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"SIGNOS_VITALES","idAutor":"%s","signosVitales":{
                                "temperatura":36.5,"frecCardiaca":80,"presionSistolica":120,
                                "presionDiastolica":80,"frecRespiratoria":16,"saturacion":98}}
                                """.formatted(enfermero)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("SIGNOS_VITALES"))
                .andExpect(jsonPath("$.contenido").value(
                        "Signos vitales - Temp: 36.5°C | FC: 80 lpm | PA: 120/80 mmHg"
                                + " | FR: 16 rpm | SpO2: 98%"));
    }

    @Test
    void agregarSignosVitalesConObservacionesAnadeLasObservacionesAlContenido() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String enfermero = registrarEnfermero("Maria Lopez", NivelExperiencia.AVANZADO);

        mockMvc.perform(post("/api/pacientes/{id}/historial", paciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"SIGNOS_VITALES","idAutor":"%s","signosVitales":{
                                "temperatura":37.2,"frecCardiaca":72,"presionSistolica":118,
                                "presionDiastolica":76,"frecRespiratoria":15,"saturacion":97,
                                "observaciones":"paciente estable"}}
                                """.formatted(enfermero)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contenido").value(
                        "Signos vitales - Temp: 37.2°C | FC: 72 lpm | PA: 118/76 mmHg"
                                + " | FR: 15 rpm | SpO2: 97% | Obs: paciente estable"));
    }

    @Test
    void listarHistorialGeneralDevuelve200OrdenadoPorFecha() throws Exception {
        String pacienteA = registrarPaciente("Ana Torres", 30, 101);
        String pacienteB = registrarPaciente("Bruno Diaz", 45, 202);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        String enfermero = registrarEnfermero("Maria Lopez", NivelExperiencia.AVANZADO);

        // El registro de B se crea primero; el orden por fecha debe ser cronológico,
        // no el orden de inserción de los pacientes. La espera separa ambas fechas
        // (LocalDateTime.now() tiene resolución de milisegundos).
        String registroB = registrarRegistro(pacienteB,
                CUERPO_DIAGNOSTICO.formatted(enfermero, "Evolucion de Bruno"));
        Thread.sleep(20);
        String registroA = registrarRegistro(pacienteA,
                CUERPO_DIAGNOSTICO.formatted(doctor, "Diagnostico de Ana"));

        mockMvc.perform(get("/api/historial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].idRegistro").value(registroB))
                .andExpect(jsonPath("$[1].idRegistro").value(registroA));
    }

    @Test
    void listarHistorialFiltradoPorPaciente() throws Exception {
        String pacienteA = registrarPaciente("Ana Torres", 30, 101);
        String pacienteB = registrarPaciente("Bruno Diaz", 45, 202);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        String enfermero = registrarEnfermero("Maria Lopez", NivelExperiencia.AVANZADO);
        registrarRegistro(pacienteA, CUERPO_DIAGNOSTICO.formatted(doctor, "Diagnostico de Ana"));
        registrarRegistro(pacienteB, CUERPO_DIAGNOSTICO.formatted(enfermero, "Evolucion de Bruno"));

        mockMvc.perform(get("/api/historial").param("filtro", "paciente").param("q", "ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].idPaciente").value(pacienteA));
    }

    @Test
    void listarHistorialFiltradoPorAutor() throws Exception {
        String pacienteA = registrarPaciente("Ana Torres", 30, 101);
        String pacienteB = registrarPaciente("Bruno Diaz", 45, 202);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        String enfermero = registrarEnfermero("Maria Lopez", NivelExperiencia.AVANZADO);
        registrarRegistro(pacienteA, CUERPO_DIAGNOSTICO.formatted(doctor, "Diagnostico de Ana"));
        registrarRegistro(pacienteB, CUERPO_DIAGNOSTICO.formatted(enfermero, "Evolucion de Bruno"));

        mockMvc.perform(get("/api/historial").param("filtro", "autor").param("q", "maria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].idPaciente").value(pacienteB));

        mockMvc.perform(get("/api/historial").param("filtro", "todos").param("q", "doc-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].idPaciente").value(pacienteA));
    }

    @Test
    void listarHistorialConFiltroInvalidoDevuelve400ConErrorResponse() throws Exception {
        mockMvc.perform(get("/api/historial").param("filtro", "inventado"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]").value("El filtro debe ser todos, paciente o autor."));
    }

    @Test
    void listarHistorialDeUnPacienteDevuelve200() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        String enfermero = registrarEnfermero("Maria Lopez", NivelExperiencia.AVANZADO);
        registrarRegistro(paciente, CUERPO_DIAGNOSTICO.formatted(doctor, "Diagnostico de Ana"));
        registrarRegistro(paciente, CUERPO_DIAGNOSTICO.formatted(enfermero, "Evolucion de Ana"));

        mockMvc.perform(get("/api/pacientes/{id}/historial", paciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void listarHistorialDeUnPacienteFiltraPorAutor() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        String enfermero = registrarEnfermero("Maria Lopez", NivelExperiencia.AVANZADO);
        registrarRegistro(paciente, CUERPO_DIAGNOSTICO.formatted(doctor, "Diagnostico de Ana"));
        registrarRegistro(paciente, CUERPO_DIAGNOSTICO.formatted(enfermero, "Evolucion de Ana"));

        mockMvc.perform(get("/api/pacientes/{id}/historial", paciente).param("q", "maria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].contenido").value("Evolucion de Ana"));
    }

    @Test
    void listarHistorialDeUnPacienteSinRegistrosDevuelve200Vacio() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);

        mockMvc.perform(get("/api/pacientes/{id}/historial", paciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listarHistorialDePacienteInexistenteDevuelve404ConErrorResponse() throws Exception {
        mockMvc.perform(get("/api/pacientes/{id}/historial", "PAC-9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]").value("No se encontró el paciente PAC-9999."));
    }

    @Test
    void agregarRegistroAPacienteInexistenteDevuelve404ConErrorResponse() throws Exception {
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");

        mockMvc.perform(post("/api/pacientes/{id}/historial", "PAC-9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_DIAGNOSTICO.formatted(doctor, "Hipertension leve")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensajes[0]").value("No se encontró el paciente PAC-9999."));
    }

    @Test
    void agregarRegistroConAutorInexistenteDevuelve404ConErrorResponse() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);

        mockMvc.perform(post("/api/pacientes/{id}/historial", paciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_DIAGNOSTICO.formatted("DOC-9999", "Hipertension leve")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensajes[0]").value("No se encontró un trabajador con el ID DOC-9999."));
    }

    @Test
    void agregarContenidoVacioDevuelve400ConErrorResponse() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");

        mockMvc.perform(post("/api/pacientes/{id}/historial", paciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_DIAGNOSTICO.formatted(doctor, "   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]").value("El contenido no puede estar vacío."));
    }

    @Test
    void agregarContenidoDemasiadoCortoDevuelve400() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");

        mockMvc.perform(post("/api/pacientes/{id}/historial", paciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_DIAGNOSTICO.formatted(doctor, "abc")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]")
                        .value("El contenido es demasiado corto (mínimo 5 caracteres)."));
    }

    @Test
    void agregarContenidoSoloNumerosDevuelve400() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");

        mockMvc.perform(post("/api/pacientes/{id}/historial", paciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_DIAGNOSTICO.formatted(doctor, "12345678")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]")
                        .value("El contenido debe incluir texto descriptivo, no solo números."));
    }

    @Test
    void agregarSignosVitalesSinObjetoDevuelve400() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String enfermero = registrarEnfermero("Maria Lopez", NivelExperiencia.AVANZADO);

        mockMvc.perform(post("/api/pacientes/{id}/historial", paciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"SIGNOS_VITALES","idAutor":"%s"}""".formatted(enfermero)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]")
                        .value("Los signos vitales son obligatorios para este tipo de registro."));
    }

    @Test
    void agregarSignosVitalesConDiastolicaNoMenorQueSistolicaDevuelve400() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String enfermero = registrarEnfermero("Maria Lopez", NivelExperiencia.AVANZADO);

        mockMvc.perform(post("/api/pacientes/{id}/historial", paciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"SIGNOS_VITALES","idAutor":"%s","signosVitales":{
                                "temperatura":36.5,"frecCardiaca":80,"presionSistolica":100,
                                "presionDiastolica":100,"frecRespiratoria":16,"saturacion":98}}
                                """.formatted(enfermero)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]")
                        .value("La presión diastólica debe ser menor que la sistólica."));
    }

    @Test
    void agregarSignosVitalesConObservacionesNumericasDevuelve400() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String enfermero = registrarEnfermero("Maria Lopez", NivelExperiencia.AVANZADO);

        mockMvc.perform(post("/api/pacientes/{id}/historial", paciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"SIGNOS_VITALES","idAutor":"%s","signosVitales":{
                                "temperatura":36.5,"frecCardiaca":80,"presionSistolica":120,
                                "presionDiastolica":80,"frecRespiratoria":16,"saturacion":98,
                                "observaciones":"1234"}}
                                """.formatted(enfermero)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]")
                        .value("Las observaciones no pueden ser solo números."));
    }

    @Test
    void agregarSignosVitalesConTemperaturaFueraDeRangoDevuelve400() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String enfermero = registrarEnfermero("Maria Lopez", NivelExperiencia.AVANZADO);

        mockMvc.perform(post("/api/pacientes/{id}/historial", paciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"SIGNOS_VITALES","idAutor":"%s","signosVitales":{
                                "temperatura":50.0,"frecCardiaca":80,"presionSistolica":120,
                                "presionDiastolica":80,"frecRespiratoria":16,"saturacion":98}}
                                """.formatted(enfermero)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]")
                        .value("Temperatura: debe ser un número entre 30.0 y 45.0 °C."));
    }

    @Test
    void agregarSignosVitalesConFrecuenciaCardiacaFueraDeRangoDevuelve400() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String enfermero = registrarEnfermero("Maria Lopez", NivelExperiencia.AVANZADO);

        mockMvc.perform(post("/api/pacientes/{id}/historial", paciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"SIGNOS_VITALES","idAutor":"%s","signosVitales":{
                                "temperatura":36.5,"frecCardiaca":10,"presionSistolica":120,
                                "presionDiastolica":80,"frecRespiratoria":16,"saturacion":98}}
                                """.formatted(enfermero)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]")
                        .value("Frecuencia Cardíaca: debe ser entre 20 y 250 lpm."));
    }

    @Test
    void agregarRegistroSinTipoDevuelve400ConErrorResponse() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");

        mockMvc.perform(post("/api/pacientes/{id}/historial", paciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idAutor":"%s","contenido":"Hipertension leve"}""".formatted(doctor)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]").value("Debe seleccionar un tipo de registro."));
    }

    @Test
    void agregarRegistroConAutorMalFormadoDevuelve400() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);

        mockMvc.perform(post("/api/pacientes/{id}/historial", paciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_DIAGNOSTICO.formatted("no valido!", "Hipertension leve")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]")
                        .value("El ID del autor solo puede tener letras, números y guiones."));
    }

    @Test
    void losRegistrosEscritosSobrevivenAlBorradoDeSuAutor() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String enfermero = registrarEnfermero("Maria Lopez", NivelExperiencia.AVANZADO);
        registrarRegistro(paciente,
                CUERPO_DIAGNOSTICO.formatted(enfermero, "Evolucion de Ana"));

        mockMvc.perform(delete("/api/trabajadores/{id}", enfermero))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/pacientes/{id}/historial", paciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].contenido").value("Evolucion de Ana"))
                .andExpect(jsonPath("$[0].autor.idTrabajador").value(enfermero))
                .andExpect(jsonPath("$[0].autor.nombreCompleto").value("Maria Lopez"));
    }
}
