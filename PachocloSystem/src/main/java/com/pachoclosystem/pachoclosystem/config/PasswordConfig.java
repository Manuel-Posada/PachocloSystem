package com.pachoclosystem.pachoclosystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuración del cifrado de contraseñas.
 *
 * <p>Usa <strong>solo</strong> {@code spring-security-crypto} (BCrypt): no hay
 * aquí ninguna configuración de Spring Security web (ni filtros ni cadenas de
 * seguridad), que llegará en la Fase 2.</p>
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
