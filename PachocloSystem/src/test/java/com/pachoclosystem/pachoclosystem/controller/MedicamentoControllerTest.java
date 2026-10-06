package com.pachoclosystem.pachoclosystem.controller;

import com.pachoclosystem.pachoclosystem.client.MedicamentosClient;
import com.pachoclosystem.pachoclosystem.dto.ActualizarMedicamentoRequest;
import com.pachoclosystem.pachoclosystem.dto.MedicamentoRequest;
import com.pachoclosystem.pachoclosystem.dto.MedicamentoResponse;
import com.pachoclosystem.pachoclosystem.exception.ConflictoException;
import com.pachoclosystem.pachoclosystem.exception.NotFoundException;
import com.pachoclosystem.pachoclosystem.exception.RespuestaServicioInvalidaException;
import com.pachoclosystem.pachoclosystem.exception.ServicioNoDisponibleException;
import com.pachoclosystem.pachoclosystem.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.ConnectException;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoints de medicamentos del servicio principal: token obligatorio, reenvío
 * al cliente HTTP (simulado) y traducción de sus errores a códigos HTTP.
 */
class MedicamentoControllerTest extends MockMvcBaseTest {

    private static final String ALTA = """
            {"nombre":"Dolex","principioActivo":"Paracetamol","presentacion":"TABLETA",
             "concentracion":"500 mg","laboratorio":"GSK","lote":"L-1","cantidadStock":10,
             "stockMinimo":5,"fechaVencimiento":"2027-03-31","ubicacion":"Estante A3"}""";

    @MockitoBean
    private MedicamentosClient cliente;

    private static MedicamentoResponse medicamento(String id, int stock) {
        return new MedicamentoResponse(id, "Dolex", "Paracetamol", "TABLETA", "500 mg", "GSK", "L-1",
                stock, 5, LocalDate.of(2027, 3, 31), "Estante A3", stock <= 5, false);
    }

    // --- Autenticación ------------------------------------------------------

    @Test
    void sinTokenDevuelve401YNoLlamaAlServicioDeMedicamentos() throws Exception {
        sinToken(get("/api/medicamentos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        sinToken(post("/api/medicamentos/MED-0001/salidas")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":1}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(cliente);
    }

    // --- Reenvío correcto -------------------------------------------------

    @Test
    void listarReenviaElFiltro() throws Exception {
        when(cliente.listar("dolex")).thenReturn(List.of(medicamento("MED-0001", 10)));

        perform(get("/api/medicamentos").param("q", "dolex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].idMedicamento").value("MED-0001"))
                .andExpect(jsonPath("$[0].fechaVencimiento").value("2027-03-31"));
    }

    @Test
    void registrarDevuelve201ConLocationDelServicioPrincipal() throws Exception {
        when(cliente.registrar(any(MedicamentoRequest.class))).thenReturn(medicamento("MED-0001", 10));

        perform(post("/api/medicamentos").contentType(MediaType.APPLICATION_JSON).content(ALTA))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/medicamentos/MED-0001"))
                .andExpect(jsonPath("$.cantidadStock").value(10));

        verify(cliente).registrar(new MedicamentoRequest("Dolex", "Paracetamol", "TABLETA", "500 mg",
                "GSK", "L-1", 10, 5, LocalDate.of(2027, 3, 31), "Estante A3"));
    }

    @Test
    void editarYEliminarSeReenvian() throws Exception {
        when(cliente.editar(eq("MED-0001"), any(ActualizarMedicamentoRequest.class)))
                .thenReturn(medicamento("MED-0001", 10));

        perform(put("/api/medicamentos/MED-0001").contentType(MediaType.APPLICATION_JSON).content(ALTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idMedicamento").value("MED-0001"));
        perform(delete("/api/medicamentos/MED-0001"))
                .andExpect(status().isNoContent());

        verify(cliente).eliminar("MED-0001");
    }

    @Test
    void entradaYSalidaSeReenvianConLaCantidad() throws Exception {
        when(cliente.registrarEntrada("MED-0001", 5)).thenReturn(medicamento("MED-0001", 15));
        when(cliente.registrarSalida("MED-0001", 12)).thenReturn(medicamento("MED-0001", 3));

        perform(post("/api/medicamentos/MED-0001/entradas")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadStock").value(15));
        perform(post("/api/medicamentos/MED-0001/salidas")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":12}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadStock").value(3))
                .andExpect(jsonPath("$.stockBajo").value(true));
    }

    @Test
    void movimientoConCantidadInvalidaDevuelve400SinLlamarAlServicio() throws Exception {
        perform(post("/api/medicamentos/MED-0001/salidas")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("La cantidad debe ser un entero entre 1 y 1000000."));

        verifyNoInteractions(cliente);
    }

    @Test
    void consultasSeReenvian() throws Exception {
        when(cliente.listarStockBajo()).thenReturn(List.of(medicamento("MED-0001", 2)));
        when(cliente.listarPorVencer(null)).thenReturn(List.of());
        when(cliente.listarPorVencer(90)).thenReturn(List.of(medicamento("MED-0002", 10)));
        when(cliente.listarVencidos()).thenReturn(List.of());

        perform(get("/api/medicamentos/stock-bajo"))
                .andExpect(jsonPath("$[*].idMedicamento", contains("MED-0001")));
        perform(get("/api/medicamentos/por-vencer"))
                .andExpect(jsonPath("$", hasSize(0)));
        perform(get("/api/medicamentos/por-vencer").param("dias", "90"))
                .andExpect(jsonPath("$[*].idMedicamento", contains("MED-0002")));
        perform(get("/api/medicamentos/vencidos"))
                .andExpect(status().isOk());
    }

    // --- Traducción de errores --------------------------------------------

    @Test
    void medicamentoInexistenteDevuelve404ConElMensajeDelServicio() throws Exception {
        when(cliente.obtener("MED-0009"))
                .thenThrow(new NotFoundException("No se encontró el medicamento MED-0009."));

        perform(get("/api/medicamentos/MED-0009"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensajes[0]").value("No se encontró el medicamento MED-0009."));
    }

    @Test
    void stockInsuficienteDevuelve400ConElMensajeDelServicio() throws Exception {
        when(cliente.registrarSalida("MED-0001", 11)).thenThrow(
                new SolicitudInvalidaException("Stock insuficiente: disponible 10, solicitado 11."));

        perform(post("/api/medicamentos/MED-0001/salidas")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":11}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("Stock insuficiente: disponible 10, solicitado 11."));
    }

    @Test
    void duplicadoDevuelve409() throws Exception {
        String mensaje = "Ya existe un medicamento con el mismo nombre, concentración, presentación y lote.";
        when(cliente.registrar(any(MedicamentoRequest.class))).thenThrow(new ConflictoException(mensaje));

        perform(post("/api/medicamentos").contentType(MediaType.APPLICATION_JSON).content(ALTA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.mensajes[0]").value(mensaje));
    }

    @Test
    void servicioCaidoDevuelve503ConErrorResponseYSinDetalles() throws Exception {
        when(cliente.listar(null)).thenThrow(new ServicioNoDisponibleException(
                "MedicamentosService no responde: Connection refused", new ConnectException()));

        perform(get("/api/medicamentos"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"))
                .andExpect(jsonPath("$.mensajes", contains(
                        "El servicio de medicamentos no está disponible. Vuelva a intentarlo más tarde.")));
    }

    @Test
    void respuestaInesperadaDevuelve502SinDetalles() throws Exception {
        doThrow(new RespuestaServicioInvalidaException("MedicamentosService respondió 500 a DELETE"))
                .when(cliente).eliminar("MED-0001");

        perform(delete("/api/medicamentos/MED-0001"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.mensajes", contains(
                        "El servicio de medicamentos respondió de forma inesperada.")));
    }
}
