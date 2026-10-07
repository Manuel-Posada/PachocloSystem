package com.pachoclosystem.medicamentos.config;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/** Zona horaria oficial del reloj de la aplicación. */
class ClockConfigTest {

    private final ClockConfig configuracion = new ClockConfig();

    @Test
    void elRelojUsaLaZonaConfigurada() {
        assertThat(configuracion.clock("America/Bogota").getZone()).isEqualTo(ZoneId.of("America/Bogota"));
        assertThat(configuracion.clock(" Europe/Madrid ").getZone()).isEqualTo(ZoneId.of("Europe/Madrid"));
    }

    @Test
    void unaZonaInvalidaImpideArrancarConUnMensajeClaro() {
        assertThatIllegalStateException()
                .isThrownBy(() -> configuracion.clock("Bogota/Colombia"))
                .withMessageContaining("app.zona-horaria")
                .withMessageContaining("APP_ZONA_HORARIA");
        assertThatIllegalStateException().isThrownBy(() -> configuracion.clock(""));
    }
}
