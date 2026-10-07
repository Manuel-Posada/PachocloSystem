package com.pachoclosystem.medicamentos.repository;

import com.pachoclosystem.medicamentos.model.DatosMedicamento;
import com.pachoclosystem.medicamentos.model.Medicamento;

import java.util.List;

/**
 * Almacenamiento de medicamentos. La implementación de la aplicación es
 * {@link MedicamentoRepositoryJdbc} (PostgreSQL).
 *
 * <p>Los métodos que modifican un medicamento existente esperan que quien llama
 * lo haya leído antes con {@link #buscarPorIdParaActualizar(String)} dentro de la
 * misma transacción: así nadie más lo cambia entre la lectura y la escritura.</p>
 */
public interface IMedicamentoRepository {

    /** Resultado de {@link #actualizarDatos}. */
    enum ResultadoActualizacion {
        ACTUALIZADO,
        /** Los nuevos datos chocan con la clave única de otro medicamento: no cambia nada. */
        DUPLICADO,
        /** El medicamento ya no existe (p. ej. se eliminó a la vez). */
        NO_EXISTE
    }

    String generarNuevoId();

    /**
     * Inserta un medicamento nuevo. Devuelve {@code false} (y no guarda nada) si
     * otro medicamento ya tiene su misma clave única o su id.
     */
    boolean guardar(Medicamento medicamento);

    /**
     * Sustituye los datos descriptivos de un medicamento (no el stock) y, si se
     * aplican, también los del objeto recibido.
     */
    ResultadoActualizacion actualizarDatos(Medicamento medicamento, DatosMedicamento nuevosDatos);

    /** Guarda el stock de un medicamento ya leído con {@link #buscarPorIdParaActualizar}. */
    void actualizarStock(String id, int cantidadStock);

    Medicamento buscarPorId(String id);

    /**
     * Como {@link #buscarPorId(String)}, pero bloquea el medicamento hasta el final
     * de la transacción en curso: otra petición que quiera modificarlo espera.
     */
    Medicamento buscarPorIdParaActualizar(String id);

    /** Todos los medicamentos, ordenados por id. */
    List<Medicamento> listarTodos();

    boolean eliminar(String id);
}
