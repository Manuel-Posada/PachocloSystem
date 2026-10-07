package com.pachoclosystem.pachoclosystem.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/** IP del cliente con y sin proxies de confianza, sin contexto Spring. */
class ResolutorIpClienteTest {

    private static MockHttpServletRequest peticion(String remota, String... xForwardedFor) {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.setRemoteAddr(remota);
        for (String valor : xForwardedFor) {
            peticion.addHeader(ResolutorIpCliente.CABECERA, valor);
        }
        return peticion;
    }

    @Test
    void sinProxiesConfiablesSeUsaLaIpDelSocketYSeIgnoraLaCabecera() {
        ResolutorIpCliente resolutor = new ResolutorIpCliente("");

        assertThat(resolutor.ipDe(peticion("198.51.100.1", "1.1.1.1"))).isEqualTo("198.51.100.1");
        assertThat(resolutor.ipDe(peticion("10.0.0.5", "1.1.1.1"))).isEqualTo("10.0.0.5");
    }

    @Test
    void unaPeticionQueNoVieneDeUnProxyConfiableNoPuedeFalsificarSuIp() {
        ResolutorIpCliente resolutor = new ResolutorIpCliente("10.0.0.5");

        assertThat(resolutor.ipDe(peticion("198.51.100.1", "203.0.113.9"))).isEqualTo("198.51.100.1");
    }

    @Test
    void desdeUnProxyConfiableSeUsaLaIpQueEsteAnade() {
        ResolutorIpCliente resolutor = new ResolutorIpCliente("10.0.0.5");

        assertThat(resolutor.ipDe(peticion("10.0.0.5", "203.0.113.9"))).isEqualTo("203.0.113.9");
    }

    @Test
    void loQueElClienteEscribeALaIzquierdaNoCuenta() {
        ResolutorIpCliente resolutor = new ResolutorIpCliente("10.0.0.5");

        // El cliente envía "X-Forwarded-For: 1.1.1.1" y el proxy añade su IP real.
        assertThat(resolutor.ipDe(peticion("10.0.0.5", "1.1.1.1, 203.0.113.9"))).isEqualTo("203.0.113.9");
    }

    @Test
    void conVariosProxiesConfiablesEncadenadosSeSaltanTodos() {
        ResolutorIpCliente resolutor = new ResolutorIpCliente("10.0.0.5, 10.0.0.6");

        assertThat(resolutor.ipDe(peticion("10.0.0.5", "1.1.1.1, 203.0.113.9, 10.0.0.6")))
                .isEqualTo("203.0.113.9");
        // La cabecera puede llegar repetida en vez de separada por comas.
        assertThat(resolutor.ipDe(peticion("10.0.0.5", "203.0.113.9", "10.0.0.6")))
                .isEqualTo("203.0.113.9");
    }

    @Test
    void sinCabeceraOSoloConProxiesOConBasuraSeUsaLaIpDelSocket() {
        ResolutorIpCliente resolutor = new ResolutorIpCliente("10.0.0.5,10.0.0.6");

        assertThat(resolutor.ipDe(peticion("10.0.0.5"))).isEqualTo("10.0.0.5");
        assertThat(resolutor.ipDe(peticion("10.0.0.5", "10.0.0.6"))).isEqualTo("10.0.0.5");
        assertThat(resolutor.ipDe(peticion("10.0.0.5", "no-es-una-ip"))).isEqualTo("10.0.0.5");
        assertThat(resolutor.ipDe(peticion("10.0.0.5", "unknown, 203.0.113.9"))).isEqualTo("203.0.113.9");
    }

    @Test
    void admiteIpv6() {
        ResolutorIpCliente resolutor = new ResolutorIpCliente("0:0:0:0:0:0:0:1");

        assertThat(resolutor.ipDe(peticion("0:0:0:0:0:0:0:1", "2001:db8::7"))).isEqualTo("2001:db8::7");
    }

    @Test
    void unaConfiguracionQueNoEsUnaListaDeIpsImpideArrancar() {
        assertThatIllegalStateException()
                .isThrownBy(() -> new ResolutorIpCliente("10.0.0.5, proxy.interno"))
                .withMessageContaining("app.login.proxies-confiables")
                .withMessageContaining("proxy.interno");
        assertThatIllegalStateException().isThrownBy(() -> new ResolutorIpCliente("10.0.0.0/8"));
    }
}
