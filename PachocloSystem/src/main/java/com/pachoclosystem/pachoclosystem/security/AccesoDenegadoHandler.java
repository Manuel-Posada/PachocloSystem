package com.pachoclosystem.pachoclosystem.security;

import com.pachoclosystem.pachoclosystem.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Respuesta {@code 403} con el {@link ErrorResponse} uniforme de la API cuando
 * un usuario autenticado no tiene permiso para la operación.
 *
 * <p>Si el usuario autenticado aún tiene la contraseña pendiente de cambio (su
 * única autoridad es {@value JwtUsuarioAuthenticationConverter#AUTORIDAD_CAMBIO_PASSWORD_PENDIENTE}),
 * el mensaje le indica el paso que debe dar antes de operar; en el resto de los
 * casos se usa el mensaje genérico de permisos.</p>
 */
@Component
public class AccesoDenegadoHandler implements AccessDeniedHandler {

    private static final String MENSAJE = "No tiene permisos para realizar esta operación.";
    private static final String MENSAJE_CAMBIO_PASSWORD =
            "Debe cambiar su contraseña antes de continuar.";

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException excepcion) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ErrorResponse error = new ErrorResponse(HttpServletResponse.SC_FORBIDDEN,
                "Forbidden", List.of(mensajeSegunElEstadoDeLaAutenticacion()));
        response.getWriter().write(EscritorErrorApi.jsonDe(error));
    }

    private String mensajeSegunElEstadoDeLaAutenticacion() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.getAuthorities().stream().anyMatch(authoridad ->
                JwtUsuarioAuthenticationConverter.AUTORIDAD_CAMBIO_PASSWORD_PENDIENTE
                        .equals(authoridad.getAuthority()))) {
            return MENSAJE_CAMBIO_PASSWORD;
        }
        return MENSAJE;
    }
}