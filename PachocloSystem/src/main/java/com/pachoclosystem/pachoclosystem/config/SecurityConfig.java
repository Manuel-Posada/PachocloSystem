package com.pachoclosystem.pachoclosystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Cadena de filtros de seguridad stateless con tokens JWT (HS256) y resource
 * server de OAuth2 (Nimbus).
 *
 * <p>Solo {@code POST /api/auth/login} es público. El resto de los endpoints
 * reales exigen autenticación con un rol real (ADMIN, DOCTOR, ENFERMERO): un
 * usuario cuya contraseña está pendiente de cambio solo recibe la autoridad
 * {@code CAMBIO_PASSWORD_PENDIENTE} y queda bloqueado (403) hasta cambiarla.
 * La lectura es para cualquier rol autenticado, la gestión de trabajadores y
 * la baja de pacientes son solo de ADMIN, el alta/edición de pacientes es solo
 * de DOCTOR y el alta de registros clínicos corresponde a DOCTOR o ENFERMERO
 * (el tipo se valida en el servicio).
 * Todo el estado de autenticación vive en el token <em>bearer</em>: sin
 * sesiones HTTP, sin login por formulario, sin Basic auth.</p>
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
                                        AccessDeniedHandler accesoDenegado,
                                        @Qualifier("corsConfigurationSource") CorsConfigurationSource corsFuente) throws Exception {
        http
                // API stateless protegida con token bearer: no hay cookies ni sesiones,
                // así que no hay estado que un ataque CSRF pueda aprovechar; se desactiva.
                .csrf(csrf -> csrf.disable())
                // CORS por orígenes explícitos (CorsConfiguracion): las preflight OPTIONS
                // de un origen permitido se resuelven aquí, antes de la autenticación, y
                // no exigen token. Con app.cors.origenes vacío no se emite ninguna
                // cabecera Access-Control-Allow-*.
                .cors(cors -> cors.configurationSource(corsFuente))
                .sessionManagement(sesiones -> sesiones.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(login -> login.disable())
                .httpBasic(basica -> basica.disable())
                .logout(cierre -> cierre.disable())
                .authorizeHttpRequests(peticiones -> peticiones
                        // Único endpoint público.
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()

                        // Identidad: cualquier usuario autenticado puede verse a sí mismo.
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()

                        // Cambio de contraseña propia: lo permite la identidad
                        // autenticada (incluido quien aún debe cambiarla tras un
                        // alta o un reset temporal).
                        .requestMatchers(HttpMethod.POST, "/api/auth/password").authenticated()
                        // no basta con estar autenticado: quien aún debe cambiar su contraseña
                        // (única autoridad CAMBIO_PASSWORD_PENDIENTE) recibe 403 con el aviso
                        // correspondiente hasta completar el cambio.

                        // Pacientes: lectura (listado, búsqueda, detalle e historial)
                        // para cualquier rol autenticado.
                        .requestMatchers(HttpMethod.GET, "/api/pacientes", "/api/pacientes/**")
                        .hasAnyRole("ADMIN", "DOCTOR", "ENFERMERO")
                        // Alta y edición de pacientes: solo DOCTOR.
                        .requestMatchers(HttpMethod.POST, "/api/pacientes").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.PUT, "/api/pacientes/**").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/pacientes/**").hasRole("DOCTOR")
                        // Baja (soft delete): solo ADMIN.
                        .requestMatchers(HttpMethod.DELETE, "/api/pacientes/**").hasRole("ADMIN")

                        // Historial: lectura para todos los roles; alta de registro para
                        // DOCTOR y ENFERMERO (el tipo permitido por rol se valida en el
                        // servicio).
                        .requestMatchers(HttpMethod.GET, "/api/historial")
                        .hasAnyRole("ADMIN", "DOCTOR", "ENFERMERO")
                        .requestMatchers(HttpMethod.GET, "/api/pacientes/*/historial")
                        .hasAnyRole("ADMIN", "DOCTOR", "ENFERMERO")
                        .requestMatchers(HttpMethod.POST, "/api/pacientes/*/historial")
                        .hasAnyRole("DOCTOR", "ENFERMERO")

                        // Trabajadores: lectura para todos los roles; alta/edición/baja
                        // solo ADMIN.
                        .requestMatchers(HttpMethod.GET, "/api/trabajadores", "/api/trabajadores/**")
                        .hasAnyRole("ADMIN", "DOCTOR", "ENFERMERO")
                        .requestMatchers(HttpMethod.POST, "/api/trabajadores").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/trabajadores/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/trabajadores/**").hasRole("ADMIN")

                        // Usuarios: toda la gestión es solo de ADMIN.
                        .requestMatchers("/api/usuarios", "/api/usuarios/**").hasRole("ADMIN")

                        // Ningún endpoint real depende de esta red final: se conserva
                        // para que las rutas inexistentes sigan dando 401 sin token y
                        // 404 (uniforme) con token.
                        .anyRequest().authenticated())
                // 401 y 403 con el cuerpo de error uniforme de la API
                // (ErrorResponse, sin trazas ni detalles internos).
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(puntoDeEntrada)
                        .accessDeniedHandler(accesoDenegado))
                .oauth2ResourceServer(recurso -> recurso
                        .authenticationEntryPoint(puntoDeEntrada)
                        .accessDeniedHandler(accesoDenegado)
                        .jwt(jwt -> jwt
                                .decoder(decoder)
                                .jwtAuthenticationConverter(conversor)));
        return http.build();
    }
}