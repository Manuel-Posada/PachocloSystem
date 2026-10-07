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
        for (String invalida : new String[]{"10.0.0.0/33", "10.0.0.0/", "/8", "10.0.0.0/8/1", "10.0.0.0/x",
                "2001:db8::/129", "999.1.1.1", "10.0.0.0/-1"}) {
            assertThatIllegalStateException()
                    .as(invalida)
                    .isThrownBy(() -> new ResolutorIpCliente(invalida))
                    .withMessageContaining("CIDR");
        }
    }

    @Test
    void desdeUnRangoDeProxiesConfiablesSeUsaLaIpQueAnadeElProxy() {
        // Proxies de una plataforma sin IP fija (p. ej. 100.64.0.0/10).
        ResolutorIpCliente resolutor = new ResolutorIpCliente("100.64.0.0/10");

        assertThat(resolutor.ipDe(peticion("100.64.0.1", "203.0.113.7"))).isEqualTo("203.0.113.7");
        assertThat(resolutor.ipDe(peticion("100.127.255.254", "1.2.3.4, 203.0.113.7")))
                .isEqualTo("203.0.113.7");
        // Fuera del rango (100.128.0.0 ya no está en /10): la cabecera no se cree.
        assertThat(resolutor.ipDe(peticion("100.128.0.0", "203.0.113.7"))).isEqualTo("100.128.0.0");
        assertThat(resolutor.ipDe(peticion("100.63.255.255", "203.0.113.7"))).isEqualTo("100.63.255.255");
    }

    @Test
    void variosRangosYSaltosDentroDeEllosSeSaltanTodos() {
        ResolutorIpCliente resolutor = new ResolutorIpCliente("10.0.0.0/8, 192.168.1.0/24");

        assertThat(resolutor.ipDe(peticion("10.1.2.3", "198.51.100.9, 192.168.1.20, 10.9.9.9")))
                .isEqualTo("198.51.100.9");
    }

    @Test
    void rangosIpv6YComparacionPorValor() {
        ResolutorIpCliente resolutor = new ResolutorIpCliente("fd00::/8, ::1");

        assertThat(resolutor.ipDe(peticion("fd12:3456::1", "2001:db8::7"))).isEqualTo("2001:db8::7");
        assertThat(resolutor.ipDe(peticion("0:0:0:0:0:0:0:1", "2001:db8::8"))).isEqualTo("2001:db8::8");
        assertThat(resolutor.ipDe(peticion("fe80::1", "2001:db8::7"))).isEqualTo("fe80::1");
        // Un rango IPv4 no contiene direcciones IPv6 ni al revés.
        assertThat(new ResolutorIpCliente("0.0.0.0/0").ipDe(peticion("2001:db8::1", "203.0.113.7")))
                .isEqualTo("2001:db8::1");
    }

    @Test
    void prefijosLimite() {
        assertThat(new ResolutorIpCliente("203.0.113.5/32").ipDe(peticion("203.0.113.5", "198.51.100.1")))
                .isEqualTo("198.51.100.1");
        assertThat(new ResolutorIpCliente("203.0.113.5/32").ipDe(peticion("203.0.113.6", "198.51.100.1")))
                .isEqualTo("203.0.113.6");
        assertThat(new ResolutorIpCliente("10.0.0.0/7").ipDe(peticion("11.255.0.1", "198.51.100.1")))
                .isEqualTo("198.51.100.1");
        assertThat(new ResolutorIpCliente("10.0.0.0/7").ipDe(peticion("12.0.0.1", "198.51.100.1")))
                .isEqualTo("12.0.0.1");
    }
}
