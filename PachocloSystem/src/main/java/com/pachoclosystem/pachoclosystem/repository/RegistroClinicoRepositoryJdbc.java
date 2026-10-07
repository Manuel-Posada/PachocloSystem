package com.pachoclosystem.pachoclosystem.repository;

import com.pachoclosystem.pachoclosystem.model.AutorRegistro;
import com.pachoclosystem.pachoclosystem.model.NivelExperiencia;
import com.pachoclosystem.pachoclosystem.model.RegistroClinico;
import com.pachoclosystem.pachoclosystem.model.TipoRegistro;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Historial clínico en PostgreSQL (tabla {@code registros_clinicos}). Cada
 * registro guarda una copia del autor ({@code autor_*}), sin clave foránea al
 * trabajador: lo firmado no cambia aunque se edite o elimine al trabajador.
 */
@Repository
public class RegistroClinicoRepositoryJdbc implements IRegistroClinicoRepository {

    private static final String COLUMNAS = "r.id_registro, r.fecha, r.tipo, r.contenido, r.autor_id, "
            + "r.autor_nombre, r.autor_tipo, r.autor_especialidad, r.autor_nivel, r.id_medicamento, r.cantidad";

    private static final RowMapper<RegistroClinico> MAPEO = (fila, numero) -> registro(fila);

    private final JdbcClient jdbc;

    public RegistroClinicoRepositoryJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insertar(String idPaciente, RegistroClinico registro) {
        AutorRegistro autor = registro.getAutor();
        jdbc.sql("INSERT INTO registros_clinicos (id_registro, id_paciente, fecha, tipo, contenido, autor_id, "
                        + "autor_nombre, autor_tipo, autor_especialidad, autor_nivel, id_medicamento, cantidad) "
                        + "VALUES (:id, :idPaciente, :fecha, :tipo, :contenido, :autorId, :autorNombre, "
                        + ":autorTipo, :autorEspecialidad, :autorNivel, :idMedicamento, :cantidad)")
                .param("id", UUID.fromString(registro.getIdRegistro()))
                .param("idPaciente", idPaciente)
                .param("fecha", registro.getFecha())
                .param("tipo", registro.getTipo().name())
                .param("contenido", registro.getContenido())
                .param("autorId", autor.idTrabajador())
                .param("autorNombre", autor.nombreCompleto())
                .param("autorTipo", AutorRegistro.DOCTOR.equals(autor.rol()) ? "DOCTOR" : "ENFERMERO")
                .param("autorEspecialidad", autor.especialidad())
                .param("autorNivel", autor.nivelExperiencia() == null ? null : autor.nivelExperiencia().name())
                .param("idMedicamento", registro.getIdMedicamento())
                .param("cantidad", registro.getCantidad())
                .update();
    }

    @Override
    public List<RegistroClinico> listarPorPaciente(String idPaciente) {
        return jdbc.sql("SELECT " + COLUMNAS + " FROM registros_clinicos r WHERE r.id_paciente = :idPaciente "
                        + "ORDER BY r.orden")
                .param("idPaciente", idPaciente)
                .query(MAPEO)
                .list();
    }

    @Override
    public List<RegistroDePaciente> listarDePacientesActivos() {
        return jdbc.sql("SELECT p.id_paciente, p.nombre, " + COLUMNAS + " FROM registros_clinicos r "
                        + "JOIN pacientes p ON p.id_paciente = r.id_paciente WHERE p.activo "
                        + "ORDER BY r.fecha, p.orden, r.orden")
                .query((fila, numero) -> new RegistroDePaciente(fila.getString("id_paciente"),
                        fila.getString("nombre"), registro(fila)))
                .list();
    }

    private static RegistroClinico registro(ResultSet fila) throws SQLException {
        String nivel = fila.getString("autor_nivel");
        AutorRegistro autor = new AutorRegistro(
                fila.getString("autor_id"),
                fila.getString("autor_nombre"),
                "DOCTOR".equals(fila.getString("autor_tipo")) ? AutorRegistro.DOCTOR : AutorRegistro.ENFERMERO,
                fila.getString("autor_especialidad"),
                nivel == null ? null : NivelExperiencia.valueOf(nivel));
        return new RegistroClinico(
                fila.getObject("id_registro", UUID.class).toString(),
                fila.getObject("fecha", LocalDateTime.class),
                autor,
                TipoRegistro.valueOf(fila.getString("tipo")),
                fila.getString("contenido"),
                fila.getString("id_medicamento"),
                fila.getObject("cantidad", Integer.class));
    }
}
