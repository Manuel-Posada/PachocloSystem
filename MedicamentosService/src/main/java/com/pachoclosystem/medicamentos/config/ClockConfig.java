package com.pachoclosystem.medicamentos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.ZoneId;

/**
 * Reloj del sistema como bean, en la zona horaria oficial de la aplicación
 * ({@code app.zona-horaria} / variable {@code APP_ZONA_HORARIA}, por defecto
 * {@code America/Bogota}). El servicio calcula "hoy" (vencidos, por vencer) a
 * partir de él: no depende de la zona del servidor, y las pruebas pueden fijar la
 * fecha y obtener resultados deterministas. Una zona inválida impide arrancar.
 */
@Configuration
public class ClockConfig {

    public static final String PROP_ZONA_HORARIA = "app.zona-horaria";

    @Bean
    public Clock clock(@Value("${" + PROP_ZONA_HORARIA + ":America/Bogota}") String zona) {
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
