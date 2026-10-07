package com.pachoclosystem.pachoclosystem.repository;

import com.pachoclosystem.pachoclosystem.model.Doctor;
import com.pachoclosystem.pachoclosystem.model.Enfermero;
import com.pachoclosystem.pachoclosystem.model.NivelExperiencia;
import com.pachoclosystem.pachoclosystem.model.TrabajadorHospital;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Trabajadores en PostgreSQL (tabla {@code trabajadores}): doctores y
 * enfermeros en una sola tabla, distinguidos por {@code tipo}. Los ids DOC-0001
 * y ENF-0001 salen de una secuencia por tipo. El borrado es físico.
 */
@Repository
public class TrabajadorRepositoryJdbc implements ITrabajadoresRepository {

    private static final Map<String, String> SECUENCIA_POR_PREFIJO =
            Map.of("DOC", "doctores_id_seq", "ENF", "enfermeros_id_seq");

    private static final String COLUMNAS = "id_trabajador, tipo, nombre_completo, especialidad, nivel_experiencia";

    private static final RowMapper<TrabajadorHospital> MAPEO = (fila, numero) ->
            "DOCTOR".equals(fila.getString("tipo"))
                    ? new Doctor(fila.getString("id_trabajador"), fila.getString("nombre_completo"),
                            fila.getString("especialidad"))
                    : new Enfermero(fila.getString("id_trabajador"), fila.getString("nombre_completo"),
                            NivelExperiencia.valueOf(fila.getString("nivel_experiencia")));

    private final JdbcClient jdbc;

    public TrabajadorRepositoryJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public String generarNuevoId(String prefijo) {
        String secuencia = SECUENCIA_POR_PREFIJO.get(prefijo);
        if (secuencia == null) {
            throw new IllegalArgumentException("Prefijo de trabajador desconocido: " + prefijo);
        }
        // El nombre de la secuencia sale de la tabla fija de arriba, nunca de la petición.
        long numero = jdbc.sql("SELECT nextval('" + secuencia + "')").query(Long.class).single();
        return String.format("%s-%04d", prefijo, numero);
    }

    @Override
    public boolean guardarTrabajador(TrabajadorHospital u) {
        if (u == null) {
            return false;
        }
        jdbc.sql("INSERT INTO trabajadores (" + COLUMNAS + ") VALUES (:id, :tipo, :nombre, :especialidad, :nivel)")
                .param("id", u.getIdTrabajador())
                .params(datos(u))
                .update();
        return true;
    }

    @Override
    public boolean actualizarTrabajador(TrabajadorHospital t) {
        return jdbc.sql("UPDATE trabajadores SET nombre_completo = :nombre, especialidad = :especialidad, "
                        + "nivel_experiencia = :nivel WHERE id_trabajador = :id AND tipo = :tipo")
                .param("id", t.getIdTrabajador())
                .params(datos(t))
                .update() > 0;
    }

    @Override
    public TrabajadorHospital buscarPorId(String id) {
        if (id == null) {
            return null;
        }
        return jdbc.sql("SELECT " + COLUMNAS + " FROM trabajadores WHERE id_trabajador = :id")
                .param("id", id).query(MAPEO).optional().orElse(null);
    }

    /** Fuera de una transacción el bloqueo no serviría de nada: se exige una. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public TrabajadorHospital buscarPorIdParaActualizar(String id) {
        if (id == null) {
            return null;
        }
        return jdbc.sql("SELECT " + COLUMNAS + " FROM trabajadores WHERE id_trabajador = :id FOR UPDATE")
                .param("id", id).query(MAPEO).optional().orElse(null);
    }

    @Override
    public boolean eliminarTrabajador(String id) {
        if (id == null) {
            return false;
        }
        return jdbc.sql("DELETE FROM trabajadores WHERE id_trabajador = :id").param("id", id).update() > 0;
    }

    @Override
    public List<TrabajadorHospital> obtenerTodos() {
        return jdbc.sql("SELECT " + COLUMNAS + " FROM trabajadores ORDER BY orden").query(MAPEO).list();
    }

    /** Tipo y datos propios del tipo; el que no corresponde va a NULL. */
    private static Map<String, Object> datos(TrabajadorHospital t) {
        HashMap<String, Object> datos = new HashMap<>();
        datos.put("nombre", t.getNombreCompleto());
        if (t instanceof Doctor d) {
            datos.put("tipo", "DOCTOR");
            datos.put("especialidad", d.getEspecialidad());
            datos.put("nivel", null);
        } else {
            Enfermero e = (Enfermero) t;
            datos.put("tipo", "ENFERMERO");
            datos.put("especialidad", null);
            datos.put("nivel", e.getNivelExperiencia() == null ? null : e.getNivelExperiencia().name());
        }
        return datos;
    }
}
