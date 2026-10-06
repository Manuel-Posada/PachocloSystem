package com.pachoclosystem.pachoclosystem.security;

import com.pachoclosystem.pachoclosystem.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Punto de entrada de la autenticación: cuando una petición protegida llega sin
 * token válido (ausente, malformado, expirado o con firma inválida) responde
 * {@code 401} con el {@link ErrorResponse} uniforme de la API y la cabecera
 * {@code WWW-Authenticate: Bearer}. Nunca se exponen detalles internos de
 * Nimbus ni nombres de clases.
 */
@Component
public class AutenticacionEntryPoint implements AuthenticationEntryPoint {

    private static final String MENSAJE = "Debe autenticarse para acceder a este recurso.";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException excepcion) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ErrorResponse error = new ErrorResponse(HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized", List.of(MENSAJE));
        response.getWriter().write(EscritorErrorApi.jsonDe(error));
    }
}