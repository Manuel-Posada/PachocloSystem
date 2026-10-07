package com.pachoclosystem.pachoclosystem.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validación de {@code app.cors.origenes}: el valor por defecto es una lista
 * vacía (sin CORS), las entradas se recortan e ignoran las vacías, y un origen
 * que no sea una URL absoluta http(s) sin barra final —o un comodín— aborta el
 * arranque con un mensaje claro.
 */
class CorsConfiguracionTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(CorsConfiguracion.class);

    @Test
    void sinConfiguracionNoSePermiteNingunOrigen() {
        assertThat(CorsConfiguracion.normalizarOrigenes(null)).isEmpty();
        assertThat(CorsConfiguracion.normalizarOrigenes("")).isEmpty();
        assertThat(CorsConfiguracion.normalizarOrigenes("   ")).isEmpty();
    }

    @Test
    void recortaEspaciosEIgnoraEntradasVacias() {
        List<String> origenes = CorsConfiguracion.normalizarOrigenes(
                "  http://localhost:3000 , , https://app.example.com  ");
        assertThat(origenes)
                .containsExactly("http://localhost:3000", "https://app.example.com");
    }

    @Test
    void unComodinAbortaElArranqueConMensajeClaro() {
        assertThatThrownBy(() -> CorsConfiguracion.normalizarOrigenes("*"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no admite comodines");

        contexto.withPropertyValues("app.cors.origenes=*")
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(ctx.getStartupFailure()).hasStackTraceContaining("no admite comodines");
                });
    }

    @Test
    void unaUrlInvalidaAbortaElArranqueConMensajeClaro() {
        for (String invalida : List.of("no-es-una-url", "localhost:3000", "ftp://localhost:3000",
                "http://localhost:3000/", "https://localhost:3000/api/")) {
            assertThatThrownBy(() -> CorsConfiguracion.normalizarOrigenes(invalida))
                    .as("origen inválido: %s", invalida)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no es válido");
        }

        contexto.withPropertyValues("app.cors.origenes=http://localhost:3000/")
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(ctx.getStartupFailure()).hasStackTraceContaining("no es válido");
                });
    }
}