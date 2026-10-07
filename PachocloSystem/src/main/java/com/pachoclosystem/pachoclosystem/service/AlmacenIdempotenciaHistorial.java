package com.pachoclosystem.pachoclosystem.service;

import com.pachoclosystem.pachoclosystem.dto.RegistroResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Claves {@code Idempotency-Key} ya usadas al crear registros del historial, en
 * memoria (como el resto de datos: al reiniciar se pierden).
 *
 * <ul>
 *   <li>Cada clave recuerda con qué usuario, paciente y cuerpo (huella) se usó y,
 *       si se llegó a crear, el registro. Sin registro significa que el intento
 *       no se pudo confirmar y se puede repetir con la misma petición.</li>
 *   <li>Cada entrada caduca a las {@code historial.idempotencia.caducidad} (24 h
 *       por defecto) de guardarse.</li>
 *   <li>Como mucho {@code historial.idempotencia.max-entradas} (10 000 por
 *       defecto): al pasarse se descartan primero las caducadas y después las más
 *       antiguas. Una clave descartada se trataría como nueva.</li>
 *   <li>{@link #conClave(String, Supplier)} ejecuta con el cerrojo de una clave:
 *       las peticiones con la misma clave van de una en una. El cerrojo es de esa
 *       clave sola (dura mientras haya peticiones con ella), así que una petición
 *       que espera a MedicamentosService no frena a las de otras claves.</li>
 * </ul>
 */
@Component
public class AlmacenIdempotenciaHistorial {

    /** Petición con la que se usó una clave; {@code registro} es null si no se pudo confirmar. */
    public record Uso(String idUsuario, String idPaciente, String huella, RegistroResponse registro,
                      Instant usadaEn) {

        public boolean esDe(String idUsuario, String idPaciente, String huella) {
            return this.idUsuario.equals(idUsuario) && this.idPaciente.equals(idPaciente)
                    && this.huella.equals(huella);
        }
    }

    /** Cerrojo de una clave y cuántas peticiones lo usan o esperan (protegido por el mapa). */
    private static final class Cerrojo {
        private final ReentrantLock bloqueo = new ReentrantLock();
        private volatile int peticiones;
    }

    private final Clock reloj;
    private final Duration caducidad;
    private final int maximoEntradas;
    /** En orden de inserción, que es el de tiempo: la primera es la más antigua. Se protege con su monitor. */
    private final LinkedHashMap<String, Uso> usos = new LinkedHashMap<>();
    private final ConcurrentHashMap<String, Cerrojo> cerrojos = new ConcurrentHashMap<>();

    @Autowired
    public AlmacenIdempotenciaHistorial(@Value("${historial.idempotencia.caducidad:24h}") Duration caducidad,
                                        @Value("${historial.idempotencia.max-entradas:10000}") int maximoEntradas) {
        this(Clock.systemUTC(), caducidad, maximoEntradas);
    }

    AlmacenIdempotenciaHistorial(Clock reloj, Duration caducidad, int maximoEntradas) {
        if (caducidad.isNegative() || caducidad.isZero() || maximoEntradas < 1) {
            throw new IllegalStateException("La caducidad y el máximo de entradas de idempotencia "
                    + "del historial deben ser positivos.");
        }
        this.reloj = reloj;
        this.caducidad = caducidad;
        this.maximoEntradas = maximoEntradas;
    }

    /** Ejecuta {@code accion} con el cerrojo de la clave: nadie más con esa clave entra a la vez. */
    public <T> T conClave(String clave, Supplier<T> accion) {
        Cerrojo cerrojo = cerrojos.compute(clave, (k, actual) -> {
            Cerrojo c = actual == null ? new Cerrojo() : actual;
            c.peticiones++;
            return c;
        });
        cerrojo.bloqueo.lock();
        try {
            return accion.get();
        } finally {
            cerrojo.bloqueo.unlock();
            cerrojos.computeIfPresent(clave, (k, c) -> --c.peticiones == 0 ? null : c);
        }
    }

    /** El uso guardado de esa clave, si existe y no ha caducado. */
    public Optional<Uso> buscar(String clave) {
        synchronized (usos) {
            Uso uso = usos.get(clave);
            if (uso != null && caducado(uso)) {
                usos.remove(clave);
                return Optional.empty();
            }
            return Optional.ofNullable(uso);
        }
    }

    /** Guarda (o sustituye) el uso de una clave; {@code registro} null = sin confirmar. */
    public void guardar(String clave, String idUsuario, String idPaciente, String huella,
                        RegistroResponse registro) {
        synchronized (usos) {
            usos.remove(clave);
            usos.put(clave, new Uso(idUsuario, idPaciente, huella, registro, reloj.instant()));
            purgar();
        }
    }

    /** Número de claves guardadas (para pruebas y diagnóstico). */
    public int tamano() {
        synchronized (usos) {
            return usos.size();
        }
    }

    /** Peticiones con esa clave en curso o esperando su turno (para pruebas y diagnóstico). */
    public int peticionesConClave(String clave) {
        Cerrojo cerrojo = cerrojos.get(clave);
        return cerrojo == null ? 0 : cerrojo.peticiones;
    }

    /**
     * Cerrojos vivos (para pruebas y diagnóstico): solo los de claves con
     * peticiones en curso o esperando. No dependen de la caducidad de la clave:
     * el de una clave se borra en cuanto termina su última petición.
     */
    public int cerrojosActivos() {
        return cerrojos.size();
    }

    private void purgar() {
        Iterator<Uso> iterador = usos.values().iterator();
        while (iterador.hasNext()) {
            Uso masAntiguo = iterador.next();
            boolean sobra = caducado(masAntiguo) || usos.size() > maximoEntradas;
            if (!sobra) {
                break;
            }
            iterador.remove();
        }
    }

    private boolean caducado(Uso uso) {
        return !reloj.instant().isBefore(uso.usadaEn().plus(caducidad));
    }
}
