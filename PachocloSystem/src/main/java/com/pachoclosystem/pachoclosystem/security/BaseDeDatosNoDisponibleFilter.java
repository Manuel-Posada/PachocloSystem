package com.pachoclosystem.pachoclosystem.security;

import com.pachoclosystem.pachoclosystem.dto.ErrorResponse;
import com.pachoclosystem.pachoclosystem.exception.BaseDeDatosNoDisponible;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 503 uniforme si la base de datos falla <strong>durante la autenticación</strong>:
 * {@link JwtUsuarioAuthenticationConverter} relee el usuario en cada petición y,
 * sin este filtro, ese fallo acabaría en un 500 sin el cuerpo de la API. Nunca
 * es 401: el frontend cerraría la sesión de un usuario cuyo token sigue siendo
 * válido. Los fallos dentro de los controladores los responde
 * {@code GlobalExceptionHandler} con el mismo 503.
 *
 * <p>Va en la cadena de seguridad antes del filtro del token bearer; no es un
 * bean para que Spring Boot no lo registre además como filtro del servlet.</p>
 */
public class BaseDeDatosNoDisponibleFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(BaseDeDatosNoDisponibleFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } catch (RuntimeException | ServletException fallo) {
            if (!BaseDeDatosNoDisponible.es(fallo) || response.isCommitted()) {
                throw fallo;
            }
            LOG.warn("Base de datos no disponible al autenticar la petición: {}", fallo.getMessage());
            response.resetBuffer();
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            ErrorResponse error = new ErrorResponse(HttpStatus.SERVICE_UNAVAILABLE.value(),
                    HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(), List.of(BaseDeDatosNoDisponible.MENSAJE));
            response.getWriter().write(EscritorErrorApi.jsonDe(error));
        }
    }
}
