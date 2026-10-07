package com.pachoclosystem.medicamentos.repository;

import com.pachoclosystem.medicamentos.PostgresTestBase;
import com.pachoclosystem.medicamentos.dto.MedicamentoResponse;
import com.pachoclosystem.medicamentos.model.Presentacion;
import com.pachoclosystem.medicamentos.repository.IdempotenciaSalidasRepository.SalidaRegistrada;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/** Claves de idempotencia en PostgreSQL: respuesta guardada, caducidad, purga y bloqueo. */
class IdempotenciaSalidasRepositoryJdbcTest extends PostgresTestBase {

    private static final Duration CADUCIDAD = Duration.ofHours(24);
    private static final String CLAVE = "8f14e45f-ceea-4672-a5b1-7a0c3c9e2f01";

    @Autowired
    private IdempotenciaSalidasRepository claves;

    @Autowired
    private TransactionTemplate transaccion;

    private static MedicamentoResponse respuesta(int stock) {
        return new MedicamentoResponse("MED-0001", "Dolex", "Paracetamol", Presentacion.TABLETA, "500 mg",
                "Genfar", "L-1", stock, 5, LocalDate.of(2027, 1, 31), "Estante A3", stock <= 5, false);
    }

    private static Instant limite(Instant ahora) {
        return ahora.minus(CADUCIDAD);
    }

    @Test
    void guardaYDevuelveLaSalidaConLaMismaRespuesta() {
        Instant ahora = reloj.instant();
        claves.guardar(CLAVE, "MED-0001", 3, respuesta(17), ahora, limite(ahora));

        Optional<SalidaRegistrada> guardada = claves.buscarVigente(CLAVE, limite(ahora));

        assertThat(guardada).isPresent();
        assertThat(guardada.get().idMedicamento()).isEqualTo("MED-0001");
        assertThat(guardada.get().cantidad()).isEqualTo(3);
        assertThat(guardada.get().respuesta()).isEqualTo(respuesta(17));
        assertThat(guardada.get().hechaEn()).isEqualTo(ahora);
        assertThat(claves.buscarVigente("otra-clave-que-no-existe", limite(ahora))).isEmpty();
    }

    @Test
    void unaClaveCaducaALas24Horas() {
        Instant hecha = reloj.instant();
        claves.guardar(CLAVE, "MED-0001", 3, respuesta(17), hecha, limite(hecha));

        Instant casi = hecha.plus(CADUCIDAD).minusMillis(1);
        assertThat(claves.buscarVigente(CLAVE, limite(casi))).isPresent();

        Instant justo = hecha.plus(CADUCIDAD);
        assertThat(claves.buscarVigente(CLAVE, limite(justo))).isEmpty();
    }

    @Test
    void unaClaveCaducadaSeSustituyeAlVolverAUsarla() {
        Instant hecha = reloj.instant();
        claves.guardar(CLAVE, "MED-0001", 3, respuesta(17), hecha, limite(hecha));

        Instant despues = hecha.plus(CADUCIDAD).plusSeconds(1);
        claves.guardar(CLAVE, "MED-0002", 1, respuesta(9), despues, limite(despues));

        SalidaRegistrada nueva = claves.buscarVigente(CLAVE, limite(despues)).orElseThrow();
        assertThat(nueva.idMedicamento()).isEqualTo("MED-0002");
        assertThat(nueva.respuesta()).isEqualTo(respuesta(9));
    }

    @Test
    void nuncaSobrescribeUnaClaveVigente() {
        Instant hecha = reloj.instant();
        claves.guardar(CLAVE, "MED-0001", 3, respuesta(17), hecha, limite(hecha));

        Instant pronto = hecha.plusSeconds(60);
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> claves.guardar(CLAVE, "MED-0002", 1, respuesta(9), pronto, limite(pronto)));
        assertThat(claves.buscarVigente(CLAVE, limite(pronto)).orElseThrow().idMedicamento())
                .isEqualTo("MED-0001");
    }

    @Test
    void purgarBorraSoloLasCaducadas() {
        Instant vieja = reloj.instant();
        claves.guardar("clave-vieja-0000000001", "MED-0001", 1, respuesta(1), vieja, limite(vieja));
        Instant reciente = vieja.plus(Duration.ofHours(20));
        claves.guardar("clave-reciente-0000001", "MED-0001", 1, respuesta(1), reciente, limite(reciente));

        Instant ahora = vieja.plus(Duration.ofHours(25));
        assertThat(claves.purgarCaducadas(limite(ahora))).isEqualTo(1);

        assertThat(jdbc.sql("SELECT clave FROM idempotencia_salidas").query(String.class).list())
                .containsExactly("clave-reciente-0000001");
    }

    @Test
    void bloquearExigeUnaTransaccion() {
        assertThatExceptionOfType(IllegalTransactionStateException.class)
                .isThrownBy(() -> claves.bloquear(CLAVE));
        transaccion.executeWithoutResult(estado -> claves.bloquear(CLAVE));
    }
}
