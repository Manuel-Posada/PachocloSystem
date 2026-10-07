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
 * {@value #CLAIM_VERSION} (versión de token con la que se emitió, ver
 * {@link Usuario.Credenciales#version()}), {@code iss}, {@code iat} y
 * {@code exp}. Ni la contraseña ni el hash se incluyen jamás en un token.</p>
 */
@Service
public class JwtTokenService {

    /** Versión de token del usuario al emitirlo; cambia con la contraseña y al desactivar. */
    public static final String CLAIM_VERSION = "ver";

    private final JwtEncoder codificador;
    private final ClaveFirmaJwt firma;

    public JwtTokenService(JwtEncoder codificador, ClaveFirmaJwt firma) {
        this.codificador = codificador;
        this.firma = firma;
    }

    /** Token con las credenciales actuales del usuario. */
    public String generarToken(Usuario usuario) {
        return generarToken(usuario, usuario.getCredenciales());
    }

    /**
     * Token ligado a unas credenciales concretas: el login pasa las mismas que
     * usó para comprobar la contraseña. Si después cambia la versión (cambio de
     * contraseña o desactivación), el token deja de valer.
     */
    public String generarToken(Usuario usuario, Usuario.Credenciales credenciales) {
        Instant ahora = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .subject(usuario.getIdUsuario())
                .claim("username", usuario.getUsername())
                .claim("rol", usuario.getRol().name())
                .claim(CLAIM_VERSION, credenciales.version())
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
