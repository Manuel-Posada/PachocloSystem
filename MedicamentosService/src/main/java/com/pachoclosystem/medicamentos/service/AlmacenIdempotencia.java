package com.pachoclosystem.medicamentos.service;

import com.pachoclosystem.medicamentos.dto.MedicamentoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Optional;

/**
 * Salidas de stock ya hechas, por clave de idempotencia, en memoria (como el
 * resto de datos del servicio: al reiniciar se pierden junto con el stock).
 *
 * <ul>
 *   <li>Cada entrada caduca a las {@code medicamentos.idempotencia.caducidad}
 *       (24 h por defecto) de hacerse la salida.</li>
 *   <li>Como mucho {@code medicamentos.idempotencia.max-entradas} (10 000 por
 *       defecto): al pasarse se descartan primero las caducadas y después las
 *       más antiguas. Una clave descartada se trataría como nueva.</li>
 *   <li>{@link #cerrojo(String)} da el bloqueo de una clave: con él se comprueba
 *       la clave, se hace la salida y se guarda sin que otra petición con la
 *       misma clave se cuele en medio. Hay un número fijo de cerrojos (claves
 *       distintas pueden compartir uno), así que no crecen con las claves.</li>
 * </ul>
 */
@Component
public class AlmacenIdempotencia {

    /** Lo que se recuerda de una salida hecha con una clave. */
    public record Salida(String idMedicamento, int cantidad, MedicamentoResponse respuesta, Instant hechaEn) {
    }

    private static final int CERROJOS = 64;

    private final Clock reloj;
    private final Duration caducidad;
    private final int maximoEntradas;
    /** En orden de inserción, que es el de tiempo: la primera es la más antigua. Se protege con su monitor. */
    private final LinkedHashMap<String, Salida> salidas = new LinkedHashMap<>();
    private final Object[] cerrojos = new Object[CERROJOS];

    public AlmacenIdempotencia(Clock reloj,
                               @Value("${medicamentos.idempotencia.caducidad:24h}") Duration caducidad,
                               @Value("${medicamentos.idempotencia.max-entradas:10000}") int maximoEntradas) {
        if (caducidad.isNegative() || caducidad.isZero() || maximoEntradas < 1) {
            throw new IllegalStateException("La caducidad y el máximo de entradas de idempotencia "
                    + "deben ser positivos.");
        }
        this.reloj = reloj;
        this.caducidad = caducidad;
        this.maximoEntradas = maximoEntradas;
        for (int i = 0; i < CERROJOS; i++) {
            cerrojos[i] = new Object();
        }
    }

    /** Cerrojo con el que se serializan las peticiones de una misma clave. */
    public Object cerrojo(String clave) {
        return cerrojos[Math.floorMod(clave.hashCode(), CERROJOS)];
    }

    /** La salida guardada con esa clave, si existe y no ha caducado. */
    public Optional<Salida> buscar(String clave) {
        synchronized (salidas) {
            Salida salida = salidas.get(clave);
            if (salida != null && caducada(salida)) {
                salidas.remove(clave);
                return Optional.empty();
            }
            return Optional.ofNullable(salida);
        }
    }

    public void guardar(String clave, String idMedicamento, int cantidad, MedicamentoResponse respuesta) {
        synchronized (salidas) {
            salidas.remove(clave);
            salidas.put(clave, new Salida(idMedicamento, cantidad, respuesta, reloj.instant()));
            purgar();
        }
    }

    /** Número de claves guardadas (para pruebas y diagnóstico). */
    public int tamano() {
        synchronized (salidas) {
            return salidas.size();
        }
    }

    private void purgar() {
        Iterator<Salida> iterador = salidas.values().iterator();
        while (iterador.hasNext()) {
            Salida masAntigua = iterador.next();
            boolean sobra = caducada(masAntigua) || salidas.size() > maximoEntradas;
            if (!sobra) {
                break;
            }
            iterador.remove();
        }
    }

    private boolean caducada(Salida salida) {
        return !reloj.instant().isBefore(salida.hechaEn().plus(caducidad));
    }
}
