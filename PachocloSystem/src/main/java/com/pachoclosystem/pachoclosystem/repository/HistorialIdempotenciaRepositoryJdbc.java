package com.pachoclosystem.pachoclosystem.repository;

import com.pachoclosystem.pachoclosystem.dto.RegistroResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Claves de idempotencia del historial en PostgreSQL (tabla
 * {@code historial_idempotencia}).
 *
 * <ul>
 *   <li>El registro creado se guarda como JSONB con el mismo {@link JsonMapper}
 *       que usa la API y se vuelve a leer como {@link RegistroResponse}: la
 *       repetición devuelve el mismo JSON. {@code respuesta} NULL = sin confirmar.</li>
 *   <li>Las peticiones con la misma clave ya van de una en una (cerrojo en la JVM
 *       de {@code AlmacenIdempotenciaHistorial}), así que guardar sustituye sin más
 *       el uso anterior. La clave primaria sobre {@code clave} es la garantía final.</li>
 * </ul>
 */
@Repository
public class HistorialIdempotenciaRepositoryJdbc implements IHistorialIdempotenciaRepository {

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public HistorialIdempotenciaRepositoryJdbc(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public Optional<UsoGuardado> buscarVigente(String clave, Instant limite) {
        return jdbc.sql("SELECT id_usuario, id_paciente, huella, respuesta, usada_en FROM historial_idempotencia "
                        + "WHERE clave = :clave AND usada_en > :limite")
                .param("clave", clave)
                .param("limite", enUtc(limite))
                .query((fila, numero) -> {
                    String respuesta = fila.getString("respuesta");
                    return new UsoGuardado(
                            fila.getString("id_usuario"),
                            fila.getString("id_paciente"),
                            fila.getString("huella"),
                            respuesta == null ? null : json.readValue(respuesta, RegistroResponse.class),
                            fila.getObject("usada_en", OffsetDateTime.class).toInstant());
                })
                .optional();
    }

    @Override
    public void guardar(String clave, String idUsuario, String idPaciente, String huella, RegistroResponse registro,
                        Instant usadaEn) {
        jdbc.sql("INSERT INTO historial_idempotencia (clave, id_usuario, id_paciente, huella, respuesta, usada_en) "
                        + "VALUES (:clave, :idUsuario, :idPaciente, :huella, CAST(:respuesta AS jsonb), :usadaEn) "
                        + "ON CONFLICT (clave) DO UPDATE SET id_usuario = EXCLUDED.id_usuario, "
                        + "id_paciente = EXCLUDED.id_paciente, huella = EXCLUDED.huella, "
                        + "respuesta = EXCLUDED.respuesta, usada_en = EXCLUDED.usada_en")
                .param("clave", clave)
                .param("idUsuario", idUsuario)
                .param("idPaciente", idPaciente)
                .param("huella", huella)
                .param("respuesta", registro == null ? null : json.writeValueAsString(registro))
                .param("usadaEn", enUtc(usadaEn))
                .update();
    }

    @Override
    public int purgarCaducadas(Instant limite) {
        return jdbc.sql("DELETE FROM historial_idempotencia WHERE usada_en <= :limite")
                .param("limite", enUtc(limite))
                .update();
    }

    @Override
    public long contarVigentes(Instant limite) {
        return jdbc.sql("SELECT count(*) FROM historial_idempotencia WHERE usada_en > :limite")
                .param("limite", enUtc(limite))
                .query(Long.class)
                .single();
    }

    private static OffsetDateTime enUtc(Instant instante) {
        return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }
}
