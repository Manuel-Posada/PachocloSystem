package com.pachoclosystem.pachoclosystem.controller;

import com.pachoclosystem.pachoclosystem.model.Rol;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fija el manejo uniforme de errores de la API: siempre el mismo
 * {@code ErrorResponse}, sin trazas de pila, mensajes internos ni clases Java.
 */
class ErroresHttpTest extends MockMvcBaseTest {

    private static final String MENSAJE_CUERPO_INVALIDO =
            "El cuerpo de la petición es inválido o tiene valores no reconocidos.";

    @Test
    void rutaInexistenteDevuelve404ConErrorResponseYSinTrazas() throws Exception {
        perform(get("/api/noexiste"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]").value("No se encontró el recurso solicitado."))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(content().string(not(containsString("trace"))))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("com.pachoclosystem"))));
    }

    @Test
    void metodoNoSoportadoDevuelve405ConErrorResponseYSinTrazas() throws Exception {
        perform(delete("/api/pacientes"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("Method Not Allowed"))
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]")
                        .value("El método de la petición no está permitido en este recurso."))
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(header().string("Allow", containsString("POST")))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(content().string(not(containsString("trace"))))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("com.pachoclosystem"))));
    }

    @Test
    void cuerpoJsonMalformadoDevuelve400ConErrorResponseYSinTrazas() throws Exception {
        performComo(Rol.DOCTOR, post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana Torres","edad":"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]").value(MENSAJE_CUERPO_INVALIDO))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(content().string(not(containsString("trace"))))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("com.pachoclosystem"))));
    }

    @Test
    void tipoDeRegistroDesconocidoDevuelve400ConErrorResponse() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");

        postHistorial(paciente, """
                                {"tipo":"INVENTADO","idAutor":"%s","contenido":"Hipertension leve"}"""
                                .formatted(doctor))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]").value(MENSAJE_CUERPO_INVALIDO))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("com.pachoclosystem"))));
    }

    @Test
    void elHandlerGenericoNoPisaAlDeNoEncontrado() throws Exception {
        perform(get("/api/pacientes/{id}", "PAC-9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensajes[0]").value("No se encontró el paciente PAC-9999."));
    }

    @Test
    void elHandlerGenericoNoPisaALaValidacionDeDtos() throws Exception {
        performComo(Rol.DOCTOR, post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes", hasSize(3)))
                .andExpect(jsonPath("$.mensajes[0]").value("El nombre completo es obligatorio."));
    }

    @Test
    void elHandlerGenericoNoPisaALasSolicitudesInvalidasDeNegocio() throws Exception {
        perform(get("/api/historial").param("filtro", "inventado"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]")
                        .value("El filtro debe ser todos, paciente o autor."));
    }

    @Test
    void contentTypeNoSoportadoDevuelve415ConErrorResponseYSinTrazas() throws Exception {
        performComo(Rol.DOCTOR, post("/api/pacientes")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("nombre=Ana"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.error").value("Unsupported Media Type"))
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]")
                        .value("El tipo de contenido de la petición no está soportado por este recurso."))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(content().string(not(containsString("trace"))))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("com.pachoclosystem"))));
    }

    /**
     * Nota: cuando el cliente solo acepta un tipo sin conversor (p. ej.
     * {@code application/xml}, que no está en el classpath) no puede
     * serializarse <em>ningún</em> cuerpo, ni siquiera el {@code ErrorResponse};
     * por eso aquí solo se comprueba el estado 406 y que no aparezcan trazas.
     * La forma del {@code ErrorResponse} de este camino se cubre en el test
     * unitario {@code GlobalExceptionHandlerTest}.
     */
    @Test
    void acceptNoNegociableDevuelve406SinTrazas() throws Exception {
        perform(get("/api/pacientes").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable())
                .andExpect(content().string(""))
                .andExpect(content().string(not(containsString("trace"))))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("com.pachoclosystem"))));
    }
}
