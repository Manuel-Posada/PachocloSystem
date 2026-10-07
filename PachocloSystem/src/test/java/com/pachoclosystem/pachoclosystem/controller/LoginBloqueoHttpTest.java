package com.pachoclosystem.pachoclosystem.controller;

import com.pachoclosystem.pachoclosystem.security.LimitadorIntentosLogin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bloqueo temporal del login por HTTP con la cadena real y un reloj controlado
 * (el {@code @TestConfiguration} reemplaza el bean {@code Clock} por uno
 * ajustable): 5 fallos -> el 6º intento responde 429 con el cuerpo uniforme y
 * {@code Retry-After}, el bloqueo no distingue usuarios inexistentes, un éxito
 * reinicia solo el contador del usuario, la IP se bloquea con fallos de
 * usuarios distintos, {@code X-Forwarded-For} se ignora, el 429 no extiende la
 * cuenta atrás y un 400 por body inválido no cuenta ni comprueba el bloqueo.
 */
class LoginBloqueoHttpTest extends MockMvcBaseTest {

    private static final String PASSWORD_ADMIN = "pwd-de-prueba-solo-tests-12345";
    private static final Instant INICIO = Instant.parse("2026-01-01T00:00:00Z");

    @TestConfiguration
    static class ConfiguracionRelojDePrueba {

        final RelojControlado reloj = new RelojControlado(INICIO);

        @Bean
        @Primary
        Clock relojDePrueba() {
            return reloj;
        }
    }

    @Autowired
    private ConfiguracionRelojDePrueba configuracion;

    @Autowired
    private LimitadorIntentosLogin limitadorIntentos;

    @BeforeEach
    void fijarReloj() {
        configuracion.reloj.fijar(INICIO);
    }

    @Test
    void cincoFallosBloqueanElSextoDa429UniformeYElMismoCuerpoQueUnInexistente() throws Exception {
        String ipReal = "198.51.100.10";

        // 5 fallos por contraseña errónea: cada uno 401.
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(conIp(loginCon("admin", "clave-inventada"), ipReal))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }

        // 6º intento: 429 con el cuerpo uniforme y Retry-After en segundos.
        MvcResult sextoAdmin = mockMvc.perform(conIp(loginCon("admin", "clave-inventada"), ipReal))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "900"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]")
                        .value("Demasiados intentos fallidos. Inténtelo de nuevo más tarde."))
                .andReturn();

        // Con la contraseña CORRECTA durante el bloqueo también 429.
        mockMvc.perform(conIp(loginCon("admin", PASSWORD_ADMIN), ipReal))
                .andExpect(status().isTooManyRequests());

        // Un usuario inexistente se bloquea igual y responde EXACTAMENTE el mismo cuerpo.
        String ipInexistente = "198.51.100.11";
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(conIp(loginCon("nadie.registrado", "pase-inventado"), ipInexistente))
                    .andExpect(status().isUnauthorized());
        }
        MvcResult sextoInexistente = mockMvc.perform(
                        conIp(loginCon("nadie.registrado", "pase-inventado"), ipInexistente))
                .andExpect(status().isTooManyRequests())
                .andReturn();
        assertThat(sextoInexistente.getResponse().getContentAsString())
                .isEqualTo(sextoAdmin.getResponse().getContentAsString());

        // Al avanzar el reloj la ventana se agota y el login vuelve a funcionar.
        configuracion.reloj.avanzar(Duration.ofMinutes(15).plusSeconds(1));
        mockMvc.perform(conIp(loginCon("admin", PASSWORD_ADMIN), ipReal))
                .andExpect(status().isOk());
    }

    @Test
    void unExitoReiniciaSoloElContadorDelUsuarioYCuatroFallosMasNoBloquean() throws Exception {
        String ipA = "203.0.113.1";
        String ipB = "203.0.113.2";

        // 4 fallos desde la IP A.
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(conIp(loginCon("admin", "clave-mala"), ipA))
                    .andExpect(status().isUnauthorized());
        }
        // Éxito: reinicia el contador del usuario (la IP A se queda en 4, nunca se reinicia).
        mockMvc.perform(conIp(loginCon("admin", PASSWORD_ADMIN), ipA))
                .andExpect(status().isOk());

        // 4 fallos más desde la IP B: el usuario no vuelve a alcanzar 5.
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(conIp(loginCon("admin", "clave-mala"), ipB))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(conIp(loginCon("admin", PASSWORD_ADMIN), ipB))
                .andExpect(status().isOk());

        // A nivel de componente: el contador del usuario se reinició pero la IP A
        // sigue acumulando (5º fallo sobre la IP A -> bloqueada).
        limitadorIntentos.registrarFallo("admin", ipA);
        assertThat(limitadorIntentos.estaBloqueado("admin", ipA)).isPositive();
    }

    @Test
    void cincoFallosDeLaMismaIpConUsuariosDistintosBloqueanEsaIp() throws Exception {
        String ip = "203.0.113.50";

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(conIp(loginCon("inventado." + i, "pase"), ip))
                    .andExpect(status().isUnauthorized());
        }
        // El 6º intento desde la misma IP, con un usuario nuevo, se bloquea por IP.
        mockMvc.perform(conIp(loginCon("inventado.nuevo", "pase"), ip))
                .andExpect(status().isTooManyRequests());

        // El mismo usuario desde otra IP sigue dando 401: solo la IP quedó bloqueada.
        mockMvc.perform(conIp(loginCon("inventado.nuevo", "pase"), "203.0.113.60"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void laCabeceraXForwardedForSeIgnoraParaElBloqueo() throws Exception {
        String ipReal = "198.51.100.1";

        // Fallos con X-Forwarded-For falsificada: el bloqueo sigue atado a getRemoteAddr().
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(conIp(
                            conCabecera(loginCon("victima", "pase"), "X-Forwarded-For", "1.1.1.1"), ipReal))
                    .andExpect(status().isUnauthorized());
        }
        // Cambiar X-Forwarded-For no esquiva el bloqueo de la IP real.
        mockMvc.perform(conIp(
                        conCabecera(loginCon("victima", "pase"), "X-Forwarded-For", "2.2.2.2"), ipReal))
                .andExpect(status().isTooManyRequests());

        // Y una IP distinta cuyo X-Forwarded-For apunta a la IP bloqueada NO se
        // bloquea: con un usuario nuevo el bloqueo depende de getRemoteAddr().
        mockMvc.perform(conIp(
                        conCabecera(loginCon("otra.victima", "pase"), "X-Forwarded-For", ipReal), "198.51.100.2"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void losIntentosDuranteElBloqueoNoExtiendenLaCuentaAtras() throws Exception {
        String ip = "198.51.100.99";

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(conIp(loginCon("admin", "pase"), ip))
                    .andExpect(status().isUnauthorized());
        }
        // Varios 429 (mala y correcta) durante el bloqueo: ninguno registra fallo.
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(conIp(loginCon("admin", "pase"), ip))
                    .andExpect(status().isTooManyRequests());
            mockMvc.perform(conIp(loginCon("admin", PASSWORD_ADMIN), ip))
                    .andExpect(status().isTooManyRequests());
        }

        // Si el 429 extendiera el bloqueo, una sola ventana no alcanzaría.
        configuracion.reloj.avanzar(Duration.ofMinutes(15).plusSeconds(1));
        mockMvc.perform(conIp(loginCon("admin", PASSWORD_ADMIN), ip))
                .andExpect(status().isOk());
    }

    @Test
    void un400PorBodyInvalidoNiCuentaNiCompruebaElBloqueo() throws Exception {
        String ip = "203.0.113.6";

        // Body inválido: 400 por validación; no cuenta como fallo.
        mockMvc.perform(conIp(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\"}"), ip))
                .andExpect(status().isBadRequest());

        // Solo 4 fallos válidos después: si el 400 hubiera contado, el usuario
        // llegaría a 5 y el login correcto respondería 429.
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(conIp(loginCon("admin", "clave-mala"), ip))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(conIp(loginCon("admin", PASSWORD_ADMIN), ip))
                .andExpect(status().isOk());

        // Y durante un bloqueo activo, un body inválido sigue siendo 400 (no 429):
        // la validación precede a la comprobación de bloqueo.
        String ipBloqueada = "203.0.113.7";
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(conIp(loginCon("admin", "clave-mala"), ipBloqueada))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(conIp(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\"}"), ipBloqueada))
                .andExpect(status().isBadRequest());
    }

    private MockHttpServletRequestBuilder loginCon(String username, String password) {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password));
    }

    private MockHttpServletRequestBuilder conCabecera(MockHttpServletRequestBuilder peticion,
                                                      String nombre, String valor) {
        return peticion.header(nombre, valor);
    }

    /** Fija la IP de origen de la petición ({@code getRemoteAddr}). */
    private MockHttpServletRequestBuilder conIp(MockHttpServletRequestBuilder peticion, String ip) {
        return peticion.with(request -> {
            request.setRemoteAddr(ip);
            return request;
        });
    }

    /** Reloj cuyo instante actual puede ajustarse y adelantarse en las pruebas. */
    static final class RelojControlado extends Clock {

        private Instant ahora;

        RelojControlado(Instant inicio) {
            this.ahora = inicio;
        }

        void fijar(Instant instante) {
            this.ahora = instante;
        }

        void avanzar(Duration tiempo) {
            this.ahora = ahora.plus(tiempo);
        }

        @Override
        public Instant instant() {
            return ahora;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }
    }
}