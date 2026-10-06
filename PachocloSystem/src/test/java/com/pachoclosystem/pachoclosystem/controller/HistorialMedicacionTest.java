package com.pachoclosystem.pachoclosystem.controller;

import com.pachoclosystem.pachoclosystem.client.MedicamentosClient;
import com.pachoclosystem.pachoclosystem.dto.MedicamentoResponse;
import com.pachoclosystem.pachoclosystem.exception.NotFoundException;
import com.pachoclosystem.pachoclosystem.exception.ServicioNoDisponibleException;
import com.pachoclosystem.pachoclosystem.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

import java.net.ConnectException;
import java.time.LocalDate;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Registros de MEDICACION que descuentan stock en MedicamentosService (cliente
 * simulado): la salida se hace antes de guardar, y si falla no se crea el registro.
 */
class HistorialMedicacionTest extends MockMvcBaseTest {

    @MockitoBean
    private MedicamentosClient cliente;

    private ResultActions registrar(String paciente, String cuerpo) throws Exception {
        return perform(post("/api/pacientes/{id}/historial", paciente)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    private static String medicacion(String autor, String idMedicamento, Integer cantidad) {
        String extra = (idMedicamento == null ? "" : ",\"idMedicamento\":\"" + idMedicamento + "\"")
                + (cantidad == null ? "" : ",\"cantidad\":" + cantidad);
        return "{\"tipo\":\"MEDICACION\",\"idAutor\":\"" + autor
                + "\",\"contenido\":\"Paracetamol 500 mg via oral\"" + extra + "}";
    }

    private void sinRegistros(String paciente) throws Exception {
        perform(get("/api/pacientes/{id}/historial", paciente))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void medicacionConMedicamentoDescuentaStockYDevuelve201ConMedicacion() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        when(cliente.registrarSalida("MED-0001", 2)).thenReturn(new MedicamentoResponse("MED-0001",
                "Dolex", "Paracetamol", "TABLETA", "500 mg", "GSK", "L-1", 8, 5,
                LocalDate.of(2027, 3, 31), "Estante A3", false, false));

        registrar(paciente, medicacion(doctor, "MED-0001", 2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("MEDICACION"))
                .andExpect(jsonPath("$.contenido").value("Paracetamol 500 mg via oral"))
                .andExpect(jsonPath("$.medicacion.idMedicamento").value("MED-0001"))
                .andExpect(jsonPath("$.medicacion.cantidad").value(2));

        verify(cliente).registrarSalida("MED-0001", 2);
        perform(get("/api/pacientes/{id}/historial", paciente))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].medicacion.cantidad").value(2));
    }

    @Test
    void medicacionSinMedicamentoNoLlamaAlServicioNiIncluyeMedicacion() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");

        registrar(paciente, medicacion(doctor, null, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.medicacion").doesNotExist());

        verifyNoInteractions(cliente);
    }

    @Test
    void stockInsuficienteDevuelve400YNoCreaElRegistro() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        when(cliente.registrarSalida("MED-0001", 50)).thenThrow(
                new SolicitudInvalidaException("Stock insuficiente: disponible 10, solicitado 50."));

        registrar(paciente, medicacion(doctor, "MED-0001", 50))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes", contains("Stock insuficiente: disponible 10, solicitado 50.")));

        sinRegistros(paciente);
    }

    @Test
    void medicamentoInexistenteDevuelve404YNoCreaElRegistro() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        when(cliente.registrarSalida("MED-0009", 1))
                .thenThrow(new NotFoundException("No se encontró el medicamento MED-0009."));

        registrar(paciente, medicacion(doctor, "MED-0009", 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensajes[0]").value("No se encontró el medicamento MED-0009."));

        sinRegistros(paciente);
    }

    @Test
    void servicioCaidoDevuelve503YNoCreaElRegistro() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        when(cliente.registrarSalida("MED-0001", 1)).thenThrow(
                new ServicioNoDisponibleException("MedicamentosService no responde", new ConnectException()));

        registrar(paciente, medicacion(doctor, "MED-0001", 1))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));

        sinRegistros(paciente);
    }

    @Test
    void pacienteInexistenteNoDescuentaStock() throws Exception {
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");

        registrar("PAC-9999", medicacion(doctor, "MED-0001", 1))
                .andExpect(status().isNotFound());

        verify(cliente, never()).registrarSalida(anyString(), anyInt());
    }

    @Test
    void medicamentoSinCantidadDevuelve400() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");

        registrar(paciente, medicacion(doctor, "MED-0001", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value(
                        "Para descontar stock hay que indicar el ID del medicamento y la cantidad."));

        verifyNoInteractions(cliente);
    }

    @Test
    void medicamentoEnOtroTipoDeRegistroDevuelve400() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");

        registrar(paciente, """
                {"tipo":"DIAGNOSTICO","idAutor":"%s","contenido":"Hipertension leve",
                 "idMedicamento":"MED-0001","cantidad":1}""".formatted(doctor))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value(
                        "Solo los registros de MEDICACION pueden descontar stock de un medicamento."));

        verifyNoInteractions(cliente);
    }

    @Test
    void cantidadFueraDeRangoDevuelve400() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");

        registrar(paciente, medicacion(doctor, "MED-0001", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value(
                        "La cantidad administrada debe ser un entero entre 1 y 1000000."));

        verifyNoInteractions(cliente);
    }
}
