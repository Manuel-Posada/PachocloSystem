package com.pachoclosystem.medicamentos.service;

import com.pachoclosystem.medicamentos.dto.MedicamentoResponse;
import com.pachoclosystem.medicamentos.model.Presentacion;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/** Caducidad y límite de entradas del almacén de claves de idempotencia. */
class AlmacenIdempotenciaTest {

    /** Reloj que avanza a mano. */
    private static final class RelojManual extends Clock {
        private Instant ahora = Instant.parse("2026-06-15T10:00:00Z");

        void avanzar(Duration tiempo) {
            ahora = ahora.plus(tiempo);
        }

        @Override
        public Instant instant() {
            return ahora;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }
    }

    private static final MedicamentoResponse RESPUESTA = new MedicamentoResponse("MED-0001", "Dolex",
            "Paracetamol", Presentacion.TABLETA, "500 mg", "Genfar", "L-1", 7, 2,
            LocalDate.of(2027, 1, 31), "Estante A3", false, false);

    private final RelojManual reloj = new RelojManual();

    @Test
    void guardaYDevuelveLaSalida() {
        AlmacenIdempotencia almacen = new AlmacenIdempotencia(reloj, Duration.ofHours(24), 10);

        almacen.guardar("clave-uno-0000000", "MED-0001", 3, RESPUESTA);

        assertThat(almacen.buscar("clave-uno-0000000")).hasValueSatisfying(salida -> {
            assertThat(salida.idMedicamento()).isEqualTo("MED-0001");
            assertThat(salida.cantidad()).isEqualTo(3);
            assertThat(salida.respuesta()).isEqualTo(RESPUESTA);
        });
        assertThat(almacen.buscar("otra-clave-000000")).isEmpty();
    }

    @Test
    void unaClaveCaducaALas24Horas() {
        AlmacenIdempotencia almacen = new AlmacenIdempotencia(reloj, Duration.ofHours(24), 10);
        almacen.guardar("clave-uno-0000000", "MED-0001", 3, RESPUESTA);

        reloj.avanzar(Duration.ofHours(24).minusSeconds(1));
        assertThat(almacen.buscar("clave-uno-0000000")).isPresent();

        reloj.avanzar(Duration.ofSeconds(1));
        assertThat(almacen.buscar("clave-uno-0000000")).isEmpty();
        assertThat(almacen.tamano()).isZero();
    }

    @Test
    void alPasarseDelLimiteDescartaLasMasAntiguas() {
        AlmacenIdempotencia almacen = new AlmacenIdempotencia(reloj, Duration.ofHours(24), 3);

        for (int i = 1; i <= 5; i++) {
            almacen.guardar("clave-" + i + "-0000000000", "MED-0001", i, RESPUESTA);
            reloj.avanzar(Duration.ofMinutes(1));
        }

        assertThat(almacen.tamano()).isEqualTo(3);
        assertThat(almacen.buscar("clave-1-0000000000")).isEmpty();
        assertThat(almacen.buscar("clave-2-0000000000")).isEmpty();
        assertThat(almacen.buscar("clave-3-0000000000")).isPresent();
        assertThat(almacen.buscar("clave-5-0000000000")).isPresent();
    }

    @Test
    void alGuardarSePurganLasCaducadasAunqueNoSeConsulten() {
        AlmacenIdempotencia almacen = new AlmacenIdempotencia(reloj, Duration.ofHours(1), 100);
        almacen.guardar("vieja-1-0000000000", "MED-0001", 1, RESPUESTA);
        almacen.guardar("vieja-2-0000000000", "MED-0001", 1, RESPUESTA);

        reloj.avanzar(Duration.ofHours(2));
        almacen.guardar("nueva-0000000000000", "MED-0001", 1, RESPUESTA);

        assertThat(almacen.tamano()).isEqualTo(1);
    }

    @Test
    void laMismaClaveUsaSiempreElMismoCerrojo() {
        AlmacenIdempotencia almacen = new AlmacenIdempotencia(reloj, Duration.ofHours(24), 10);

        assertThat(almacen.cerrojo("clave-uno-0000000")).isSameAs(almacen.cerrojo("clave-uno-0000000"));
    }

    @Test
    void rechazaUnaConfiguracionSinSentido() {
        assertThatIllegalStateException().isThrownBy(() -> new AlmacenIdempotencia(reloj, Duration.ZERO, 10));
        assertThatIllegalStateException().isThrownBy(() -> new AlmacenIdempotencia(reloj, Duration.ofHours(1), 0));
    }
}
