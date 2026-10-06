package com.pachoclosystem.pachoclosystem.security;

import com.pachoclosystem.pachoclosystem.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El manejador de acceso denegado escribe el 403 con el ErrorResponse uniforme
 * de la API, sin cabecera WWW-Authenticate (esa es del 401) y sin trazas.
 */
class AccesoDenegadoHandlerTest {

    @Test
    void elHandler403EscribeElCuerpoUniformeEnJson() throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest("GET", "/api/pacientes");
        MockHttpServletResponse respuesta = new MockHttpServletResponse();

        new AccesoDenegadoHandler().handle(peticion, respuesta,
                new AccessDeniedException("operación denegada"));

        assertThat(respuesta.getStatus()).isEqualTo(403);
        assertThat(respuesta.getContentType()).startsWith("application/json");
        assertThat(respuesta.getHeader("WWW-Authenticate")).isNull();

        ErrorResponse error = new ObjectMapper().readValue(
                respuesta.getContentAsString(), ErrorResponse.class);
        assertThat(error.status()).isEqualTo(403);
        assertThat(error.error()).isEqualTo("Forbidden");
        assertThat(error.mensajes())
                .containsExactly("No tiene permisos para realizar esta operación.");
    }
}