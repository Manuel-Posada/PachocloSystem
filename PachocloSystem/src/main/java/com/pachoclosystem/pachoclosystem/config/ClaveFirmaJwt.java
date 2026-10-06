package com.pachoclosystem.pachoclosystem.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;

/**
 * Clave de firma de los tokens JWT (HS256) y configuración asociada.
 *
 * <p>Reglas del secreto ({@code app.jwt.secret} / variable {@code JWT_SECRET}):</p>
 * <ul>
 *   <li>Si está definido, se exige una longitud de al menos 32 bytes (UTF-8);
 *       si es más corto, la aplicación falla en el arranque con un mensaje
 *       claro (nunca se muestra el valor del secreto).</li>
 *   <li>Si no está definido, se genera una clave aleatoria de 32 bytes con
 *       {@link SecureRandom} y se emite <strong>un único WARN</strong> indicando
 *       que los tokens no sobrevivirán a un reinicio. El secreto jamás se
 *       escribe en el log.</li>
 * </ul>
 */
@Component
public class ClaveFirmaJwt {

    public static final String PROP_SECRET = "app.jwt.secret";
    public static final String PROP_EXPIRACION_MINUTOS = "app.jwt.expiracion-minutos";
    public static final String PROP_EMISOR = "app.jwt.emisor";

    /** Longitud mínima exigida a JWT_SECRET (32 bytes = 256 bits). */
    static final int LONGITUD_MINIMA_SECRETO_BYTES = 32;
    private static final String ALGORITMO_FIRMA = "HmacSHA256";

    private static final Logger LOG = LoggerFactory.getLogger(ClaveFirmaJwt.class);
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final SecretKey clave;
    private final String emisor;
    private final Duration expiracion;

    public ClaveFirmaJwt(Environment entorno) {
        String secreto = entorno.getProperty(PROP_SECRET, "");
        if (secreto == null || secreto.isBlank()) {
            byte[] aleatoria = new byte[LONGITUD_MINIMA_SECRETO_BYTES];
            ALEATORIO.nextBytes(aleatoria);
            this.clave = new SecretKeySpec(aleatoria, ALGORITMO_FIRMA);
            LOG.warn("No se ha definido {} (variable JWT_SECRET): se genera una clave de firma "
                            + "aleatoria para HS256. Los tokens no sobrevivirán a un reinicio "
                            + "(solo para desarrollo).",
                    PROP_SECRET);
        } else {
            byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < LONGITUD_MINIMA_SECRETO_BYTES) {
                // NUNCA se incluye el valor del secreto en el mensaje.
                throw new IllegalStateException("El valor de " + PROP_SECRET
                        + " (variable JWT_SECRET) debe tener al menos "
                        + LONGITUD_MINIMA_SECRETO_BYTES + " bytes codificados en UTF-8.");
            }
            this.clave = new SecretKeySpec(bytes, ALGORITMO_FIRMA);
        }
        this.emisor = entorno.getProperty(PROP_EMISOR, "pachoclosystem");
        this.expiracion = Duration.ofMinutes(
                Long.parseLong(entorno.getProperty(PROP_EXPIRACION_MINUTOS, "30")));
    }

    /** Clave de firma HS256 lista para {@code NimbusJwtEncoder}/{@code NimbusJwtDecoder}. */
    public SecretKey clave() {
        return clave;
    }

    public String emisor() {
        return emisor;
    }

    public Duration expiracion() {
        return expiracion;
    }
}