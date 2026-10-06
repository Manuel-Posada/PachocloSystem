package com.pachoclosystem.medicamentos.config;

import com.pachoclosystem.medicamentos.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Autenticación entre servicios: si {@code medicamentos.api-key} tiene valor, toda
 * petición a {@code /api/**} debe traer esa clave en la cabecera {@code X-Api-Key}
 * (la envía PachocloSystem); si falta o no coincide, responde 401 con el
 * {@link ErrorResponse} de la API.
 *
 * <p>Con la clave vacía el filtro no actúa (modo desarrollo, un único WARN al
 * arrancar). La clave nunca se escribe en el log.</p>
 */
@Component
public class ClaveServicioFilter extends OncePerRequestFilter {

    public static final String CABECERA = "X-Api-Key";

    private static final Logger LOG = LoggerFactory.getLogger(ClaveServicioFilter.class);
    private static final String MENSAJE = "Falta la clave de servicio o no es válida.";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final byte[] clave;

    public ClaveServicioFilter(@Value("${medicamentos.api-key:}") String clave) {
        boolean vacia = clave == null || clave.isBlank();
        this.clave = vacia ? null : clave.getBytes(StandardCharsets.UTF_8);
        if (vacia) {
            LOG.warn("MEDICAMENTOS_API_KEY vacía: la API no exige clave de servicio (solo para desarrollo).");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return clave == null || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain cadena) throws ServletException, IOException {
        String recibida = request.getHeader(CABECERA);
        if (recibida != null && MessageDigest.isEqual(clave, recibida.getBytes(StandardCharsets.UTF_8))) {
            cadena.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(JSON.writeValueAsString(
                new ErrorResponse(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", List.of(MENSAJE))));
    }
}
