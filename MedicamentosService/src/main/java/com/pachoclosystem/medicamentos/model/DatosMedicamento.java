package com.pachoclosystem.medicamentos.model;

import java.time.LocalDate;
import java.util.Locale;

/**
 * Datos descriptivos de un medicamento (todo salvo el id y el stock).
 *
 * <p>Es inmutable: editar un medicamento sustituye la instancia completa, de modo
 * que nadie puede leer una mezcla de datos viejos y nuevos.</p>
 */
public record DatosMedicamento(
        String nombre,
        String principioActivo,
        Presentacion presentacion,
        String concentracion,
        String laboratorio,
        String lote,
        int stockMinimo,
        LocalDate fechaVencimiento,
        String ubicacion) {

    /**
     * Clave de unicidad: nombre + concentración + presentación + lote, sin
     * distinguir mayúsculas ni espacios repetidos.
     */
    public String claveUnica() {
        return normalizar(nombre) + "|" + normalizar(concentracion) + "|" + presentacion + "|" + normalizar(lote);
    }

    private static String normalizar(String texto) {
        return texto.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
