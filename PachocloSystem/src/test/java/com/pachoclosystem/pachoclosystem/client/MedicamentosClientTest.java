package com.pachoclosystem.pachoclosystem.client;

import com.pachoclosystem.pachoclosystem.config.MedicamentosClientConfig;
import com.pachoclosystem.pachoclosystem.config.MedicamentosProperties;
import com.pachoclosystem.pachoclosystem.dto.MedicamentoRequest;
import com.pachoclosystem.pachoclosystem.dto.MedicamentoResponse;
import com.pachoclosystem.pachoclosystem.exception.ConflictoException;
import com.pachoclosystem.pachoclosystem.exception.NotFoundException;
import com.pachoclosystem.pachoclosystem.exception.RespuestaServicioInvalidaException;
import com.pachoclosystem.pachoclosystem.exception.ServicioNoDisponibleException;
import com.pachoclosystem.pachoclosystem.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Cliente HTTP de MedicamentosService sin levantar el otro servicio: las
 * respuestas las simula {@link MockRestServiceServer}.
 */
class MedicamentosClientTest {

    private static final String BASE = "http://medicamentos.test";
    private static final String MEDICAMENTO_JSON = """
            {"idMedicamento":"MED-0001","nombre":"Dolex","principioActivo":"Paracetamol",
             "presentacion":"TABLETA","concentracion":"500 mg","laboratorio":"GSK","lote":"L-1",
             "cantidadStock":10,"stockMinimo":5,"fechaVencimiento":"2027-03-31",
             "ubicacion":"Estante A3","stockBajo":false,"vencido":false}""";

    private MockRestServiceServer servidor;
    private MedicamentosClient cliente;

    @BeforeEach
    void preparar() {
        prepararConClave("clave-de-prueba");
    }

    private void prepararConClave(String apiKey) {
        MedicamentosProperties propiedades = new MedicamentosProperties(
                URI.create(BASE), Duration.ofSeconds(1), Duration.ofSeconds(1), apiKey);
        RestClient.Builder constructor = MedicamentosClientConfig.configurar(RestClient.builder(), propiedades);
        servidor = MockRestServiceServer.bindTo(constructor).build();
        cliente = new MedicamentosClient(constructor.build());
    }

    private static String error(int status, String error, String... mensajes) {
        String lista = String.join("\",\"", mensajes);
        return "{\"status\":" + status + ",\"error\":\"" + error + "\",\"mensajes\":[\"" + lista + "\"]}";
    }

    // --- Peticiones y respuestas correctas --------------------------------

    @Test
    void obtenerEnviaLaClaveYMapeaElMedicamento() {
        servidor.expect(requestTo(BASE + "/api/medicamentos/MED-0001"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Api-Key", "clave-de-prueba"))
                .andRespond(withSuccess(MEDICAMENTO_JSON, MediaType.APPLICATION_JSON));

        MedicamentoResponse medicamento = cliente.obtener("MED-0001");

        assertThat(medicamento.idMedicamento()).isEqualTo("MED-0001");
        assertThat(medicamento.cantidadStock()).isEqualTo(10);
        assertThat(medicamento.fechaVencimiento()).isEqualTo(LocalDate.of(2027, 3, 31));
        servidor.verify();
    }

    @Test
    void sinClaveConfiguradaNoSeEnviaLaCabecera() {
        prepararConClave("");
        servidor.expect(requestTo(BASE + "/api/medicamentos"))
                .andExpect(headerDoesNotExist("X-Api-Key"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(cliente.listar(null)).isEmpty();
        servidor.verify();
    }

    @Test
    void listarReenviaElFiltro() {
        servidor.expect(requestTo(BASE + "/api/medicamentos?q=dolex"))
                .andRespond(withSuccess("[" + MEDICAMENTO_JSON + "]", MediaType.APPLICATION_JSON));

        List<MedicamentoResponse> lista = cliente.listar("dolex");

        assertThat(lista).extracting(MedicamentoResponse::nombre).containsExactly("Dolex");
        servidor.verify();
    }

    @Test
    void registrarEnviaElCuerpoComoJson() {
        servidor.expect(requestTo(BASE + "/api/medicamentos"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"nombre":"Dolex","presentacion":"TABLETA","cantidadStock":10,
                         "fechaVencimiento":"2027-03-31"}"""))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                        .body(MEDICAMENTO_JSON));

        MedicamentoResponse creado = cliente.registrar(new MedicamentoRequest("Dolex", "Paracetamol",
                "TABLETA", "500 mg", "GSK", "L-1", 10, 5, LocalDate.of(2027, 3, 31), "Estante A3"));

        assertThat(creado.idMedicamento()).isEqualTo("MED-0001");
        servidor.verify();
    }

    @Test
    void salidaEnviaLaCantidad() {
        servidor.expect(requestTo(BASE + "/api/medicamentos/MED-0001/salidas"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"cantidad\":3}"))
                .andRespond(withSuccess(MEDICAMENTO_JSON, MediaType.APPLICATION_JSON));

        cliente.registrarSalida("MED-0001", 3);
        servidor.verify();
    }

    @Test
    void porVencerSinDiasNoEnviaElParametro() {
        servidor.expect(requestTo(BASE + "/api/medicamentos/por-vencer"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(BASE + "/api/medicamentos/por-vencer?dias=90"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        cliente.listarPorVencer(null);
        cliente.listarPorVencer(90);
        servidor.verify();
    }

    @Test
    void eliminarAceptaElCodigo204() {
        servidor.expect(requestTo(BASE + "/api/medicamentos/MED-0001"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        cliente.eliminar("MED-0001");
        servidor.verify();
    }

    // --- Traducción de errores --------------------------------------------

    @Test
    void error404SeTraduceANotFoundConElMismoMensaje() {
        servidor.expect(requestTo(BASE + "/api/medicamentos/MED-0009"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                        .body(error(404, "Not Found", "No se encontró el medicamento MED-0009.")));

        assertThatExceptionOfType(NotFoundException.class)
                .isThrownBy(() -> cliente.obtener("MED-0009"))
                .withMessage("No se encontró el medicamento MED-0009.");
    }

    @Test
    void error400SeTraduceASolicitudInvalidaConTodosLosMensajes() {
        servidor.expect(requestTo(BASE + "/api/medicamentos"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body(error(400, "Bad Request", "El lote es obligatorio.", "El nombre es obligatorio.")));

        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> cliente.registrar(new MedicamentoRequest(
                        null, null, null, null, null, null, null, null, null, null)))
                .satisfies(e -> assertThat(e.getErrores())
                        .containsExactly("El lote es obligatorio.", "El nombre es obligatorio."));
    }

    @Test
    void stockInsuficienteLlegaComo400ConSuMensaje() {
        servidor.expect(requestTo(BASE + "/api/medicamentos/MED-0001/salidas"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body(error(400, "Bad Request", "Stock insuficiente: disponible 10, solicitado 11.")));

        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> cliente.registrarSalida("MED-0001", 11))
                .satisfies(e -> assertThat(e.getErrores())
                        .containsExactly("Stock insuficiente: disponible 10, solicitado 11."));
    }

    @Test
    void error409SeTraduceAConflicto() {
        String mensaje = "Ya existe un medicamento con el mismo nombre, concentración, presentación y lote.";
        servidor.expect(requestTo(BASE + "/api/medicamentos"))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                        .body(error(409, "Conflict", mensaje)));

        assertThatExceptionOfType(ConflictoException.class)
                .isThrownBy(() -> cliente.registrar(new MedicamentoRequest(
                        null, null, null, null, null, null, null, null, null, null)))
                .withMessage(mensaje);
    }

    @Test
    void error500SeTraduceARespuestaInvalida() {
        servidor.expect(requestTo(BASE + "/api/medicamentos"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON)
                        .body(error(500, "Internal Server Error", "Se produjo un error interno.")));

        assertThatExceptionOfType(RespuestaServicioInvalidaException.class)
                .isThrownBy(() -> cliente.listar(null));
    }

    @Test
    void claveRechazadaSeTraduceARespuestaInvalida() {
        servidor.expect(requestTo(BASE + "/api/medicamentos"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON)
                        .body(error(401, "Unauthorized", "Falta la clave de servicio o no es válida.")));

        assertThatExceptionOfType(RespuestaServicioInvalidaException.class)
                .isThrownBy(() -> cliente.listar(null));
    }

    @Test
    void errorConCuerpoIlegibleSeTraduceARespuestaInvalida() {
        servidor.expect(requestTo(BASE + "/api/medicamentos/MED-0001"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.TEXT_HTML)
                        .body("<html>no encontrado</html>"));

        assertThatExceptionOfType(RespuestaServicioInvalidaException.class)
                .isThrownBy(() -> cliente.obtener("MED-0001"));
    }

    @Test
    void exitoConCuerpoIlegibleSeTraduceARespuestaInvalida() {
        servidor.expect(requestTo(BASE + "/api/medicamentos/MED-0001"))
                .andRespond(withSuccess("esto no es json", MediaType.APPLICATION_JSON));

        assertThatExceptionOfType(RespuestaServicioInvalidaException.class)
                .isThrownBy(() -> cliente.obtener("MED-0001"));
    }

    @Test
    void conexionRechazadaSeTraduceAServicioNoDisponible() {
        servidor.expect(requestTo(BASE + "/api/medicamentos"))
                .andRespond(withException(new ConnectException("Connection refused")));

        assertThatExceptionOfType(ServicioNoDisponibleException.class)
                .isThrownBy(() -> cliente.listar(null));
    }

    @Test
    void timeoutSeTraduceAServicioNoDisponible() {
        servidor.expect(requestTo(BASE + "/api/medicamentos/MED-0001/salidas"))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatExceptionOfType(ServicioNoDisponibleException.class)
                .isThrownBy(() -> cliente.registrarSalida("MED-0001", 1));
    }
}
