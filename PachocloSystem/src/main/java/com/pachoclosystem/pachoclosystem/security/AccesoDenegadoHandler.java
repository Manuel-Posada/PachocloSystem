package com.pachoclosystem.pachoclosystem.security;

import com.pachoclosystem.pachoclosystem.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Respuesta {@code 403} con el {@link ErrorResponse} uniforme de la API cuando
 * un usuario autenticado no tiene permiso para la operación. Aún no hay reglas
 * por rol en la API, pero el manejador queda fijado para cuando las haya.
 */
@Component
public class AccesoDenegadoHandler implements AccessDeniedHandler {

    private static final String MENSAJE = "No tiene permisos para realizar esta operación.";

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException excepcion) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ErrorResponse error = new ErrorResponse(HttpServletResponse.SC_FORBIDDEN,
                "Forbidden", List.of(MENSAJE));
        response.getWriter().write(EscritorErrorApi.jsonDe(error));
    }
}