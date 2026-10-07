package com.pachoclosystem.medicamentos.repository;

import com.pachoclosystem.medicamentos.dto.MedicamentoResponse;

import java.time.Instant;
import java.util.Optional;

/**
 * Salidas de stock ya hechas, por clave de idempotencia. Todas las operaciones
 * deben ejecutarse en la misma transacción que la salida, empezando por
 * {@link #bloquear(String)}.
 */
public interface IdempotenciaSalidasRepository {

    /** Lo que se recuerda de una salida hecha con una clave. */
    record SalidaRegistrada(String idMedicamento, int cantidad, MedicamentoResponse respuesta, Instant hechaEn) {
    }

    /**
     * Bloquea la clave hasta el final de la transacción en curso: otra petición
     * con la misma clave espera hasta que esta termine.
     */
    void bloquear(String clave);

    /** La salida guardada con esa clave si se hizo después de {@code limite} (no ha caducado). */
    Optional<SalidaRegistrada> buscarVigente(String clave, Instant limite);

    /**
     * Guarda la salida. Si la clave ya existía pero había caducado (anterior o
     * igual a {@code limite}), la sustituye; si seguía vigente, falla.
     */
    void guardar(String clave, String idMedicamento, int cantidad, MedicamentoResponse respuesta,
                 Instant hechaEn, Instant limite);

    /** Borra las salidas caducadas (anteriores o iguales a {@code limite}). Devuelve cuántas. */
    int purgarCaducadas(Instant limite);
}
