package com.pachoclosystem.pachoclosystem.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Límite de intentos de login con un proxy inverso de confianza configurado
 * ({@code app.login.proxies-confiables}): todas las peticiones llegan desde la
 * IP del proxy, pero el bloqueo se aplica a la IP del cliente que este añade en
 * {@code X-Forwarded-For}. Quien no es el proxy no puede elegir su IP con esa
 * cabecera.
 */
@TestPropertySource(properties = "app.login.proxies-confiables=" + LoginTrasProxyConfiableHttpTest.PROXY)
class LoginTrasProxyConfiableHttpTest extends MockMvcBaseTest {

    static final String PROXY = "10.0.0.5";
    private static final String PASSWORD_ADMIN = "pwd-de-prueba-solo-tests-12345";

    @Test
    void elBloqueoPorIpSeAplicaAlClienteYNoAlProxy() throws Exception {
        String atacante = "203.0.113.66";

        // Un cliente prueba 50 usernames: se bloquea su IP...
        for (int i = 0; i < 50; i++) {
            mockMvc.perform(desde(atacante, loginCon("inventado." + i, "pase")))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(desde(atacante, loginCon("admin", PASSWORD_ADMIN)))
                .andExpect(status().isTooManyRequests());

        // ...pero no la del proxy: los demás clientes, por el mismo proxy, entran.
        mockMvc.perform(desde("198.51.100.7", loginCon("admin", PASSWORD_ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void elBloqueoDeUnaCuentaSeAplicaAlParConLaIpDelCliente() throws Exception {
        String atacante = "203.0.113.66";

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(desde(atacante, loginCon("admin", "adivinando")))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(desde(atacante, loginCon("admin", PASSWORD_ADMIN)))
                .andExpect(status().isTooManyRequests());

        // El admin, desde otro cliente y a través del mismo proxy, entra.
        mockMvc.perform(desde("198.51.100.7", loginCon("admin", PASSWORD_ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void elClienteNoEsquivaElBloqueoEscribiendoSuPropiaCabecera() throws Exception {
        String atacante = "203.0.113.66";

        // El cliente envía su propio X-Forwarded-For y el proxy añade su IP real
        // al final: se usa la del proxy, no la inventada.
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(conCabecera(PROXY, "1.1.1." + i + ", " + atacante, loginCon("admin", "adivinando")))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(conCabecera(PROXY, "9.9.9.9, " + atacante, loginCon("admin", PASSWORD_ADMIN)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void quienNoEsElProxyNoPuedeElegirSuIpConLaCabecera() throws Exception {
        String directa = "198.51.100.20";

        // Fallos directos (sin pasar por el proxy) con IPs inventadas en la cabecera.
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(conCabecera(directa, "1.1.1." + i, loginCon("admin", "adivinando")))
                    .andExpect(status().isUnauthorized());
        }
        // Cuenta su IP real: cambiar la cabecera no lo desbloquea.
        mockMvc.perform(conCabecera(directa, "2.2.2.2", loginCon("admin", PASSWORD_ADMIN)))
                .andExpect(status().isTooManyRequests());
    }

    /** Petición que el proxy reenvía en nombre de {@code cliente}. */
    private MockHttpServletRequestBuilder desde(String cliente, MockHttpServletRequestBuilder peticion) {
        return conCabecera(PROXY, cliente, peticion);
    }

    private MockHttpServletRequestBuilder conCabecera(String ipSocket, String xForwardedFor,
                                                      MockHttpServletRequestBuilder peticion) {
        return peticion.header("X-Forwarded-For", xForwardedFor).with(request -> {
            request.setRemoteAddr(ipSocket);
            return request;
        });
    }

    private MockHttpServletRequestBuilder loginCon(String username, String password) {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password));
    }
}
