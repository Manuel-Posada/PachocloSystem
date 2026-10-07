package com.pachoclosystem.medicamentos.service;

import com.pachoclosystem.medicamentos.dto.MedicamentoResponse;
import com.pachoclosystem.medicamentos.exception.ConflictoException;
import com.pachoclosystem.medicamentos.exception.SolicitudInvalidaException;
import com.pachoclosystem.medicamentos.model.Medicamento;
import org.springframework.stereotype.Service;

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
 *   <li>Dos peticiones simultáneas con la misma clave se serializan: una hace
 *       la salida y la otra recibe su respuesta.</li>
 *   <li>Solo se guardan las salidas que salen bien. Una que falla (stock
 *       insuficiente, vencido, inexistente) no ha cambiado nada, así que al
 *       reintentarla se vuelve a evaluar: puede salir bien si entre tanto
 *       entró stock, o volver a fallar.</li>
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
    private final AlmacenIdempotencia almacen;

    public SalidasIdempotentesService(MedicamentoService medicamentos, AlmacenIdempotencia almacen) {
        this.medicamentos = medicamentos;
        this.almacen = almacen;
    }

    public Resultado registrarSalida(String idMedicamento, int cantidad, String clave) {
        if (clave == null || !PATRON_CLAVE.matcher(clave).matches()) {
            throw new SolicitudInvalidaException("La cabecera Idempotency-Key debe tener entre 16 y 100 "
                    + "caracteres: letras sin tilde, dígitos, guion o guion bajo (por ejemplo, un UUID).");
        }
        synchronized (almacen.cerrojo(clave)) {
            Optional<AlmacenIdempotencia.Salida> previa = almacen.buscar(clave);
            if (previa.isPresent()) {
                AlmacenIdempotencia.Salida salida = previa.get();
                if (!salida.idMedicamento().equals(idMedicamento) || salida.cantidad() != cantidad) {
                    throw new ConflictoException("La clave de idempotencia ya se usó para otra salida de "
                            + "stock con otro medicamento o cantidad.");
                }
                return new Resultado(salida.respuesta(), true);
            }
            // Si la salida falla, la excepción sale sin guardar nada: un reintento se reevalúa.
            Medicamento medicamento = medicamentos.registrarSalida(idMedicamento, cantidad);
            MedicamentoResponse respuesta = MedicamentoResponse.from(medicamento, medicamentos.hoy());
            almacen.guardar(clave, idMedicamento, cantidad, respuesta);
            return new Resultado(respuesta, false);
        }
    }
}
