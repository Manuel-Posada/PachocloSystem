package com.pachoclosystem.pachoclosystem.security;

import com.pachoclosystem.pachoclosystem.model.Usuario;
import com.pachoclosystem.pachoclosystem.repository.IUsuarioRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Convierte el JWT ya validado en la autenticación de la petición, releyendo el
 * usuario del repositorio en <strong>cada petición</strong>:
 *
 * <ul>
 *   <li>El {@code sub} del token es el {@code idUsuario}.</li>
 *   <li>Si el usuario ya no existe o está {@code activo=false}, el token se
 *       rechaza (401): un usuario desactivado pierde su acceso de inmediato.</li>
 *   <li>Las autoridades ({@code ROLE_ADMIN}, {@code ROLE_DOCTOR},
 *       {@code ROLE_ENFERMERO}) salen del repositorio, no de los claims del
 *       token, para que ningún dato del token tenga más peso que el estado real.</li>
 * </ul>
 */
@Component
public class JwtUsuarioAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final IUsuarioRepository repositorio;

    public JwtUsuarioAuthenticationConverter(IUsuarioRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Usuario usuario = repositorio.buscarPorId(jwt.getSubject());
        if (usuario == null || !usuario.isActivo()) {
            // Nunca se filtra la causa (usuario inexistente vs inactivo) ni el id.
            throw new BadCredentialsException("Usuario no válido para el token.");
        }
        List<GrantedAuthority> autoridades = List.of(
                new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()));
        // El nombre de la autenticación es el username (en minúsculas), tal como
        // se guarda en el sistema, para p. ej. GET /api/auth/me.
        return new JwtAuthenticationToken(jwt, autoridades, usuario.getUsername());
    }
}