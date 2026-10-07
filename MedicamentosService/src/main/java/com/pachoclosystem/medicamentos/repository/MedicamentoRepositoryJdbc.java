package com.pachoclosystem.medicamentos.repository;

import com.pachoclosystem.medicamentos.model.DatosMedicamento;
import com.pachoclosystem.medicamentos.model.Medicamento;
import com.pachoclosystem.medicamentos.model.Presentacion;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Medicamentos en PostgreSQL (tabla {@code medicamentos}, ver
 * {@code db/migration}).
 *
 * <ul>
 *   <li>La unicidad de nombre + concentración + presentación + lote la decide la
 *       restricción UNIQUE sobre {@code clave_unica}, que guarda
 *       {@link DatosMedicamento#claveUnica()}: la normalización es la de Java.</li>
 *   <li>El id sale de la secuencia {@code medicamentos_id_seq} con el formato
 *       {@code MED-0001}. Un alta que falla también consume su número.</li>
 *   <li>{@link #buscarPorIdParaActualizar} usa {@code SELECT ... FOR UPDATE}: el
 *       bloqueo dura hasta el final de la transacción.</li>
 * </ul>
 */
@Repository
public class MedicamentoRepositoryJdbc implements IMedicamentoRepository {

    private static final String PREFIJO_ID = "MED";

    private static final String COLUMNAS = "id_medicamento, nombre, principio_activo, presentacion, "
            + "concentracion, laboratorio, lote, stock_minimo, cantidad_stock, fecha_vencimiento, ubicacion";

    private static final RowMapper<Medicamento> MAPEO = (fila, numero) -> new Medicamento(
            fila.getString("id_medicamento"),
            new DatosMedicamento(
                    fila.getString("nombre"),
                    fila.getString("principio_activo"),
                    Presentacion.valueOf(fila.getString("presentacion")),
                    fila.getString("concentracion"),
                    fila.getString("laboratorio"),
                    fila.getString("lote"),
                    fila.getInt("stock_minimo"),
                    fila.getObject("fecha_vencimiento", LocalDate.class),
                    fila.getString("ubicacion")),
            fila.getInt("cantidad_stock"));

    private final JdbcClient jdbc;

    public MedicamentoRepositoryJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public String generarNuevoId() {
        long numero = jdbc.sql("SELECT nextval('medicamentos_id_seq')").query(Long.class).single();
        return String.format("%s-%04d", PREFIJO_ID, numero);
    }

    @Override
    public boolean guardar(Medicamento medicamento) {
        if (medicamento == null) {
            return false;
        }
        DatosMedicamento datos = medicamento.getDatos();
        try {
            jdbc.sql("INSERT INTO medicamentos (" + COLUMNAS + ", clave_unica) VALUES (:id, :nombre, "
                            + ":principioActivo, :presentacion, :concentracion, :laboratorio, :lote, "
                            + ":stockMinimo, :cantidadStock, :fechaVencimiento, :ubicacion, :claveUnica)")
                    .param("id", medicamento.getIdMedicamento())
                    .params(parametros(datos))
                    .param("cantidadStock", medicamento.getCantidadStock())
                    .update();
            return true;
        } catch (DuplicateKeyException duplicado) {
            return false;
        }
    }

    @Override
    public ResultadoActualizacion actualizarDatos(Medicamento medicamento, DatosMedicamento nuevosDatos) {
        int filas;
        try {
            filas = jdbc.sql("UPDATE medicamentos SET nombre = :nombre, principio_activo = :principioActivo, "
                            + "presentacion = :presentacion, concentracion = :concentracion, "
                            + "laboratorio = :laboratorio, lote = :lote, stock_minimo = :stockMinimo, "
                            + "fecha_vencimiento = :fechaVencimiento, ubicacion = :ubicacion, "
                            + "clave_unica = :claveUnica WHERE id_medicamento = :id")
                    .param("id", medicamento.getIdMedicamento())
                    .params(parametros(nuevosDatos))
                    .update();
        } catch (DuplicateKeyException duplicado) {
            return ResultadoActualizacion.DUPLICADO;
        }
        if (filas == 0) {
            return ResultadoActualizacion.NO_EXISTE;
        }
        medicamento.setDatos(nuevosDatos);
        return ResultadoActualizacion.ACTUALIZADO;
    }

    @Override
    public void actualizarStock(String id, int cantidadStock) {
        jdbc.sql("UPDATE medicamentos SET cantidad_stock = :cantidad WHERE id_medicamento = :id")
                .param("cantidad", cantidadStock)
                .param("id", id)
                .update();
    }

    @Override
    public Medicamento buscarPorId(String id) {
        if (id == null) {
            return null;
        }
        return jdbc.sql("SELECT " + COLUMNAS + " FROM medicamentos WHERE id_medicamento = :id")
                .param("id", id)
                .query(MAPEO)
                .optional()
                .orElse(null);
    }

    /** Fuera de una transacción el bloqueo no serviría de nada: se exige una. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Medicamento buscarPorIdParaActualizar(String id) {
        if (id == null) {
            return null;
        }
        return jdbc.sql("SELECT " + COLUMNAS + " FROM medicamentos WHERE id_medicamento = :id FOR UPDATE")
                .param("id", id)
                .query(MAPEO)
                .optional()
                .orElse(null);
    }

    /** Ordenados por id como {@code String.compareTo} (los ids son ASCII). */
    @Override
    public List<Medicamento> listarTodos() {
        return jdbc.sql("SELECT " + COLUMNAS + " FROM medicamentos ORDER BY id_medicamento COLLATE \"C\"")
                .query(MAPEO)
                .list();
    }

    @Override
    public boolean eliminar(String id) {
        if (id == null) {
            return false;
        }
        return jdbc.sql("DELETE FROM medicamentos WHERE id_medicamento = :id")
                .param("id", id)
                .update() > 0;
    }

    private static Map<String, Object> parametros(DatosMedicamento datos) {
        return Map.ofEntries(
                Map.entry("nombre", datos.nombre()),
                Map.entry("principioActivo", datos.principioActivo()),
                Map.entry("presentacion", datos.presentacion().name()),
                Map.entry("concentracion", datos.concentracion()),
                Map.entry("laboratorio", datos.laboratorio()),
                Map.entry("lote", datos.lote()),
                Map.entry("stockMinimo", datos.stockMinimo()),
                Map.entry("fechaVencimiento", datos.fechaVencimiento()),
                Map.entry("ubicacion", datos.ubicacion()),
                Map.entry("claveUnica", datos.claveUnica()));
    }
}
