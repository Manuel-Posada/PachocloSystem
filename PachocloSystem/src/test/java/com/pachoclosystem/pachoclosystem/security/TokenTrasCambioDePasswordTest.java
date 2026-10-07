package com.pachoclosystem.pachoclosystem.security;

import com.pachoclosystem.pachoclosystem.model.Rol;
import com.pachoclosystem.pachoclosystem.model.Usuario;
import com.pachoclosystem.pachoclosystem.repository.UsuarioRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * El conversor del JWT rechaza los tokens cuya versión ya no es la del
 * usuario: tras cambiar la contraseña y tras desactivarlo (aunque se reactive).
 */
class TokenTrasCambioDePasswordTest {

    private static final Instant EMITIDO = Instant.parse("2026-10-07T10:00:00Z");

    private UsuarioRepositoryEnMemoria repositorio;
    private JwtUsuarioAuthenticationConverter conversor;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        repositorio = new UsuarioRepositoryEnMemoria();
        conversor = new JwtUsuarioAuthenticationConverter(repositorio);
        usuario = new Usuario("USR-0001", "ana.torres", "$2a$10$hash-inicial", Rol.ADMIN, null);
        repositorio.guardar(usuario);
    }

    @Test
    void unTokenConLaVersionActualSeAcepta() {
        assertThat(conversor.convert(token(usuario.getVersionToken())).getName()).isEqualTo("ana.torres");
    }

    @Test
    void unTokenSinClaimDeVersionSeRechaza() {
        Jwt sinClaim = Jwt.withTokenValue("t").header("alg", "HS256")
                .subject("USR-0001").issuedAt(EMITIDO).build();

        assertThatExceptionOfType(BadCredentialsException.class)
                .isThrownBy(() -> conversor.convert(sinClaim));
    }

    @Test
    void trasCambiarLaPasswordSeRechazaElTokenAnteriorYSeAceptaElNuevo() {
        Jwt anterior = token(usuario.getVersionToken());

        usuario.cambiarPassword("$2a$10$hash-nuevo", false);
        Jwt nuevo = token(usuario.getVersionToken());

        assertThatExceptionOfType(BadCredentialsException.class)
                .isThrownBy(() -> conversor.convert(anterior));
        assertThat(conversor.convert(nuevo).getName()).isEqualTo("ana.torres");
    }

    @Test
    void desactivarYReactivarNoResucitaElTokenAnterior() {
        Jwt anterior = token(usuario.getVersionToken());

        usuario.desactivar();
        usuario.reactivar();

        assertThatExceptionOfType(BadCredentialsException.class)
                .isThrownBy(() -> conversor.convert(anterior));
        assertThat(conversor.convert(token(usuario.getVersionToken())).getName()).isEqualTo("ana.torres");
    }

    @Test
    void conLaPasswordPendienteSoloTieneLaAutoridadDeCambiarla() {
        usuario.cambiarPassword("$2a$10$hash-temporal", true);

        assertThat(conversor.convert(token(usuario.getVersionToken())).getAuthorities())
                .extracting(a -> a.getAuthority())
                .containsExactly(JwtUsuarioAuthenticationConverter.AUTORIDAD_CAMBIO_PASSWORD_PENDIENTE);
    }

    @Test
    void unaVersionQueNoEsUnNumeroSeRechaza() {
        Jwt manipulado = Jwt.withTokenValue("t").header("alg", "HS256").subject("USR-0001")
                .issuedAt(EMITIDO).claim(JwtTokenService.CLAIM_VERSION, "0").build();

        assertThatExceptionOfType(BadCredentialsException.class)
                .isThrownBy(() -> conversor.convert(manipulado));
    }

    private static Jwt token(long version) {
        return Jwt.withTokenValue("t").header("alg", "HS256")
                .subject("USR-0001")
                .issuedAt(EMITIDO)
                .claim(JwtTokenService.CLAIM_VERSION, version)
                .build();
    }
}
