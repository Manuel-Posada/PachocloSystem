package com.pachoclosystem.pachoclosystem.security;

import com.pachoclosystem.pachoclosystem.config.ClaveFirmaJwt;
import com.pachoclosystem.pachoclosystem.model.Usuario;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Emite los tokens JWT (HS256) firmados con la clave configurada.
 *
 * <p>Claims: {@code sub}=idUsuario, {@code username}, {@code rol},
 * {@code idTrabajador} (solo si el usuario está vinculado a un trabajador),
 * {@code ver} (versión de token: se incrementa en cada cambio de contraseña,
 * de modo que el token deja de valer de inmediato), {@code iss}, {@code iat}
 * y {@code exp}. Ni la contraseña ni el hash se incluyen jamás en un token.</p>
 */
@Service
public class JwtTokenService {

    private final JwtEncoder codificador;
    private final ClaveFirmaJwt firma;

    public JwtTokenService(JwtEncoder codificador, ClaveFirmaJwt firma) {
        this.codificador = codificador;
        this.firma = firma;
    }

    public String generarToken(Usuario usuario) {
        Instant ahora = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .subject(usuario.getIdUsuario())
                .claim("username", usuario.getUsername())
                .claim("rol", usuario.getRol().name())
                .claim("ver", usuario.getVersionToken())
                .issuer(firma.emisor())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(firma.expiracion()));
        if (usuario.getIdTrabajador() != null) {
            claims.claim("idTrabajador", usuario.getIdTrabajador());
        }
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        return codificador.encode(JwtEncoderParameters.from(cabecera, claims.build())).getTokenValue();
    }

    /** Segundos de validez del token (se usa en la respuesta del login). */
    public long expiraEnSegundos() {
        return firma.expiracion().toSeconds();
    }
}