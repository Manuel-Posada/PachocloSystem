package com.pachoclosystem.pachoclosystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Reloj de la aplicación como bean inyectable: {@code LimitadorIntentosLogin}
 * deriva sus ventanas de bloqueo de este reloj, de modo que en los tests se
 * puede sustituir por un reloj controlado ({@code Clock.fixed} o un reloj con
 * instante ajustable) sin dormir el hilo de ejecución.
 */
@Configuration
public class RelojConfig {

    @Bean
    public Clock reloj() {
        return Clock.systemUTC();
    }
}