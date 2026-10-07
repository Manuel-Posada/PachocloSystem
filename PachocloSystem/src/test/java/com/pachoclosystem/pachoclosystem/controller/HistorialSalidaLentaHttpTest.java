package com.pachoclosystem.pachoclosystem.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cliente HTTP real (el bean de la aplicación, con un tiempo de lectura de
 * 300 ms) contra un stub HTTP de MedicamentosService que imita su contrato de
 * {@code Idempotency-Key}: descuenta y recuerda la clave <em>antes</em> de
 * tardar en responder, así que la respuesta se pierde por timeout después de
 * haber descontado, que es el caso que hay que cubrir.
 */
class HistorialSalidaLentaHttpTest extends MockMvcBaseTest {

    private static final Duration MAS_QUE_LA_LECTURA = Duration.ofMillis(1500);
    private static final String MENSAJE_NO_CONFIRMADA =
            "No se pudo confirmar; puede reintentar sin riesgo de descontar dos veces.";
    private static final String MENSAJE_NO_DISPONIBLE =
            "El servicio de medicamentos no está disponible. Vuelva a intentarlo más tarde.";
    private static final String MENSAJE_RESPUESTA_INESPERADA =
            "El servicio de medicamentos respondió de forma inesperada.";

    private static final StubMedicamentos STUB = StubMedicamentos.arrancar();

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("medicamentos.url", STUB::url);
        registro.add("medicamentos.timeout-lectura", () -> "300ms");
    }

    @AfterAll
    static void pararStub() {
        STUB.parar();
    }

    @BeforeEach
    void reiniciarStub() {
        STUB.reiniciar(100);
    }

    private ResultActions registrar(String doctor, String paciente, String clave) throws Exception {
        var peticion = post("/api/pacientes/{id}/historial", paciente)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDeTrabajador(doctor))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"tipo":"MEDICACION","contenido":"Paracetamol 500 mg via oral",
                         "idMedicamento":"MED-0001","cantidad":2}""");
        if (clave != null) {
            peticion.header("Idempotency-Key", clave);
        }
        return mockMvc.perform(peticion);
    }

    private void registrosDe(String paciente, int cuantos) throws Exception {
        perform(get("/api/pacientes/{id}/historial", paciente)).andExpect(jsonPath("$", hasSize(cuantos)));
    }

    @Test
    void respuestaLentaSeReintentaConLaMismaClaveYDescuentaUnaSolaVez() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        String clave = UUID.randomUUID().toString();
        STUB.retrasar(MAS_QUE_LA_LECTURA);

        registrar(doctor, paciente, clave)
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotency-Replayed"))
                .andExpect(jsonPath("$.medicacion.cantidad").value(2));

        // La primera se descontó pero su respuesta llegó tarde; el reintento lleva la misma clave.
        assertThat(STUB.clavesRecibidas()).containsExactly(clave, clave);
        assertThat(STUB.stock()).isEqualTo(98);
        registrosDe(paciente, 1);

        // Repetir la petición devuelve el registro sin volver a llamar a MedicamentosService.
        registrar(doctor, paciente, clave)
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "true"));
        assertThat(STUB.clavesRecibidas()).hasSize(2);
        assertThat(STUB.stock()).isEqualTo(98);
        registrosDe(paciente, 1);
    }

    @Test
    void sinRespuestaNiAlReintentoDevuelve503YReintentarNoDescuentaDosVeces() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        String clave = UUID.randomUUID().toString();
        STUB.retrasar(MAS_QUE_LA_LECTURA, MAS_QUE_LA_LECTURA);

        registrar(doctor, paciente, clave)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensajes", contains(MENSAJE_NO_CONFIRMADA)));
        assertThat(STUB.clavesRecibidas()).containsExactly(clave, clave);
        assertThat(STUB.stock()).isEqualTo(98);
        registrosDe(paciente, 0);

        // El cliente hace caso al mensaje y reintenta con la misma clave: un registro, un descuento.
        registrar(doctor, paciente, clave)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.medicacion.cantidad").value(2));
        assertThat(STUB.clavesRecibidas()).containsExactly(clave, clave, clave);
        assertThat(STUB.stock()).isEqualTo(98);
        registrosDe(paciente, 1);
    }

    @Test
    void sinCabeceraUnaRespuestaLentaNoSeReintentaYDevuelveEl503DeSiempre() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        STUB.retrasar(MAS_QUE_LA_LECTURA);

        registrar(doctor, paciente, null)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensajes", contains(MENSAJE_NO_DISPONIBLE)));

        // Una sola petición, sin clave. El stock queda descontado sin registro: el límite
        // conocido sin cabecera, que es justo lo que la clave resuelve.
        assertThat(STUB.clavesRecibidas()).containsExactly((String) null);
        assertThat(STUB.stock()).isEqualTo(98);
        registrosDe(paciente, 0);
    }

    // --- Tiempo agotado leyendo el cuerpo, y cuerpo corrupto ---------------

    @Test
    void cabecerasATiempoYCuerpoTardioConClaveSeReintentaYAcabaEn503() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        String clave = UUID.randomUUID().toString();
        STUB.retrasarCuerpo(MAS_QUE_LA_LECTURA, MAS_QUE_LA_LECTURA);

        registrar(doctor, paciente, clave)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensajes", contains(MENSAJE_NO_CONFIRMADA)));

        // Se trató como timeout: un reintento con la misma clave y un solo descuento.
        assertThat(STUB.clavesRecibidas()).containsExactly(clave, clave);
        assertThat(STUB.stock()).isEqualTo(98);
        registrosDe(paciente, 0);

        // Repetir la petición termina con un registro y sin segundo descuento.
        registrar(doctor, paciente, clave).andExpect(status().isCreated());
        assertThat(STUB.stock()).isEqualTo(98);
        registrosDe(paciente, 1);
    }

    @Test
    void cuerpoTardioSoloEnElPrimerIntentoConClaveCreaElRegistro() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        String clave = UUID.randomUUID().toString();
        STUB.retrasarCuerpo(MAS_QUE_LA_LECTURA);

        registrar(doctor, paciente, clave)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.medicacion.cantidad").value(2));

        assertThat(STUB.clavesRecibidas()).containsExactly(clave, clave);
        assertThat(STUB.stock()).isEqualTo(98);
        registrosDe(paciente, 1);
    }

    @Test
    void cuerpoCorruptoConClaveSigueSiendo502SinReintento() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        String clave = UUID.randomUUID().toString();
        STUB.corromper();

        registrar(doctor, paciente, clave)
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.mensajes", contains(MENSAJE_RESPUESTA_INESPERADA)));

        assertThat(STUB.clavesRecibidas()).containsExactly(clave);
        assertThat(STUB.stock()).isEqualTo(98);
        registrosDe(paciente, 0);
    }

    @Test
    void sinCabeceraElCuerpoTardioSigueSiendo502SinReintento() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        STUB.retrasarCuerpo(MAS_QUE_LA_LECTURA);

        // Límite conocido sin clave: igual que antes de B4b.
        registrar(doctor, paciente, null)
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.mensajes", contains(MENSAJE_RESPUESTA_INESPERADA)));

        assertThat(STUB.clavesRecibidas()).containsExactly((String) null);
        assertThat(STUB.stock()).isEqualTo(98);
        registrosDe(paciente, 0);
    }

    /**
     * Stub de {@code POST /api/medicamentos/{id}/salidas} con la semántica de
     * MedicamentosService: con una clave ya usada devuelve la respuesta guardada
     * sin descontar. Cada petición se comporta como diga la cola
     * ({@link #retrasar}, {@link #retrasarCuerpo}, {@link #corromper}), siempre
     * <em>después</em> de aplicar la salida; sin indicación, responde enseguida.
     */
    private static final class StubMedicamentos {

        private static final Pattern CANTIDAD = Pattern.compile("\"cantidad\"\\s*:\\s*(\\d+)");

        /** Espera antes de las cabeceras, espera entre cabeceras y cuerpo, y si el cuerpo no es JSON. */
        private record Comportamiento(Duration antesDeCabeceras, Duration antesDelCuerpo, boolean corrupto) {
        }

        private final HttpServer servidor;
        private final ExecutorService hilos = Executors.newCachedThreadPool();
        private final Queue<Comportamiento> comportamientos = new ConcurrentLinkedQueue<>();
        private final List<String> claves = new ArrayList<>();
        private final Map<String, String> respuestas = new HashMap<>();
        private int stock;

        private StubMedicamentos(HttpServer servidor) {
            this.servidor = servidor;
        }

        static StubMedicamentos arrancar() {
            try {
                HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                StubMedicamentos stub = new StubMedicamentos(servidor);
                servidor.createContext("/api/medicamentos/", stub::atender);
                servidor.setExecutor(stub.hilos);
                servidor.start();
                return stub;
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        String url() {
            return "http://127.0.0.1:" + servidor.getAddress().getPort();
        }

        synchronized void reiniciar(int stockInicial) {
            comportamientos.clear();
            claves.clear();
            respuestas.clear();
            stock = stockInicial;
        }

        /** Las siguientes peticiones tardan eso antes de enviar nada. */
        void retrasar(Duration... porPeticion) {
            for (Duration retraso : porPeticion) {
                comportamientos.add(new Comportamiento(retraso, Duration.ZERO, false));
            }
        }

        /** Las siguientes peticiones envían las cabeceras enseguida y tardan eso en enviar el cuerpo. */
        void retrasarCuerpo(Duration... porPeticion) {
            for (Duration retraso : porPeticion) {
                comportamientos.add(new Comportamiento(Duration.ZERO, retraso, false));
            }
        }

        /** La siguiente petición responde 200 con un cuerpo que no es JSON. */
        void corromper() {
            comportamientos.add(new Comportamiento(Duration.ZERO, Duration.ZERO, true));
        }

        synchronized List<String> clavesRecibidas() {
            return new ArrayList<>(claves);
        }

        synchronized int stock() {
            return stock;
        }

        void parar() {
            servidor.stop(0);
            hilos.shutdownNow();
        }

        private void atender(HttpExchange intercambio) throws IOException {
            String clave = intercambio.getRequestHeaders().getFirst("Idempotency-Key");
            String cuerpo = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Matcher cantidad = CANTIDAD.matcher(cuerpo);
            if (!"POST".equals(intercambio.getRequestMethod())
                    || !intercambio.getRequestURI().getPath().endsWith("/salidas") || !cantidad.find()) {
                intercambio.sendResponseHeaders(404, -1);
                intercambio.close();
                return;
            }
            String respuesta;
            synchronized (this) {
                claves.add(clave);
                respuesta = clave == null ? null : respuestas.get(clave);
                if (respuesta == null) {
                    stock -= Integer.parseInt(cantidad.group(1));
                    respuesta = medicamento(stock);
                    if (clave != null) {
                        respuestas.put(clave, respuesta);
                    }
                }
            }
            Comportamiento comportamiento = comportamientos.poll();
            if (comportamiento == null) {
                comportamiento = new Comportamiento(Duration.ZERO, Duration.ZERO, false);
            }
            try {
                Thread.sleep(comportamiento.antesDeCabeceras());
                byte[] bytes = (comportamiento.corrupto() ? "esto no es json" : respuesta)
                        .getBytes(StandardCharsets.UTF_8);
                intercambio.getResponseHeaders().add("Content-Type", "application/json");
                intercambio.sendResponseHeaders(200, bytes.length);
                try (OutputStream salida = intercambio.getResponseBody()) {
                    salida.flush();
                    Thread.sleep(comportamiento.antesDelCuerpo());
                    salida.write(bytes);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException clienteSeFue) {
                // El cliente dejó de esperar: la salida ya está hecha, que es lo que se prueba.
            } finally {
                intercambio.close();
            }
        }

        private static String medicamento(int stock) {
            return """
                    {"idMedicamento":"MED-0001","nombre":"Dolex","principioActivo":"Paracetamol",
                     "presentacion":"TABLETA","concentracion":"500 mg","laboratorio":"GSK","lote":"L-1",
                     "cantidadStock":%d,"stockMinimo":5,"fechaVencimiento":"2027-03-31",
                     "ubicacion":"Estante A3","stockBajo":false,"vencido":false}""".formatted(stock);
        }
    }
}
