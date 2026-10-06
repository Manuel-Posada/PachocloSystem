package com.pachoclosystem.pachoclosystem.security;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.pachoclosystem.pachoclosystem.controller.MockMvcBaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ningún flujo de autenticación (login correcto o fallido, petición autenticada
 * o sin token) escribe en el log una contraseña, un hash BCrypt, el secreto JWT
 * ni el token emitido.
 */
class LogsSinSecretosTest extends MockMvcBaseTest {

    private static final String PASSWORD_ADMIN = "pwd-de-prueba-solo-tests-12345";
    private static final String SECRETO_JWT = "clave-jwt-de-prueba-para-tests-1234567890abcd";

    private final List<ListAppender<ILoggingEvent>> appenders = new ArrayList<>();

    @BeforeEach
    void fijarAppenders() {
        for (Logger logger : loggersCapturados()) {
            ListAppender<ILoggingEvent> appender = new ListAppender<>();
            appender.start();
            logger.addAppender(appender);
            appenders.add(appender);
        }
    }

    @AfterEach
    void quitarAppenders() {
        List<Logger> loggers = loggersCapturados();
        for (int i = 0; i < loggers.size(); i++) {
            loggers.get(i).detachAppender(appenders.get(i));
        }
        appenders.clear();
    }

    @Test
    void niElLoginNiLasPeticionesLogueanPasswordHashSecretoNiToken() throws Exception {
        // Login correcto.
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"%s"}""".formatted(PASSWORD_ADMIN)))
                .andExpect(status().isOk())
                .andReturn();
        String token = leer(login, "$.token");

        // Petición autenticada.
        mockMvc.perform(get("/api/pacientes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        // Login fallido y acceso sin token (401 por el punto de entrada).
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"incorrecta"}"""))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/pacientes"))
                .andExpect(status().isUnauthorized());

        List<String> mensajes = appenders.stream()
                .flatMap(appender -> appender.list.stream())
                .map(ILoggingEvent::getFormattedMessage)
                .toList();

        assertThat(mensajes).allSatisfy(mensaje -> {
            assertThat(mensaje)
                    .doesNotContain(PASSWORD_ADMIN)
                    .doesNotContain(SECRETO_JWT)
                    .doesNotContain(token)
                    .doesNotContain("$2a$10$");
        });
    }

    private List<Logger> loggersCapturados() {
        return List.of(
                (Logger) LoggerFactory.getLogger("com.pachoclosystem.pachoclosystem.security"),
                (Logger) LoggerFactory.getLogger("com.pachoclosystem.pachoclosystem.config.ClaveFirmaJwt"),
                (Logger) LoggerFactory.getLogger("com.pachoclosystem.pachoclosystem.controller.AuthController"),
                (Logger) LoggerFactory.getLogger("com.pachoclosystem.pachoclosystem.exception.GlobalExceptionHandler"));
    }
}