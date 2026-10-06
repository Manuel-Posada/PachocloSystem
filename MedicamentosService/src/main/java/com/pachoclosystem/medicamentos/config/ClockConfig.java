package com.pachoclosystem.medicamentos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Reloj del sistema como bean: el servicio calcula "hoy" a partir de él, de modo
 * que las pruebas pueden fijar la fecha y obtener resultados deterministas.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
