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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

        assertThat(mensajesLogueados()).allSatisfy(mensaje -> {
            assertThat(mensaje)
                    .doesNotContain(PASSWORD_ADMIN)
                    .doesNotContain(SECRETO_JWT)
                    .doesNotContain(token)
                    .doesNotContain("$2a$10$");
        });
    }

    @Test
    void laGestionDeUsuariosNoLogueaContrasenasNiHashes() throws Exception {
        String inicial = "inicial-que-no-debe-salir-1";
        String nueva = "nueva-que-no-debe-salir-22";
        String username = "logs." + UUID.randomUUID().toString().substring(0, 8);

        MvcResult alta = perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s","rol":"ADMIN"}""".formatted(username, inicial)))
                .andExpect(status().isCreated())
                .andReturn();
        String id = leer(alta, "$.idUsuario");

        // Cuerpo malformado que contiene una contraseña (lo registra GlobalExceptionHandler).
        perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"otro.x\",\"password\":\"" + nueva + "\", rol}"))
                .andExpect(status().isBadRequest());
        // Contraseña que no cumple la política, y restablecimiento correcto.
        perform(patch("/api/usuarios/{id}/password", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"corta-123\"}"))
                .andExpect(status().isBadRequest());
        perform(patch("/api/usuarios/{id}/password", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"" + nueva + "\"}"))
                .andExpect(status().isNoContent());

        // El cuerpo malformado sí se registra: el test comprueba que sin la contraseña.
        assertThat(mensajesLogueados()).anyMatch(m -> m.contains("Cuerpo de petición no legible"));
        assertThat(mensajesLogueados()).allSatisfy(mensaje -> assertThat(mensaje)
                .doesNotContain(inicial)
                .doesNotContain(nueva)
                .doesNotContain("corta-123")
                .doesNotContain("$2a$10$"));
    }

    @Test
    void laGestionDeUsuariosNoLogueaPasswordTemporalNuevaNiHash() throws Exception {
        String temporal = "temporal-log-1234567890";
        String nueva = "nueva-log-clave-1234567";
        String username = "log.oculto" + System.nanoTime();

        // Alta por API con contraseña temporal.
        MvcResult alta = mockMvc.perform(post("/api/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + temporal
                                + "\",\"rol\":\"ADMIN\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        // Login, cambio propio de contraseña y reset administrativo.
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + temporal + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String token = leer(login, "$.token");

        mockMvc.perform(post("/api/auth/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passwordActual\":\"" + temporal + "\",\"passwordNueva\":\"" + nueva + "\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch("/api/usuarios/{id}/password",
                        leer(alta, "$.idUsuario"))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"reset-log-clave-12345\"}"))
                .andExpect(status().isNoContent());

        assertThat(mensajesLogueados()).allSatisfy(mensaje -> assertThat(mensaje)
                .doesNotContain(temporal)
                .doesNotContain(nueva)
                .doesNotContain("reset-log-clave-12345")
                .doesNotContain("$2a$10$")
                .doesNotContain(SECRETO_JWT)
                .doesNotContain(token));
    }

    private List<String> mensajesLogueados() {
        return appenders.stream()
                .flatMap(appender -> appender.list.stream())
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    private List<Logger> loggersCapturados() {
        return List.of(
                (Logger) LoggerFactory.getLogger("com.pachoclosystem.pachoclosystem"),
                (Logger) LoggerFactory.getLogger("com.pachoclosystem.pachoclosystem.security"),
                (Logger) LoggerFactory.getLogger("com.pachoclosystem.pachoclosystem.service"),
                (Logger) LoggerFactory.getLogger("com.pachoclosystem.pachoclosystem.config.ClaveFirmaJwt"),
                (Logger) LoggerFactory.getLogger("com.pachoclosystem.pachoclosystem.config.AdminInicial"),
                (Logger) LoggerFactory.getLogger("com.pachoclosystem.pachoclosystem.controller.AuthController"),
                (Logger) LoggerFactory.getLogger("com.pachoclosystem.pachoclosystem.controller.UsuarioController"),
                (Logger) LoggerFactory.getLogger("com.pachoclosystem.pachoclosystem.service.UsuarioService"),
                (Logger) LoggerFactory.getLogger("com.pachoclosystem.pachoclosystem.exception.GlobalExceptionHandler"));
    }
}