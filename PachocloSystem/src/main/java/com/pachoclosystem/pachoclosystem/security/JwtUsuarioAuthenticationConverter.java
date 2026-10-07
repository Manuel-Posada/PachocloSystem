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
 *   <li>El claim {@code ver} (versión de token) debe coincidir con la versión
 *       actual del usuario: si falta o no coincide, el token se rechaza (401).
 *       Como la versión se incrementa en cada cambio de contraseña, un token
 *       emitido antes del cambio queda revocado de inmediato.</li>
 *   <li>Si el usuario ya no existe o está {@code activo=false}, el token se
 *       rechaza (401): un usuario desactivado pierde su acceso de inmediato.</li>
 *   <li>Las autoridades salen del repositorio, no de los claims del token.
 *       Con la contraseña pendiente de cambio el usuario recibe únicamente
 *       {@value #AUTORIDAD_CAMBIO_PASSWORD_PENDIENTE} y se le niega todo lo
 *       demás (403) hasta que la cambie. En caso contrario recibe su
 *       {@code ROLE_ADMIN}/{@code ROLE_DOCTOR}/{@code ROLE_ENFERMERO}.</li>
 * </ul>
 */
@Component
public class JwtUsuarioAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    /** Autoridad exclusiva del usuario que aún debe cambiar su contraseña. */
    public static final String AUTORIDAD_CAMBIO_PASSWORD_PENDIENTE = "CAMBIO_PASSWORD_PENDIENTE";

    private final IUsuarioRepository repositorio;

    public JwtUsuarioAuthenticationConverter(IUsuarioRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // Los claims numéricos llegan como Long desde Nimbus; se trata como
        // Number para comparar sin lanzar ClassCastException.
        Number versionEnToken = jwt.getClaim("ver");
        Usuario usuario = repositorio.buscarPorId(jwt.getSubject());
        if (usuario == null || !usuario.isActivo()
                || versionEnToken == null || versionEnToken.longValue() != usuario.getVersionToken()) {
            // Nunca se filtra la causa (usuario inexistente, inactivo o token con
            // versión obsoleta) ni el id.
            throw new BadCredentialsException("Usuario no válido para el token.");
        }
        List<GrantedAuthority> autoridades = usuario.isDebeCambiarPassword()
                ? List.of(new SimpleGrantedAuthority(AUTORIDAD_CAMBIO_PASSWORD_PENDIENTE))
                : List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()));
        // El nombre de la autenticación es el username (en minúsculas), tal como
        // se guarda en el sistema, para p. ej. GET /api/auth/me.
        return new JwtAuthenticationToken(jwt, autoridades, usuario.getUsername());
    }
}