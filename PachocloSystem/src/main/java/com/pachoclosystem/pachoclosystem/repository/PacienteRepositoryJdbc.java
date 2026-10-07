package com.pachoclosystem.pachoclosystem.repository;

import com.pachoclosystem.pachoclosystem.model.Paciente;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Pacientes en PostgreSQL (tabla {@code pacientes}). Ids PAC-0001 desde la
 * secuencia {@code pacientes_id_seq}. Cada modificación es un único
 * {@code UPDATE ... WHERE activo}: atómica, sin leer y escribir por separado,
 * así que dos bajas simultáneas del mismo paciente dan una 204 y una 404.
 */
@Repository
public class PacienteRepositoryJdbc implements IPacienteRepository {

    private static final String COLUMNAS = "id_paciente, nombre, edad, habitacion, activo";

    private static final RowMapper<Paciente> MAPEO = (fila, numero) -> new Paciente(
            fila.getString("id_paciente"), fila.getString("nombre"), fila.getInt("edad"),
            fila.getInt("habitacion"), fila.getBoolean("activo"));

    private final JdbcClient jdbc;

    public PacienteRepositoryJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public String generarNuevoId() {
        long numero = jdbc.sql("SELECT nextval('pacientes_id_seq')").query(Long.class).single();
        return String.format("PAC-%04d", numero);
    }

    @Override
    public Paciente buscarPorId(String id) {
        if (id == null) {
            return null;
        }
        return jdbc.sql("SELECT " + COLUMNAS + " FROM pacientes WHERE id_paciente = :id")
                .param("id", id).query(MAPEO).optional().orElse(null);
    }

    @Override
    public boolean guardarPaciente(Paciente p) {
        try {
            jdbc.sql("INSERT INTO pacientes (id_paciente, nombre, edad, habitacion, activo) "
                            + "VALUES (:id, :nombre, :edad, :habitacion, :activo)")
                    .param("id", p.getIdPaciente())
                    .param("nombre", p.getNombre())
                    .param("edad", p.getEdad())
                    .param("habitacion", p.getHabitacion())
                    .param("activo", p.isActivo())
                    .update();
            return true;
        } catch (DuplicateKeyException idOcupado) {
            return false;
        }
    }

    @Override
    public boolean actualizarDatos(String idPaciente, String nombre, int edad, int habitacion) {
        return jdbc.sql("UPDATE pacientes SET nombre = :nombre, edad = :edad, habitacion = :habitacion "
                        + "WHERE id_paciente = :id AND activo")
                .param("nombre", nombre)
                .param("edad", edad)
                .param("habitacion", habitacion)
                .param("id", idPaciente)
                .update() > 0;
    }

    @Override
    public boolean actualizarHabitacion(String idPaciente, int habitacion) {
        return jdbc.sql("UPDATE pacientes SET habitacion = :habitacion WHERE id_paciente = :id AND activo")
                .param("habitacion", habitacion)
                .param("id", idPaciente)
                .update() > 0;
    }

    @Override
    public boolean darDeBaja(String idPaciente) {
        return jdbc.sql("UPDATE pacientes SET activo = FALSE WHERE id_paciente = :id AND activo")
                .param("id", idPaciente)
                .update() > 0;
    }

    @Override
    public List<Paciente> obtenerTodos() {
        return jdbc.sql("SELECT " + COLUMNAS + " FROM pacientes ORDER BY orden").query(MAPEO).list();
    }
}
