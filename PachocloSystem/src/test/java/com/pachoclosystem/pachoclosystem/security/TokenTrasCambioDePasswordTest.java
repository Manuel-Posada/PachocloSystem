package com.pachoclosystem.pachoclosystem.security;

import com.pachoclosystem.pachoclosystem.model.Rol;
import com.pachoclosystem.pachoclosystem.model.Usuario;
import com.pachoclosystem.pachoclosystem.repository.UsuarioRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * El conversor del JWT rechaza los tokens emitidos con una contraseña que ya
 * se restableció, comparando la marca de credenciales del token con la actual.
 */
class TokenTrasCambioDePasswordTest {

    /** Un instante con milisegundos, para probar el borde del mismo segundo. */
    private static final Instant CAMBIO = Instant.parse("2026-10-07T10:00:00.700Z");

    private UsuarioRepositoryImpl repositorio;
    private JwtUsuarioAuthenticationConverter conversor;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        repositorio = new UsuarioRepositoryImpl();
        conversor = new JwtUsuarioAuthenticationConverter(repositorio);
        usuario = new Usuario("USR-0001", "ana.torres", "$2a$10$hash-inicial", Rol.ADMIN, null);
        repositorio.guardar(usuario);
    }

    @Test
    void unTokenConLaMarcaActualSeAcepta() {
        Jwt token = token(usuario.getCredenciales().marca(), Instant.parse("2026-10-07T09:00:00Z"));

        assertThat(conversor.convert(token).getName()).isEqualTo("ana.torres");
    }

    @Test
    void unTokenSinClaimDeCredencialesValeMientrasNoSeRestablezcaLaPassword() {
        Jwt sinClaim = Jwt.withTokenValue("t").header("alg", "HS256")
                .subject("USR-0001").issuedAt(Instant.parse("2026-10-07T09:00:00Z")).build();

        assertThat(conversor.convert(sinClaim).getName()).isEqualTo("ana.torres");

        usuario.cambiarPasswordHash("$2a$10$hash-nuevo", CAMBIO);
        assertThatExceptionOfType(BadCredentialsException.class)
                .isThrownBy(() -> conversor.convert(sinClaim));
    }

    @Test
    void enElMismoSegundoDelCambioSeRechazaElTokenAnteriorYSeAceptaElNuevo() {
        // Emitido a las 10:00:00.200 con la contraseña anterior: su iat (solo segundos)
        // es el mismo que el de un token emitido a las 10:00:00.900 con la nueva.
        Instant mismoSegundo = Instant.parse("2026-10-07T10:00:00Z");
        Jwt anterior = token(usuario.getCredenciales().marca(), mismoSegundo);

        usuario.cambiarPasswordHash("$2a$10$hash-nuevo", CAMBIO);
        Jwt nuevo = token(usuario.getCredenciales().marca(), mismoSegundo);

        assertThatExceptionOfType(BadCredentialsException.class)
                .isThrownBy(() -> conversor.convert(anterior));
        assertThat(conversor.convert(nuevo).getName()).isEqualTo("ana.torres");
    }

    @Test
    void dosCambiosEnElMismoMilisegundoDejanMarcasDistintasYSoloValeLaUltima() {
        usuario.cambiarPasswordHash("$2a$10$hash-dos", CAMBIO);
        long primera = usuario.getCredenciales().marca();
        usuario.cambiarPasswordHash("$2a$10$hash-tres", CAMBIO);
        long segunda = usuario.getCredenciales().marca();

        assertThat(segunda).isGreaterThan(primera);
        assertThatExceptionOfType(BadCredentialsException.class)
                .isThrownBy(() -> conversor.convert(token(primera, CAMBIO)));
        assertThat(conversor.convert(token(segunda, CAMBIO)).getName()).isEqualTo("ana.torres");
    }

    @Test
    void unaMarcaQueNoEsUnNumeroSeRechaza() {
        Jwt manipulado = Jwt.withTokenValue("t").header("alg", "HS256").subject("USR-0001")
                .issuedAt(CAMBIO).claim(JwtTokenService.CLAIM_CREDENCIALES, "0").build();

        assertThatExceptionOfType(BadCredentialsException.class)
                .isThrownBy(() -> conversor.convert(manipulado));
    }

    private static Jwt token(long marca, Instant emitido) {
        return Jwt.withTokenValue("t").header("alg", "HS256")
                .subject("USR-0001")
                .issuedAt(emitido)
                .claim(JwtTokenService.CLAIM_CREDENCIALES, marca)
                .build();
    }
}
