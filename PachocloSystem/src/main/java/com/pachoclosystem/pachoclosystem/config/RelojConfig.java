package com.pachoclosystem.pachoclosystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.ZoneId;

/**
 * Reloj de la aplicación como bean inyectable, en la zona horaria oficial
 * ({@code app.zona-horaria} / variable {@code APP_ZONA_HORARIA}, por defecto
 * {@code America/Bogota}).
 *
 * <ul>
 *   <li>La fecha de los registros clínicos sale de este reloj, así que no depende
 *       de la zona del servidor (un contenedor en la nube suele estar en UTC).</li>
 *   <li>{@code LimitadorIntentosLogin} y la idempotencia usan solo instantes, que
 *       no dependen de la zona; en los tests se sustituye por un reloj controlado
 *       ({@code Clock.fixed} o un reloj con instante ajustable).</li>
 *   <li>Una zona inválida impide arrancar, con un mensaje claro.</li>
 * </ul>
 */
@Configuration
public class RelojConfig {

    public static final String PROP_ZONA_HORARIA = "app.zona-horaria";

    @Bean
    public Clock reloj(@Value("${" + PROP_ZONA_HORARIA + ":America/Bogota}") String zona) {
        return Clock.system(zonaHoraria(zona));
    }

    /** La zona configurada; si no es válida, {@link IllegalStateException}. */
    static ZoneId zonaHoraria(String zona) {
        try {
            return ZoneId.of(zona == null ? "" : zona.trim());
        } catch (DateTimeException invalida) {
            throw new IllegalStateException("El valor de " + PROP_ZONA_HORARIA
                    + " (variable APP_ZONA_HORARIA) no es una zona horaria válida: '" + zona
                    + "'. Use un identificador como America/Bogota.", invalida);
        }
    }
}
