package com.pachoclosystem.medicamentos.controller;

import com.jayway.jsonpath.JsonPath;
import com.pachoclosystem.medicamentos.MedicamentosApplication;
import com.pachoclosystem.medicamentos.model.Medicamento;
import com.pachoclosystem.medicamentos.repository.IMedicamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base común de los tests MockMvc: arranca la aplicación completa con la fecha
 * fijada en {@link #HOY} y vacía el repositorio en memoria antes de cada test, de
 * modo que cada prueba es independiente del orden de ejecución.
 */
@SpringBootTest(classes = {MedicamentosApplication.class, MockMvcBaseTest.RelojFijo.class})
@AutoConfigureMockMvc
abstract class MockMvcBaseTest {

    protected static final LocalDate HOY = LocalDate.of(2026, 6, 15);

    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojFijo() {
            return Clock.fixed(HOY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        }
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected IMedicamentoRepository repositorio;

    @BeforeEach
    void limpiarRepositorio() {
        repositorio.listarTodos().stream()
                .map(Medicamento::getIdMedicamento)
                .forEach(repositorio::eliminar);
    }

    /** Lee un valor del cuerpo de la respuesta (siempre en UTF-8). */
    protected <T> T leer(MvcResult resultado, String expresion) throws Exception {
        return JsonPath.read(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8), expresion);
    }

    /** Cuerpo JSON válido de alta, variando lo que suelen cambiar las pruebas. */
    protected static String cuerpoAlta(String nombre, String lote, int stock, int stockMinimo, LocalDate vencimiento) {
        return """
                {"nombre":"%s","principioActivo":"Paracetamol","presentacion":"TABLETA",
                 "concentracion":"500 mg","laboratorio":"Genfar","lote":"%s",
                 "cantidadStock":%d,"stockMinimo":%d,"fechaVencimiento":"%s",
                 "ubicacion":"Farmacia - Estante A3"}
                """.formatted(nombre, lote, stock, stockMinimo, vencimiento);
    }

    /** Registra un medicamento válido y devuelve su ID (p. ej. MED-0001). */
    protected String registrar(String nombre, String lote, int stock, int stockMinimo, LocalDate vencimiento)
            throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/medicamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoAlta(nombre, lote, stock, stockMinimo, vencimiento)))
                .andExpect(status().isCreated())
                .andReturn();
        return leer(resultado, "$.idMedicamento");
    }
}
