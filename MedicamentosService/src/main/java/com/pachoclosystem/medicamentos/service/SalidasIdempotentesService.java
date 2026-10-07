package com.pachoclosystem.medicamentos.service;

import com.pachoclosystem.medicamentos.dto.MedicamentoResponse;
import com.pachoclosystem.medicamentos.exception.ConflictoException;
import com.pachoclosystem.medicamentos.exception.SolicitudInvalidaException;
import com.pachoclosystem.medicamentos.model.Medicamento;
import com.pachoclosystem.medicamentos.repository.IdempotenciaSalidasRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Salidas de stock con clave de idempotencia ({@code Idempotency-Key}): repetir
 * la petición con la misma clave y los mismos datos devuelve la misma
 * respuesta sin volver a descontar. Así quien llama (PachocloSystem) puede
 * reintentar tras un tiempo de espera sin riesgo de descontar dos veces.
 *
 * <ul>
 *   <li>Misma clave con otro medicamento u otra cantidad: 409.</li>
 *   <li>Dos peticiones simultáneas con la misma clave se serializan (bloqueo de
 *       la clave en PostgreSQL): una hace la salida y la otra recibe su
 *       respuesta.</li>
 *   <li>La salida y el registro de la clave van en la misma transacción. Una
 *       salida que falla (stock insuficiente, vencido, inexistente) no deja
 *       nada guardado, así que al reintentarla se vuelve a evaluar: puede salir
 *       bien si entre tanto entró stock, o volver a fallar.</li>
 *   <li>Cada clave caduca a las {@code medicamentos.idempotencia.caducidad} (24 h
 *       por defecto) de hacerse la salida; después se trata como nueva. Las
 *       caducadas se borran al registrar salidas. Las claves sobreviven a un
 *       reinicio del servicio.</li>
 * </ul>
 *
 * <p>Sin clave, la salida es la de {@link MedicamentoService#registrarSalida}.</p>
 */
@Service
public class SalidasIdempotentesService {

    /** Letras, dígitos, guion y guion bajo; de 16 a 100 (un UUID tiene 36). */
    private static final Pattern PATRON_CLAVE = Pattern.compile("^[A-Za-z0-9_-]{16,100}$");

    /** Respuesta de la salida y si es la repetición de una ya hecha. */
    public record Resultado(MedicamentoResponse respuesta, boolean repetida) {
    }

    private final MedicamentoService medicamentos;
    private final IdempotenciaSalidasRepository claves;
    private final Clock reloj;
    private final Duration caducidad;

    public SalidasIdempotentesService(MedicamentoService medicamentos, IdempotenciaSalidasRepository claves,
                                      Clock reloj,
                                      @Value("${medicamentos.idempotencia.caducidad:24h}") Duration caducidad) {
        if (caducidad.isNegative() || caducidad.isZero()) {
            throw new IllegalStateException("La caducidad de las claves de idempotencia debe ser positiva.");
        }
        this.medicamentos = medicamentos;
        this.claves = claves;
        this.reloj = reloj;
        this.caducidad = caducidad;
    }

    @Transactional
    public Resultado registrarSalida(String idMedicamento, int cantidad, String clave) {
        if (clave == null || !PATRON_CLAVE.matcher(clave).matches()) {
            throw new SolicitudInvalidaException("La cabecera Idempotency-Key debe tener entre 16 y 100 "
                    + "caracteres: letras sin tilde, dígitos, guion o guion bajo (por ejemplo, un UUID).");
        }
        claves.bloquear(clave);
        Instant ahora = reloj.instant();
        Instant limite = ahora.minus(caducidad);

        Optional<IdempotenciaSalidasRepository.SalidaRegistrada> previa = claves.buscarVigente(clave, limite);
        if (previa.isPresent()) {
            IdempotenciaSalidasRepository.SalidaRegistrada salida = previa.get();
            if (!salida.idMedicamento().equals(idMedicamento) || salida.cantidad() != cantidad) {
                throw new ConflictoException("La clave de idempotencia ya se usó para otra salida de "
                        + "stock con otro medicamento o cantidad.");
            }
            return new Resultado(salida.respuesta(), true);
        }

        // Misma transacción: si la salida falla, la excepción deshace todo y la clave queda libre.
        Medicamento medicamento = medicamentos.registrarSalida(idMedicamento, cantidad);
        MedicamentoResponse respuesta = MedicamentoResponse.from(medicamento, medicamentos.hoy());
        claves.purgarCaducadas(limite);
        claves.guardar(clave, idMedicamento, cantidad, respuesta, ahora, limite);
        return new Resultado(respuesta, false);
    }
}
