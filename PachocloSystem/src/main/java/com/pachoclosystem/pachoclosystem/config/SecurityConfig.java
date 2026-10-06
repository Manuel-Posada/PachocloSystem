package com.pachoclosystem.pachoclosystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Cadena de filtros de seguridad stateless con tokens JWT (HS256) y resource
 * server de OAuth2 (Nimbus).
 *
 * <p>Solo {@code POST /api/auth/login} es público; el resto de {@code /api/**}
 * exige estar autenticado (aún no hay autorización por rol). Todo el estado de
 * autenticación vive en el token <em>bearer</em>: sin sesiones HTTP, sin login
 * por formulario, sin Basic auth.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    JwtEncoder jwtEncoder(ClaveFirmaJwt firma) {
        return NimbusJwtEncoder.withSecretKey(firma.clave())
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(ClaveFirmaJwt firma) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(firma.clave())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // Valida firma HS256 (el decodificador restringido a HS256 rechaza "none"
        // y cualquier otro algoritmo), expiración/emitido-y-no-válido y el emisor
        // configurado (app.jwt.emisor / pachoclosystem).
        decodificador.setJwtValidator(JwtValidators.createDefaultWithIssuer(firma.emisor()));
        return decodificador;
    }

    @Bean
    SecurityFilterChain filtroSeguridad(HttpSecurity http,
                                        JwtDecoder decoder,
                                        Converter<Jwt, ? extends AbstractAuthenticationToken> conversor,
                                        AuthenticationEntryPoint puntoDeEntrada,
                                        AccessDeniedHandler accesoDenegado) throws Exception {
        http
                // API stateless protegida con token bearer: no hay cookies ni sesiones,
                // así que no hay estado que un ataque CSRF pueda aprovechar; se desactiva.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sesiones -> sesiones.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(login -> login.disable())
                .httpBasic(basica -> basica.disable())
                .logout(cierre -> cierre.disable())
                .authorizeHttpRequests(peticiones -> peticiones
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .anyRequest().authenticated())
                // 401 y 403 con el cuerpo de error uniforme de la API
                // (ErrorResponse, sin trazas ni detalles internos).
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(puntoDeEntrada)
                        .accessDeniedHandler(accesoDenegado))
                .oauth2ResourceServer(recurso -> recurso.jwt(jwt -> jwt
                        .decoder(decoder)
                        .jwtAuthenticationConverter(conversor)));
        return http.build();
    }
}