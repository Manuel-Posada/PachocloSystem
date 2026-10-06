package com.pachoclosystem.pachoclosystem.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Arranque real de la aplicación sin {@code JWT_SECRET} (y sin password de admin
 * de entorno): el contexto levanta y {@code ClaveFirmaJwt} provee una clave
 * aleatoria de 32 bytes con la que el circuito encodificar/decodificar real de
 * la aplicación funciona de extremo a extremo.
 *
 * <p>El propio WARN del arranque (único y sin el secreto) se verifica a nivel
 * unitario en {@link JwtArranqueTest}: en el arranque real no se puede capturar
 * con un {@code ListAppender} estático porque Spring Boot reinicializa el
 * contexto de logging (logback {@code context.reset()}) justo al levantar la
 * aplicación y desmonta los appenders previos.</p>
 */
@SpringBootTest
class JwtSinSecretArranqueTest {

    @Autowired
    private ClaveFirmaJwt firma;

    @Autowired
    private JwtEncoder codificador;

    @Autowired
    private JwtDecoder decodificador;

    @DynamicPropertySource
    static void sinSecretNiPassword(DynamicPropertyRegistry registro) {
        registro.add(ClaveFirmaJwt.PROP_SECRET, () -> "");
        registro.add("app.admin.password", () -> "");
    }

    @Test
    void elArranqueSinJwtSecretLevantaConUnaClaveAleatoriaOperativa() {
        assertThat(firma.clave().getEncoded()).hasSize(32);
        assertThat(firma.clave().getAlgorithm()).isEqualTo("HmacSHA256");
        assertThat(firma.emisor()).isEqualTo("pachoclosystem");

        // Circuito real de la aplicación: un token firmado con la clave
        // generada (no configurada) se empieza y se decodifica correctamente.
        Instant ahora = Instant.now();
        String valor = codificador.encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(),
                        JwtClaimsSet.builder()
                                .subject("USR-00000")
                                .issuer("pachoclosystem")
                                .issuedAt(ahora)
                                .expiresAt(ahora.plusSeconds(600))
                                .build()))
                .getTokenValue();

        Jwt jwt = decodificador.decode(valor);
        assertThat(jwt.getSubject()).isEqualTo("USR-00000");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("pachoclosystem");
    }
}