package com.pachoclosystem.pachoclosystem.service;

import com.pachoclosystem.pachoclosystem.dto.RegistroRequest;
import com.pachoclosystem.pachoclosystem.dto.RegistroResponse;
import com.pachoclosystem.pachoclosystem.exception.AccesoDenegadoException;
import com.pachoclosystem.pachoclosystem.exception.ConflictoException;
import com.pachoclosystem.pachoclosystem.exception.NotFoundException;
import com.pachoclosystem.pachoclosystem.exception.SolicitudInvalidaException;
import com.pachoclosystem.pachoclosystem.model.Usuario;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Creación de registros del historial con clave de idempotencia
 * ({@code Idempotency-Key}), para cualquier tipo de registro: repetir la
 * petición (doble envío, reintento tras un error de red) no crea un segundo
 * registro ni descuenta dos veces.
 *
 * <ul>
 *   <li>Misma clave, mismo usuario, mismo paciente y mismo cuerpo: se devuelve el
 *       registro ya creado sin repetir nada.</li>
 *   <li>Misma clave con otro usuario, otro paciente u otro cuerpo: 409, con un
 *       mensaje que no dice nada del registro original.</li>
 *   <li>Las peticiones con la misma clave van de una en una: dos simultáneas
 *       crean un solo registro.</li>
 *   <li>Con descuento de stock, la misma clave se reenvía a la salida de
 *       MedicamentosService, que tampoco descuenta dos veces.</li>
 * </ul>
 *
 * <p>Qué queda guardado con la clave según cómo acabe la petición:</p>
 * <ul>
 *   <li><b>Registro creado:</b> la clave con el registro (para devolverlo), en
 *       la misma transacción que el registro: o se guardan los dos o ninguno.</li>
 *   <li><b>Fallo que no cambió nada</b> (400, 403, 404 o 409, ya sean de aquí o
 *       de MedicamentosService): nada. La clave no se consume y un reintento se
 *       vuelve a evaluar, como en las salidas de MedicamentosService.</li>
 *   <li><b>Fallo con resultado incierto</b> (salida sin confirmar, respuesta
 *       inesperada de MedicamentosService o error al guardar después de una
 *       salida hecha): la clave queda ligada a esta petición sin registro. Repetir
 *       la misma petición vuelve a intentarlo (la salida no se descuenta dos
 *       veces); cualquier otra combinación recibe 409.</li>
 * </ul>
 */
@Service
public class HistorialIdempotenteService {

    /** Letras, dígitos, guion y guion bajo; de 16 a 100 (un UUID tiene 36). Igual que MedicamentosService. */
    private static final Pattern PATRON_CLAVE = Pattern.compile("^[A-Za-z0-9_-]{16,100}$");
    private static final String MENSAJE_CONFLICTO =
            "La clave de idempotencia ya se usó para otra petición.";
    private static final ObjectMapper JSON = new ObjectMapper();

    /** Registro creado y si es la repetición de uno ya creado con esa clave. */
    public record Resultado(RegistroResponse registro, boolean repetido) {
    }

    private final HistorialClinicoService historial;
    private final AlmacenIdempotenciaHistorial almacen;

    public HistorialIdempotenteService(HistorialClinicoService historial, AlmacenIdempotenciaHistorial almacen) {
        this.historial = historial;
        this.almacen = almacen;
    }

    public Resultado agregarRegistro(Usuario usuario, String idPaciente, RegistroRequest request, String clave) {
        if (clave == null || !PATRON_CLAVE.matcher(clave).matches()) {
            throw new SolicitudInvalidaException("La cabecera Idempotency-Key debe tener entre 16 y 100 "
                    + "caracteres: letras sin tilde, dígitos, guion o guion bajo (por ejemplo, un UUID).");
        }
        String idUsuario = usuario.getIdUsuario();
        String huella = huella(request);
        return almacen.conClave(clave, () -> {
            Optional<AlmacenIdempotenciaHistorial.Uso> previo = almacen.buscar(clave);
            if (previo.isPresent()) {
                AlmacenIdempotenciaHistorial.Uso uso = previo.get();
                if (!uso.esDe(idUsuario, idPaciente, huella)) {
                    throw new ConflictoException(MENSAJE_CONFLICTO);
                }
                if (uso.registro() != null) {
                    return new Resultado(uso.registro(), true);
                }
                // Intento anterior sin confirmar: se repite con la misma clave.
            }
            try {
                // La clave completada se guarda en la misma transacción que el registro.
                RegistroResponse creado = historial.agregarRegistroComo(usuario, idPaciente, request.idAutor(),
                        request.tipo(), request.contenido(), request.signosVitales(), request.idMedicamento(),
                        request.cantidad(), clave,
                        registro -> almacen.guardar(clave, idUsuario, idPaciente, huella, registro));
                return new Resultado(creado, false);
            } catch (SolicitudInvalidaException | AccesoDenegadoException | NotFoundException
                     | ConflictoException sinCambios) {
                // Ninguna de estas sale después de una salida hecha: no se guarda nada.
                throw sinCambios;
            } catch (RuntimeException incierto) {
                if (previo.isEmpty()) {
                    try {
                        almacen.guardar(clave, idUsuario, idPaciente, huella, null);
                    } catch (RuntimeException sinGuardar) {
                        // Sin base de datos no se puede ligar la clave: sale el error original.
                        incierto.addSuppressed(sinGuardar);
                    }
                }
                throw incierto;
            }
        });
    }

    /**
     * SHA-256 del cuerpo ya interpretado (no de los bytes): los espacios o el
     * orden de los campos del JSON no lo cambian; cualquier valor distinto, sí.
     */
    static String huella(RegistroRequest request) {
        try {
            byte[] cuerpo = JSON.writeValueAsString(request).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(cuerpo));
        } catch (NoSuchAlgorithmException imposible) {
            throw new IllegalStateException("SHA-256 no disponible.", imposible);
        }
    }
}
