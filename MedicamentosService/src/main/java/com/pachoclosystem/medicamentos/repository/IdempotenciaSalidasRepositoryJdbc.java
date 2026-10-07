package com.pachoclosystem.medicamentos.repository;

import com.pachoclosystem.medicamentos.dto.MedicamentoResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Claves de idempotencia de las salidas en PostgreSQL (tabla
 * {@code idempotencia_salidas}).
 *
 * <ul>
 *   <li>{@link #bloquear(String)} toma un <em>advisory lock</em> de transacción
 *       ({@code pg_advisory_xact_lock}) sobre el hash de la clave: serializa las
 *       peticiones con la misma clave aunque haya varias instancias del servicio,
 *       y se suelta solo al terminar la transacción. Dos claves distintas con el
 *       mismo hash solo se esperan entre sí, sin otro efecto.</li>
 *   <li>La respuesta se guarda como JSONB con el mismo {@link JsonMapper} que usa
 *       la API, y se vuelve a leer como {@link MedicamentoResponse}: la repetición
 *       devuelve el mismo JSON.</li>
 *   <li>La clave primaria sobre {@code clave} es la garantía final: nunca hay dos
 *       salidas guardadas con la misma clave.</li>
 * </ul>
 */
@Repository
public class IdempotenciaSalidasRepositoryJdbc implements IdempotenciaSalidasRepository {

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public IdempotenciaSalidasRepositoryJdbc(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** Fuera de una transacción el bloqueo se soltaría en el acto: se exige una. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void bloquear(String clave) {
        jdbc.sql("SELECT pg_advisory_xact_lock(hashtextextended(:clave, 0))")
                .param("clave", clave)
                .query((fila, numero) -> Boolean.TRUE)
                .single();
    }

    @Override
    public Optional<SalidaRegistrada> buscarVigente(String clave, Instant limite) {
        return jdbc.sql("SELECT id_medicamento, cantidad, respuesta, hecha_en FROM idempotencia_salidas "
                        + "WHERE clave = :clave AND hecha_en > :limite")
                .param("clave", clave)
                .param("limite", enUtc(limite))
                .query((fila, numero) -> new SalidaRegistrada(
                        fila.getString("id_medicamento"),
                        fila.getInt("cantidad"),
                        json.readValue(fila.getString("respuesta"), MedicamentoResponse.class),
                        fila.getObject("hecha_en", OffsetDateTime.class).toInstant()))
                .optional();
    }

    @Override
    public void guardar(String clave, String idMedicamento, int cantidad, MedicamentoResponse respuesta,
                        Instant hechaEn, Instant limite) {
        int filas = jdbc.sql("INSERT INTO idempotencia_salidas (clave, id_medicamento, cantidad, respuesta, hecha_en) "
                        + "VALUES (:clave, :idMedicamento, :cantidad, CAST(:respuesta AS jsonb), :hechaEn) "
                        + "ON CONFLICT (clave) DO UPDATE SET id_medicamento = EXCLUDED.id_medicamento, "
                        + "cantidad = EXCLUDED.cantidad, respuesta = EXCLUDED.respuesta, hecha_en = EXCLUDED.hecha_en "
                        + "WHERE idempotencia_salidas.hecha_en <= :limite")
                .param("clave", clave)
                .param("idMedicamento", idMedicamento)
                .param("cantidad", cantidad)
                .param("respuesta", json.writeValueAsString(respuesta))
                .param("hechaEn", enUtc(hechaEn))
                .param("limite", enUtc(limite))
                .update();
        if (filas == 0) {
            // Solo pasa si la clave seguía vigente, algo que bloquear() + buscarVigente()
            // ya descartan: se aborta la transacción (y con ella la salida) antes que
            // sobrescribir una salida válida.
            throw new IllegalStateException("La clave de idempotencia ya tiene una salida vigente.");
        }
    }

    @Override
    public int purgarCaducadas(Instant limite) {
        return jdbc.sql("DELETE FROM idempotencia_salidas WHERE hecha_en <= :limite")
                .param("limite", enUtc(limite))
                .update();
    }

    private static OffsetDateTime enUtc(Instant instante) {
        return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }
}
