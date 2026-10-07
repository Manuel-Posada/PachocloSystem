package com.pachoclosystem.pachoclosystem.client;

import com.pachoclosystem.pachoclosystem.dto.ActualizarMedicamentoRequest;
import com.pachoclosystem.pachoclosystem.dto.ErrorResponse;
import com.pachoclosystem.pachoclosystem.dto.MedicamentoRequest;
import com.pachoclosystem.pachoclosystem.dto.MedicamentoResponse;
import com.pachoclosystem.pachoclosystem.exception.ConflictoException;
import com.pachoclosystem.pachoclosystem.exception.NotFoundException;
import com.pachoclosystem.pachoclosystem.exception.RespuestaServicioInvalidaException;
import com.pachoclosystem.pachoclosystem.exception.SalidaNoConfirmadaException;
import com.pachoclosystem.pachoclosystem.exception.ServicioNoDisponibleException;
import com.pachoclosystem.pachoclosystem.exception.SolicitudInvalidaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 *
 * <p>Las salidas con clave de idempotencia ({@link #registrarSalida(String, int, String)})
 * se reintentan una vez si no responden; si tampoco responde el reintento,
 * {@link SalidaNoConfirmadaException} (503, se puede reintentar).</p>
 */
@Component
public class MedicamentosClient {

    private static final Logger LOG = LoggerFactory.getLogger(MedicamentosClient.class);
    private static final String RUTA = "/api/medicamentos";
    /** Cabecera de MedicamentosService para que repetir una salida no vuelva a descontar. */
    private static final String CABECERA_IDEMPOTENCIA = "Idempotency-Key";
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
        return movimiento(id, "entradas", cantidad, null);
    }

    public MedicamentoResponse registrarSalida(String id, int cantidad) {
        return movimiento(id, "salidas", cantidad, null);
    }

    /**
     * Salida con {@code Idempotency-Key}: MedicamentosService no descuenta dos
     * veces con la misma clave, así que si no responde (timeout o error de E/S)
     * se reintenta una vez con la misma clave. Si el reintento tampoco responde,
     * no se sabe si se descontó: {@link SalidaNoConfirmadaException}. Las
     * respuestas de error (400, 404, 409, 5xx) y los cuerpos mal formados no se
     * reintentan.
     */
    public MedicamentoResponse registrarSalida(String id, int cantidad, String claveIdempotencia) {
        try {
            return salidaConClave(id, cantidad, claveIdempotencia);
        } catch (ServicioNoDisponibleException primerFallo) {
            LOG.warn("Salida de stock sin respuesta; se reintenta una vez con la misma clave: {}",
                    primerFallo.getMessage());
        }
        try {
            return salidaConClave(id, cantidad, claveIdempotencia);
        } catch (ServicioNoDisponibleException segundoFallo) {
            throw new SalidaNoConfirmadaException("Salida de stock sin respuesta tras reintentar: "
                    + segundoFallo.getMessage(), segundoFallo);
        }
    }

    /**
     * Una salida con clave. Si el tiempo se agota (o la conexión se corta) cuando
     * ya llegaron las cabeceras pero no el cuerpo, Spring no lo da como error de
     * E/S sino como error al leer la respuesta: aquí, si la causa es de E/S, se
     * trata como un timeout. Un cuerpo mal formado no tiene causa de E/S y sigue
     * siendo respuesta inválida (502). Sin clave no se hace esta distinción.
     */
    private MedicamentoResponse salidaConClave(String id, int cantidad, String claveIdempotencia) {
        try {
            return movimiento(id, "salidas", cantidad, claveIdempotencia);
        } catch (RespuestaServicioInvalidaException fallo) {
            if (fallo.getCause() != null && tieneCausaDeEntradaSalida(fallo.getCause())) {
                throw new ServicioNoDisponibleException(
                        "MedicamentosService no terminó de responder: " + fallo.getMessage(), fallo);
            }
            throw fallo;
        }
    }

    private static boolean tieneCausaDeEntradaSalida(Throwable fallo) {
        for (Throwable causa = fallo; causa != null; causa = causa.getCause()) {
            if (causa instanceof IOException) {
                return true;
            }
            if (causa.getCause() == causa) {
                break;
            }
        }
        return false;
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

    /** {@code claveIdempotencia} nula = sin cabecera {@code Idempotency-Key}. */
    private MedicamentoResponse movimiento(String id, String tipo, int cantidad, String claveIdempotencia) {
        return ejecutar(() -> {
            RestClient.RequestBodySpec peticion = http.post()
                    .uri(RUTA + "/{id}/" + tipo, id)
                    .contentType(MediaType.APPLICATION_JSON);
            if (claveIdempotencia != null) {
                peticion.header(CABECERA_IDEMPOTENCIA, claveIdempotencia);
            }
            return peticion.body(Map.of("cantidad", cantidad))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, this::traducirError)
                    .body(MedicamentoResponse.class);
        });
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
