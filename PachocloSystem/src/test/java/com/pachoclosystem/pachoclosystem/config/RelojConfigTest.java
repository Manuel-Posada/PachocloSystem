package com.pachoclosystem.pachoclosystem.config;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/** Zona horaria oficial del reloj de la aplicación. */
class RelojConfigTest {

    private final RelojConfig configuracion = new RelojConfig();

    @Test
    void elRelojUsaLaZonaConfigurada() {
        assertThat(configuracion.reloj("America/Bogota").getZone()).isEqualTo(ZoneId.of("America/Bogota"));
        assertThat(configuracion.reloj(" Europe/Madrid ").getZone()).isEqualTo(ZoneId.of("Europe/Madrid"));
    }

    @Test
    void unaZonaInvalidaImpideArrancarConUnMensajeClaro() {
        assertThatIllegalStateException()
                .isThrownBy(() -> configuracion.reloj("Bogota/Colombia"))
                .withMessageContaining("app.zona-horaria")
                .withMessageContaining("APP_ZONA_HORARIA");
        assertThatIllegalStateException().isThrownBy(() -> configuracion.reloj(""));
    }
}
