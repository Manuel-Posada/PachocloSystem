package com.pachoclosystem.medicamentos.repository;

import com.pachoclosystem.medicamentos.model.DatosMedicamento;
import com.pachoclosystem.medicamentos.model.Medicamento;

import java.util.List;

/**
 * Almacenamiento de medicamentos. La implementación actual es en memoria; esta
 * interfaz permite sustituirla por una de base de datos sin tocar el servicio.
 */
public interface IMedicamentoRepository {

    String generarNuevoId();

    /**
     * Inserta o vuelve a guardar un medicamento. Devuelve {@code false} (y no
     * guarda nada) si otro medicamento ya tiene su misma clave única.
     */
    boolean guardar(Medicamento medicamento);

    /**
     * Sustituye los datos de un medicamento existente. Devuelve {@code false}
     * (y no cambia nada) si los nuevos datos chocan con la clave única de otro.
     */
    boolean actualizarDatos(Medicamento medicamento, DatosMedicamento nuevosDatos);

    Medicamento buscarPorId(String id);

    /** Todos los medicamentos, ordenados por id. */
    List<Medicamento> listarTodos();

    boolean eliminar(String id);
}
