package com.pachoclosystem.pachoclosystem.controller;

import com.pachoclosystem.pachoclosystem.client.MedicamentosClient;
import com.pachoclosystem.pachoclosystem.dto.MedicamentoResponse;
import com.pachoclosystem.pachoclosystem.exception.SalidaNoConfirmadaException;
import com.pachoclosystem.pachoclosystem.exception.ServicioNoDisponibleException;
import com.pachoclosystem.pachoclosystem.exception.SolicitudInvalidaException;
import com.pachoclosystem.pachoclosystem.model.NivelExperiencia;
import com.pachoclosystem.pachoclosystem.service.AlmacenIdempotenciaHistorial;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code Idempotency-Key} en {@code POST /api/pacientes/{id}/historial}, con el
 * cliente de medicamentos simulado: repeticiones, conflictos sin fuga de datos,
 * fallos que consumen o no la clave, concurrencia y comportamiento sin cabecera.
 */
class HistorialIdempotenciaTest extends MockMvcBaseTest {

    private static final String MENSAJE_CONFLICTO = "La clave de idempotencia ya se usó para otra petición.";
    private static final String MENSAJE_CLAVE_INVALIDA = "La cabecera Idempotency-Key debe tener entre 16 y "
            + "100 caracteres: letras sin tilde, dígitos, guion o guion bajo (por ejemplo, un UUID).";
    private static final MedicamentoResponse DOLEX = new MedicamentoResponse("MED-0001", "Dolex",
            "Paracetamol", "TABLETA", "500 mg", "GSK", "L-1", 8, 5, LocalDate.of(2027, 3, 31), "Estante A3",
            false, false);

    @MockitoBean
    private MedicamentosClient cliente;

    @Autowired
    private AlmacenIdempotenciaHistorial almacen;

    private static String nuevaClave() {
        return UUID.randomUUID().toString();
    }

    private ResultActions postConClave(String trabajador, String paciente, String cuerpo, String clave)
            throws Exception {
        return mockMvc.perform(post("/api/pacientes/{id}/historial", paciente)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDeTrabajador(trabajador))
                .header("Idempotency-Key", clave)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    private static String medicacion(String idMedicamento, int cantidad) {
        return """
                {"tipo":"MEDICACION","contenido":"Paracetamol 500 mg via oral",
                 "idMedicamento":"%s","cantidad":%d}""".formatted(idMedicamento, cantidad);
    }

    private static String diagnostico(String contenido) {
        return "{\"tipo\":\"DIAGNOSTICO\",\"contenido\":\"" + contenido + "\"}";
    }

    private static String cuerpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private void registrosDe(String paciente, int cuantos) throws Exception {
        perform(get("/api/pacientes/{id}/historial", paciente)).andExpect(jsonPath("$", hasSize(cuantos)));
    }

    // --- Repetición con la misma clave ------------------------------------

    @Test
    void repetirConLaMismaClaveDevuelveElMismoRegistroSinSegundaSalida() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        String clave = nuevaClave();
        when(cliente.registrarSalida("MED-0001", 2, clave)).thenReturn(DOLEX);

        MvcResult primera = postConClave(doctor, paciente, medicacion("MED-0001", 2), clave)
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotency-Replayed"))
                .andExpect(jsonPath("$.medicacion.cantidad").value(2))
                .andReturn();
        MvcResult segunda = postConClave(doctor, paciente, medicacion("MED-0001", 2), clave)
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andReturn();

        assertThat(cuerpo(segunda)).isEqualTo(cuerpo(primera));
        verify(cliente, times(1)).registrarSalida("MED-0001", 2, clave);
        verify(cliente, never()).registrarSalida(anyString(), anyInt());
        registrosDe(paciente, 1);
    }

    @Test
    void laClaveTambienProtegeRegistrosSinDescuento() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String enfermero = registrarEnfermero("Luisa Rios", NivelExperiencia.AVANZADO);
        String clave = nuevaClave();
        String signos = """
                {"tipo":"SIGNOS_VITALES","signosVitales":{"temperatura":36.5,"frecCardiaca":80,
                 "presionSistolica":120,"presionDiastolica":80,"frecRespiratoria":16,"saturacion":98}}""";

        String id = leer(postConClave(enfermero, paciente, signos, clave)
                .andExpect(status().isCreated()).andReturn(), "$.idRegistro");
        postConClave(enfermero, paciente, signos, clave)
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andExpect(jsonPath("$.idRegistro").value(id));

        registrosDe(paciente, 1);
        verifyNoInteractions(cliente);
    }

    @Test
    void laHuellaNoDependeDeEspaciosNiDelOrdenDeLosCampos() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        String clave = nuevaClave();

        postConClave(doctor, paciente, "{\"tipo\":\"EVOLUCION\",\"contenido\":\"Paciente estable\"}", clave)
                .andExpect(status().isCreated());
        postConClave(doctor, paciente, "{ \"contenido\" : \"Paciente estable\",  \"tipo\" : \"EVOLUCION\" }",
                clave)
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "true"));

        registrosDe(paciente, 1);
    }

    // --- 409 sin fuga de datos --------------------------------------------

    /** Un 409 solo lleva el mensaje genérico: nada del registro original. */
    private void conflictoSinDatos(ResultActions peticion, String... secretos) throws Exception {
        MvcResult resultado = peticion
                .andExpect(status().isConflict())
                .andExpect(header().doesNotExist("Idempotency-Replayed"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensajes", contains(MENSAJE_CONFLICTO)))
                .andReturn();
        String json = cuerpo(resultado);
        assertThat(json).doesNotContain("idRegistro", "contenido", "autor", "medicacion");
        for (String secreto : secretos) {
            assertThat(json).doesNotContain(secreto);
        }
    }

    @Test
    void mismaClaveConOtroCuerpoDevuelve409SinDatosDelOriginal() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        String clave = nuevaClave();
        when(cliente.registrarSalida("MED-0001", 2, clave)).thenReturn(DOLEX);
        String id = leer(postConClave(doctor, paciente, medicacion("MED-0001", 2), clave)
                .andExpect(status().isCreated()).andReturn(), "$.idRegistro");

        conflictoSinDatos(postConClave(doctor, paciente, medicacion("MED-0001", 3), clave),
                id, "Ana Torres", paciente, doctor, "Paracetamol");

        verify(cliente, times(1)).registrarSalida(anyString(), anyInt(), anyString());
        registrosDe(paciente, 1);
    }

    @Test
    void mismaClaveConOtroPacienteDevuelve409SinDatosDelOriginal() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String otroPaciente = registrarPaciente("Bruno Diaz", 40, 102);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        String clave = nuevaClave();
        String id = leer(postConClave(doctor, paciente, diagnostico("Hipertension leve"), clave)
                .andExpect(status().isCreated()).andReturn(), "$.idRegistro");

        conflictoSinDatos(postConClave(doctor, otroPaciente, diagnostico("Hipertension leve"), clave),
                id, "Ana Torres", paciente, "Hipertension");

        registrosDe(paciente, 1);
        registrosDe(otroPaciente, 0);
    }

    @Test
    void mismaClaveConOtroUsuarioDevuelve409SinDatosDelOriginal() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        String otroDoctor = registrarDoctor("Diana Paz", "Pediatria");
        String clave = nuevaClave();
        when(cliente.registrarSalida("MED-0001", 2, clave)).thenReturn(DOLEX);
        String id = leer(postConClave(doctor, paciente, medicacion("MED-0001", 2), clave)
                .andExpect(status().isCreated()).andReturn(), "$.idRegistro");

        // Mismo paciente y mismo cuerpo, pero otro usuario.
        conflictoSinDatos(postConClave(otroDoctor, paciente, medicacion("MED-0001", 2), clave),
                id, "Carlos Mena", doctor);

        verify(cliente, times(1)).registrarSalida(anyString(), anyInt(), anyString());
        registrosDe(paciente, 1);
    }

    // --- Validación de la clave -------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"corta", "clave con espacios 123", "clave-con-tilde-ñññññññ",
            "clave/con/barras/0000000"})
    void claveMalFormadaDevuelve400YNoCreaNada(String clave) throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");

        postConClave(doctor, paciente, diagnostico("Hipertension leve"), clave)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes", contains(MENSAJE_CLAVE_INVALIDA)));

        registrosDe(paciente, 0);
        verifyNoInteractions(cliente);
    }

    @Test
    void claveDemasiadoLargaDevuelve400() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");

        postConClave(doctor, paciente, diagnostico("Hipertension leve"), "a".repeat(101))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes", contains(MENSAJE_CLAVE_INVALIDA)));
        registrosDe(paciente, 0);
    }

    // --- Fallos: cuáles consumen la clave ---------------------------------

    @Test
    void unFalloQueNoCambioNadaNoConsumeLaClave() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        String otroDoctor = registrarDoctor("Diana Paz", "Pediatria");
        String clave = nuevaClave();
        when(cliente.registrarSalida("MED-0001", 50, clave)).thenThrow(
                new SolicitudInvalidaException("Stock insuficiente: disponible 10, solicitado 50."));

        postConClave(doctor, paciente, medicacion("MED-0001", 50), clave)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes", contains("Stock insuficiente: disponible 10, solicitado 50.")));
        registrosDe(paciente, 0);

        // La clave sigue libre: otra petición con ella se evalúa como nueva.
        when(cliente.registrarSalida("MED-0001", 5, clave)).thenReturn(DOLEX);
        postConClave(otroDoctor, paciente, medicacion("MED-0001", 5), clave)
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotency-Replayed"));
        registrosDe(paciente, 1);
    }

    @Test
    void salidaSinConfirmarDevuelve503ReintentableYElReintentoCreaUnSoloRegistro() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        String clave = nuevaClave();
        when(cliente.registrarSalida("MED-0001", 2, clave))
                .thenThrow(new SalidaNoConfirmadaException("sin respuesta", new SocketTimeoutException()))
                .thenReturn(DOLEX);

        postConClave(doctor, paciente, medicacion("MED-0001", 2), clave)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensajes", contains(
                        "No se pudo confirmar; puede reintentar sin riesgo de descontar dos veces.")));
        registrosDe(paciente, 0);

        postConClave(doctor, paciente, medicacion("MED-0001", 2), clave)
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotency-Replayed"));
        registrosDe(paciente, 1);
        // Las dos salidas llevan la misma clave (MedicamentosService no descuenta dos veces).
        verify(cliente, times(2)).registrarSalida("MED-0001", 2, clave);
    }

    @Test
    void trasUnaSalidaSinConfirmarLaClaveQuedaLigadaASuPeticion() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        String otroDoctor = registrarDoctor("Diana Paz", "Pediatria");
        String clave = nuevaClave();
        when(cliente.registrarSalida("MED-0001", 2, clave))
                .thenThrow(new SalidaNoConfirmadaException("sin respuesta", new SocketTimeoutException()));

        postConClave(doctor, paciente, medicacion("MED-0001", 2), clave)
                .andExpect(status().isServiceUnavailable());

        // Puede que la salida se hiciera: nadie más puede usar la clave con otra petición.
        conflictoSinDatos(postConClave(otroDoctor, paciente, medicacion("MED-0001", 2), clave));
        conflictoSinDatos(postConClave(doctor, paciente, medicacion("MED-0001", 3), clave));
        verify(cliente, times(1)).registrarSalida(anyString(), anyInt(), anyString());
    }

    // --- Concurrencia -----------------------------------------------------

    @Test
    void dosPeticionesSimultaneasConLaMismaClaveCreanUnSoloRegistro() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        String token = tokenDeTrabajador(doctor);
        String clave = nuevaClave();
        CountDownLatch salidaEnCurso = new CountDownLatch(1);
        CountDownLatch soltarSalida = new CountDownLatch(1);
        when(cliente.registrarSalida("MED-0001", 2, clave)).thenAnswer(invocacion -> {
            salidaEnCurso.countDown();
            assertThat(soltarSalida.await(10, TimeUnit.SECONDS)).isTrue();
            return DOLEX;
        });

        CompletableFuture<MvcResult> primera = CompletableFuture.supplyAsync(() -> enviar(token, paciente, clave));
        assertThat(salidaEnCurso.await(10, TimeUnit.SECONDS)).isTrue();
        CompletableFuture<MvcResult> segunda = CompletableFuture.supplyAsync(() -> enviar(token, paciente, clave));
        // La segunda ya está esperando el turno de la clave mientras la primera sigue en la salida.
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (almacen.peticionesConClave(clave) < 2) {
            assertThat(System.nanoTime()).as("la segunda petición no llegó").isLessThan(limite);
            Thread.sleep(10);
        }
        soltarSalida.countDown();

        List<MvcResult> resultados = List.of(primera.get(10, TimeUnit.SECONDS), segunda.get(10, TimeUnit.SECONDS));
        assertThat(resultados).allSatisfy(r -> assertThat(r.getResponse().getStatus()).isEqualTo(201));
        assertThat(leer(resultados.get(0), "$.idRegistro")).isEqualTo(leer(resultados.get(1), "$.idRegistro"));
        assertThat(resultados).filteredOn(r -> "true".equals(r.getResponse().getHeader("Idempotency-Replayed")))
                .hasSize(1);
        verify(cliente, times(1)).registrarSalida("MED-0001", 2, clave);
        registrosDe(paciente, 1);
    }

    private MvcResult enviar(String token, String paciente, String clave) {
        try {
            return mockMvc.perform(post("/api/pacientes/{id}/historial", paciente)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .header("Idempotency-Key", clave)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(medicacion("MED-0001", 2)))
                    .andReturn();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // --- Sin cabecera: como siempre ---------------------------------------

    @Test
    void sinCabeceraCadaPeticionCreaSuRegistroYDescuenta() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        when(cliente.registrarSalida("MED-0001", 2)).thenReturn(DOLEX);
        String cuerpo = medicacion("MED-0001", 2);

        postHistorialComo(doctor, paciente, cuerpo)
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotency-Replayed"));
        postHistorialComo(doctor, paciente, cuerpo)
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotency-Replayed"));

        registrosDe(paciente, 2);
        verify(cliente, times(2)).registrarSalida("MED-0001", 2);
        verify(cliente, never()).registrarSalida(anyString(), anyInt(), anyString());
    }

    @Test
    void sinCabeceraElServicioCaidoDevuelveElMismo503DeSiempre() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        when(cliente.registrarSalida("MED-0001", 2)).thenThrow(
                new ServicioNoDisponibleException("MedicamentosService no responde", new ConnectException()));

        postHistorialComo(doctor, paciente, medicacion("MED-0001", 2))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensajes", contains(
                        "El servicio de medicamentos no está disponible. Vuelva a intentarlo más tarde.")));

        verify(cliente, times(1)).registrarSalida("MED-0001", 2);
        verify(cliente, never()).registrarSalida(anyString(), anyInt(), anyString());
        registrosDe(paciente, 0);
    }
}
