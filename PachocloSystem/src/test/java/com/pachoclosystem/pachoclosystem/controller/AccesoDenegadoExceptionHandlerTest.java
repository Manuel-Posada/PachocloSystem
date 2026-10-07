package com.pachoclosystem.pachoclosystem.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Una {@link AccessDeniedException} (o su subclase {@link AuthorizationDeniedException})
 * lanzada desde un servicio/controlador la responde el manejador global con el
 * 403 uniforme, nunca la red genérica de 500.
 */
class AccesoDenegadoExceptionHandlerTest extends MockMvcBaseTest {

    private static final String MENSAJE_403 = "No tiene permisos para realizar esta operación.";

    @Test
    void accessDeniedLanzadaPorElServicioDevuelve403Nunca500() throws Exception {
        perform(get("/test/denegado"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]").value(MENSAJE_403))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(content().string(not(containsString("detalle interno secreto"))))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("com.pachoclosystem"))));
    }

    @Test
    void authorizationDeniedLanzadaPorElServicioDevuelve403Uniforme() throws Exception {
        perform(get("/test/autorizacion-denegada"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]").value(MENSAJE_403))
                .andExpect(content().string(not(containsString("com.pachoclosystem"))));
    }

    @TestConfiguration
    static class ConfiguracionDePrueba {

        @Bean
        ControladorQueNiega controladorQueNiega() {
            return new ControladorQueNiega();
        }
    }

    /** Controlador de prueba (solo en este contexto) cuyo servicio deniega el acceso. */
    @RestController
    static class ControladorQueNiega {

        private final ServicioQueNiega servicio = new ServicioQueNiega();

        @GetMapping("/test/denegado")
        String denegado() {
            return servicio.operacionDenegada();
        }

        @GetMapping("/test/autorizacion-denegada")
        String autorizacionDenegada() {
            return servicio.operacionAutorizacionDenegada();
        }
    }

    static class ServicioQueNiega {

        String operacionDenegada() {
            throw new AccessDeniedException("detalle interno secreto");
        }

        String operacionAutorizacionDenegada() {
            throw new AuthorizationDeniedException("detalle interno secreto");
        }
    }
}