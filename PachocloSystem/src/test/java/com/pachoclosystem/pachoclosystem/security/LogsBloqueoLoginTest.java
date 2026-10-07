package com.pachoclosystem.pachoclosystem.security;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.pachoclosystem.pachoclosystem.controller.MockMvcBaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Durante el bloqueo temporal del login el log sale saneado: el username se
 * registra en minúsculas, sin caracteres de control y truncado a 30, y nunca
 * aparecen ni la contraseña, ni el hash, ni tokens.
 */
class LogsBloqueoLoginTest extends MockMvcBaseTest {

    private static final String PASSWORD_INVENTADA = "clave-secreta-no-esta-en-el-log-123456";
    /** Salto de línea (U+000A) construido sin escapes Unicode ambiguos. */
    private static final char SALTO_DE_LINEA = (char) 0x000A;
    private static final String USERNAME_CRUDO = "admin" + SALTO_DE_LINEA + "INYECTA:2026-nuevalinea";
    private static final String USERNAME_SANEADO_ESPERADO = "admininyecta:2026-nuevalinea";

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
    void elBloqueoLogeaElUsernameSaneadoYNuncaLaPasswordNiTokens() throws Exception {
        // El username lleva un salto de línea en medio. En el JSON viaja como
        // escape Unicode (texto literal formado por barra, u, 0, 0, 0, A) y
        // Jackson lo decodifica como el carácter de control real; el servicio lo
        // normaliza para las claves y lo sanea (sin controles, minúsculas, 30
        // chars) para el log.
        String contenido = "{\"username\":\"admin" + secuenciaEscapeUnicode(0x000A)
                + "inyecta:2026-nuevalinea\",\"password\":\"" + PASSWORD_INVENTADA + "\"}";

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(contenido))
                    .andExpect(status().isUnauthorized());
        }
        // 6º intento: bloqueado (429). Aquí se emite el WARN con el username saneado.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contenido))
                .andExpect(status().isTooManyRequests());

        List<String> mensajes = appenders.stream()
                .flatMap(appender -> appender.list.stream())
                .map(ILoggingEvent::getFormattedMessage)
                .toList();

        // El WARN de bloqueo existe y muestra el username saneado (minúsculas y
        // sin el carácter de control).
        assertThat(mensajes).anySatisfy(mensaje -> {
            assertThat(mensaje)
                    .contains("Login bloqueado temporalmente")
                    .contains(USERNAME_SANEADO_ESPERADO);
        });

        // Ninguna línea del log lleva la contraseña, el hash, un token ni el
        // username crudo con su carácter de control.
        assertThat(mensajes).allSatisfy(mensaje -> {
            assertThat(mensaje)
                    .doesNotContain(PASSWORD_INVENTADA)
                    .doesNotContain("$2a$10$")
                    .doesNotContain("eyJ")
                    .doesNotContain(USERNAME_CRUDO);
        });
    }

    /** Devuelve el texto literal de un escape Unicode (barra, u, 0, 0, 0, A). */
    private static String secuenciaEscapeUnicode(int punto) {
        return String.valueOf('\\') + "u" + String.format("%04X", punto);
    }

    private List<Logger> loggersCapturados() {
        return List.of(
                (Logger) LoggerFactory.getLogger("com.pachoclosystem.pachoclosystem"),
                (Logger) LoggerFactory.getLogger("com.pachoclosystem.pachoclosystem.security"),
                (Logger) LoggerFactory.getLogger("com.pachoclosystem.pachoclosystem.service"),
                (Logger) LoggerFactory.getLogger("com.pachoclosystem.pachoclosystem.controller.AuthController"),
                (Logger) LoggerFactory.getLogger("com.pachoclosystem.pachoclosystem.exception.GlobalExceptionHandler"));
    }
}