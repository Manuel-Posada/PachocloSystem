package com.pachoclosystem.pachoclosystem.controller;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.pachoclosystem.pachoclosystem.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cubre la red de seguridad genérica ({@code Exception -> 500}): la respuesta es
 * un {@code ErrorResponse} con mensaje genérico y sin detalles internos; el
 * detalle real solo aparece en el log del servidor.
 */
class ErrorInternoTest extends MockMvcBaseTest {

    private ListAppender<ILoggingEvent> registroDeLog;

    @BeforeEach
    void capturarLogDelManejador() {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        registroDeLog = new ListAppender<>();
        registroDeLog.start();
        logger.addAppender(registroDeLog);
    }

    @AfterEach
    void soltarLogDelManejador() {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        logger.detachAppender(registroDeLog);
    }

    @Test
    void errorInesperadoDevuelve500GenericoSinDetallesEnLaRespuesta() throws Exception {
        perform(get("/test/exploto"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]")
                        .value("Se produjo un error interno. Vuelva a intentarlo más tarde."))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(content().string(not(containsString("trace"))))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("com.pachoclosystem"))))
                .andExpect(content().string(not(containsString("detalle interno secreto"))));

        assertThat(registroDeLog.list).isNotEmpty();
        boolean detalleEnElLog = registroDeLog.list.stream()
                .map(ILoggingEvent::getThrowableProxy)
                .filter(proxy -> proxy != null)
                .anyMatch(proxy -> proxy.getMessage() != null
                        && proxy.getMessage().contains("detalle interno secreto"));
        assertThat(detalleEnElLog)
                .as("el detalle real de la excepción solo debe aparecer en el log del servidor")
                .isTrue();
    }

    @TestConfiguration
    static class ConfiguracionDePrueba {

        @Bean
        ControladorConFallo controladorConFallo() {
            return new ControladorConFallo();
        }
    }

    /** Controlador de prueba (solo en este contexto) que siempre falla. */
    @RestController
    static class ControladorConFallo {

        @GetMapping("/test/exploto")
        String explotar() {
            throw new IllegalStateException("detalle interno secreto");
        }
    }
}
