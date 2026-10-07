package com.pachoclosystem.pachoclosystem.controller;

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
 * ajustable), con la configuración por defecto (sin proxies de confianza):
 * 5 fallos de un usuario desde una IP -> el 6º intento responde 429 con el
 * cuerpo uniforme y {@code Retry-After}; el bloqueo no distingue usuarios
 * inexistentes; atacar una cuenta desde una IP no la bloquea desde otra ni
 * bloquea a otros usuarios de la misma IP; la IP se bloquea a los 50 fallos;
 * un éxito reinicia solo el par usuario + IP; {@code X-Forwarded-For} se
 * ignora; el 429 no extiende la cuenta atrás y un 400 por body inválido no
 * cuenta ni comprueba el bloqueo.
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
    void atacarAlAdminDesdeUnaIpNoLeImpideEntrarDesdeOtra() throws Exception {
        String ipAtacante = "203.0.113.66";
        String ipAdmin = "198.51.100.7";

        // El atacante conoce el username y falla hasta quedar bloqueado.
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(conIp(loginCon("admin", "adivinando-" + i), ipAtacante))
                    .andExpect(status().isUnauthorized());
        }
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(conIp(loginCon("admin", "sigo-probando"), ipAtacante))
                    .andExpect(status().isTooManyRequests());
        }

        // El administrador entra con normalidad desde su IP.
        mockMvc.perform(conIp(loginCon("admin", PASSWORD_ADMIN), ipAdmin))
                .andExpect(status().isOk());
        // Y su éxito no desbloquea al atacante.
        mockMvc.perform(conIp(loginCon("admin", PASSWORD_ADMIN), ipAtacante))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void unExitoReiniciaElParYCuatroFallosMasNoBloquean() throws Exception {
        String ip = "203.0.113.1";

        for (int i = 0; i < 4; i++) {
            mockMvc.perform(conIp(loginCon("admin", "clave-mala"), ip))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(conIp(loginCon("admin", PASSWORD_ADMIN), ip))
                .andExpect(status().isOk());

        // El contador del par volvió a cero: 4 fallos más tampoco bloquean.
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(conIp(loginCon("admin", "clave-mala"), ip))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(conIp(loginCon("admin", PASSWORD_ADMIN), ip))
                .andExpect(status().isOk());
    }

    @Test
    void variosUsuariosQueCompartenIpNoSeBloqueanEntreSi() throws Exception {
        // Sin proxies de confianza, todos los que pasan por un mismo proxy
        // (p. ej. el de desarrollo del frontend) llegan con la misma IP.
        String ipCompartida = "127.0.0.1";

        // Alguien se equivoca hasta bloquearse...
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(conIp(loginCon("despistado", "pase-erroneo"), ipCompartida))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(conIp(loginCon("despistado", "pase-erroneo"), ipCompartida))
                .andExpect(status().isTooManyRequests());

        // ...y el resto de usuarios de esa IP sigue entrando.
        mockMvc.perform(conIp(loginCon("admin", PASSWORD_ADMIN), ipCompartida))
                .andExpect(status().isOk());
        mockMvc.perform(conIp(loginCon("otro.usuario", "pase-erroneo"), ipCompartida))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unaIpConCincuentaFallosDeUsuariosDistintosSeBloquea() throws Exception {
        String ip = "203.0.113.50";

        for (int i = 0; i < 50; i++) {
            mockMvc.perform(conIp(loginCon("inventado." + i, "pase"), ip))
                    .andExpect(status().isUnauthorized());
        }
        // Desde esa IP ya nadie entra, ni con la contraseña correcta...
        mockMvc.perform(conIp(loginCon("admin", PASSWORD_ADMIN), ip))
                .andExpect(status().isTooManyRequests());

        // ...pero desde otra IP sí.
        mockMvc.perform(conIp(loginCon("admin", PASSWORD_ADMIN), "203.0.113.60"))
                .andExpect(status().isOk());

        // Y se recupera al agotarse la ventana.
        configuracion.reloj.avanzar(Duration.ofMinutes(15).plusSeconds(1));
        mockMvc.perform(conIp(loginCon("admin", PASSWORD_ADMIN), ip))
                .andExpect(status().isOk());
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

        // Y otra IP cuyo X-Forwarded-For apunta a la bloqueada NO hereda el
        // bloqueo: con la configuración por defecto cuenta getRemoteAddr().
        mockMvc.perform(conIp(
                        conCabecera(loginCon("victima", "pase"), "X-Forwarded-For", ipReal), "198.51.100.2"))
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