package com.pachoclosystem.pachoclosystem.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Configuración del secreto JWT: un JWT_SECRET corto impide el arranque con un
 * mensaje claro y sin filtrar el valor; sin JWT_SECRET se genera una clave
 * aleatoria de 32 bytes y se emite un único WARN que no contiene el secreto.
 */
class JwtArranqueTest {

    @Test
    void unJwtSecretCortoFallaConMensajeClaroYSinFiltrarElValor() {
        MockEnvironment entorno = new MockEnvironment()
                .withProperty(ClaveFirmaJwt.PROP_SECRET, "demasiadoCorto");

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new ClaveFirmaJwt(entorno))
                .withMessageContaining(ClaveFirmaJwt.PROP_SECRET)
                .withMessageContaining("32 bytes")
                .satisfies(excepcion -> assertThat(excepcion.getMessage())
                        .doesNotContain("demasiadoCorto"));
    }

    @Test
    void unJwtSecretCortoImpideElArranqueDelContexto() {
        new ApplicationContextRunner()
                .withUserConfiguration(ConfiguracionFirma.class)
                .withPropertyValues(ClaveFirmaJwt.PROP_SECRET + "=demasiadoCorto")
                .run(contexto -> {
                    Throwable fallo = contexto.getStartupFailure();
                    Throwable ultima = fallo;
                    while (ultima.getCause() != null && ultima.getCause() != ultima) {
                        ultima = ultima.getCause();
                    }
                    assertThat(ultima)
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining(ClaveFirmaJwt.PROP_SECRET)
                            .hasMessageContaining("32 bytes");
                    assertThat(ultima.getMessage()).doesNotContain("demasiadoCorto");
                });
    }

    @Test
    void sinJwtSecretSeGeneraUnaClaveAleatoriaDe32Bytes() {
        ClaveFirmaJwt firma = new ClaveFirmaJwt(new MockEnvironment());

        assertThat(firma.clave().getEncoded()).hasSize(32);
        assertThat(firma.clave().getAlgorithm()).isEqualTo("HmacSHA256");
        assertThat(firma.emisor()).isEqualTo("pachoclosystem");
        assertThat(firma.expiracion().toMinutes()).isEqualTo(30);
    }

    @Test
    void sinJwtSecretSeEmiteUnUnicoWarnQueNoContieneElSecreto() {
        Logger logger = (Logger) LoggerFactory.getLogger(ClaveFirmaJwt.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            new ClaveFirmaJwt(new MockEnvironment());

            List<ILoggingEvent> warns = appender.list.stream()
                    .filter(evento -> evento.getLevel() == Level.WARN)
                    .toList();
            assertThat(warns).hasSize(1);
            String mensaje = warns.get(0).getFormattedMessage();
            assertThat(mensaje).contains(ClaveFirmaJwt.PROP_SECRET);
        } finally {
            logger.detachAppender(appender);
        }
    }

    @TestConfiguration
    static class ConfiguracionFirma {
        @Bean
        ClaveFirmaJwt claveFirmaJwt(Environment entorno) {
            return new ClaveFirmaJwt(entorno);
        }
    }
}