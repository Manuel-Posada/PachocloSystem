package com.pachoclosystem.pachoclosystem.client;

import com.pachoclosystem.pachoclosystem.dto.ActualizarMedicamentoRequest;
import com.pachoclosystem.pachoclosystem.dto.ErrorResponse;
import com.pachoclosystem.pachoclosystem.dto.MedicamentoRequest;
import com.pachoclosystem.pachoclosystem.dto.MedicamentoResponse;
import com.pachoclosystem.pachoclosystem.exception.ConflictoException;
import com.pachoclosystem.pachoclosystem.exception.NotFoundException;
import com.pachoclosystem.pachoclosystem.exception.RespuestaServicioInvalidaException;
import com.pachoclosystem.pachoclosystem.exception.ServicioNoDisponibleException;
import com.pachoclosystem.pachoclosystem.exception.SolicitudInvalidaException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Cliente HTTP de MedicamentosService. Es el único punto que conoce sus rutas y
 * el único que traduce sus errores a las excepciones de este servicio:
 *
 * <ul>
 *   <li>404 → {@link NotFoundException}, 400 → {@link SolicitudInvalidaException}
 *       y 409 → {@link ConflictoException}, con los mismos mensajes.</li>
 *   <li>Cualquier otra respuesta de error (5xx, 401/403 por clave mal
 *       configurada) o un cuerpo ilegible → {@link RespuestaServicioInvalidaException} (502).</li>
 *   <li>Conexión rechazada o timeout → {@link ServicioNoDisponibleException} (503).</li>
 * </ul>
 */
@Component
public class MedicamentosClient {

    private static final String RUTA = "/api/medicamentos";
    private static final ParameterizedTypeReference<List<MedicamentoResponse>> LISTA =
            new ParameterizedTypeReference<>() {
            };
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestClient http;

    public MedicamentosClient(@Qualifier("medicamentosRestClient") RestClient http) {
        this.http = http;
    }

    public List<MedicamentoResponse> listar(String q) {
        return ejecutar(() -> http.get()
                .uri(uri -> uri.path(RUTA).queryParamIfPresent("q", Optional.ofNullable(q)).build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::traducirError)
                .body(LISTA));
    }

    public MedicamentoResponse obtener(String id) {
        return ejecutar(() -> http.get()
                .uri(RUTA + "/{id}", id)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::traducirError)
                .body(MedicamentoResponse.class));
    }

    public MedicamentoResponse registrar(MedicamentoRequest solicitud) {
        return ejecutar(() -> http.post()
                .uri(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .body(solicitud)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::traducirError)
                .body(MedicamentoResponse.class));
    }

    public MedicamentoResponse editar(String id, ActualizarMedicamentoRequest solicitud) {
        return ejecutar(() -> http.put()
                .uri(RUTA + "/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(solicitud)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::traducirError)
                .body(MedicamentoResponse.class));
    }

    public void eliminar(String id) {
        ejecutar(() -> http.delete()
                .uri(RUTA + "/{id}", id)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::traducirError)
                .toBodilessEntity());
    }

    public MedicamentoResponse registrarEntrada(String id, int cantidad) {
        return movimiento(id, "entradas", cantidad);
    }

    public MedicamentoResponse registrarSalida(String id, int cantidad) {
        return movimiento(id, "salidas", cantidad);
    }

    public List<MedicamentoResponse> listarStockBajo() {
        return consulta("/stock-bajo");
    }

    /** {@code dias} nulo = el valor por defecto de MedicamentosService. */
    public List<MedicamentoResponse> listarPorVencer(Integer dias) {
        return ejecutar(() -> http.get()
                .uri(uri -> uri.path(RUTA + "/por-vencer")
                        .queryParamIfPresent("dias", Optional.ofNullable(dias)).build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::traducirError)
                .body(LISTA));
    }

    public List<MedicamentoResponse> listarVencidos() {
        return consulta("/vencidos");
    }

    private MedicamentoResponse movimiento(String id, String tipo, int cantidad) {
        return ejecutar(() -> http.post()
                .uri(RUTA + "/{id}/" + tipo, id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("cantidad", cantidad))
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::traducirError)
                .body(MedicamentoResponse.class));
    }

    private List<MedicamentoResponse> consulta(String subruta) {
        return ejecutar(() -> http.get()
                .uri(RUTA + subruta)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::traducirError)
                .body(LISTA));
    }

    /** Convierte los fallos de red y las respuestas ilegibles en excepciones propias. */
    private <T> T ejecutar(Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (ResourceAccessException fallo) {
            throw new ServicioNoDisponibleException(
                    "MedicamentosService no responde: " + fallo.getMessage(), fallo);
        } catch (RestClientException fallo) {
            throw new RespuestaServicioInvalidaException(
                    "Respuesta ilegible de MedicamentosService: " + fallo.getMessage(), fallo);
        }
    }

    /** Traduce una respuesta de error de MedicamentosService (mismo formato ErrorResponse). */
    private void traducirError(HttpRequest peticion, ClientHttpResponse respuesta) throws IOException {
        int estado = respuesta.getStatusCode().value();
        List<String> mensajes = leerMensajes(respuesta);
        if (mensajes.isEmpty() || !List.of(400, 404, 409).contains(estado)) {
            throw new RespuestaServicioInvalidaException("MedicamentosService respondió " + estado
                    + " a " + peticion.getMethod() + " " + peticion.getURI().getPath());
        }
        throw switch (estado) {
            case 404 -> new NotFoundException(mensajes.getFirst());
            case 409 -> new ConflictoException(mensajes.getFirst());
            default -> new SolicitudInvalidaException(mensajes);
        };
    }

    private static List<String> leerMensajes(ClientHttpResponse respuesta) {
        try {
            ErrorResponse error = JSON.readValue(respuesta.getBody(), ErrorResponse.class);
            return error == null || error.mensajes() == null ? List.of() : error.mensajes();
        } catch (Exception cuerpoIlegible) {
            return List.of();
        }
    }
}
