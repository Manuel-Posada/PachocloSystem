package com.pachoclosystem.pachoclosystem.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios del {@link LimitadorIntentosLogin} con reloj controlado: sin
 * dormir el hilo se comprueban el bloqueo a los 5 fallos, los segundos
 * restantes, el desbloqueo al agotarse la ventana, el reinicio por éxito (solo
 * del usuario), la independencia de los espacios usuario/IP, la caducidad y el
 * tope de entradas, la normalización del username y la atomicidad en
 * concurrencia.
 */
class LimitadorIntentosLoginTest {

    private static final Instant INICIO = Instant.parse("2026-01-01T00:00:00Z");

    private RelojControlado reloj;
    private LimitadorIntentosLogin limitador;

    @BeforeEach
    void prepararLimitador() {
        reloj = new RelojControlado(INICIO);
        limitador = new LimitadorIntentosLogin(reloj, 5, 15, 10000);
    }

    @Test
    void cincoFallosBloqueanYElSextoIntentoVeSegundosRestantes() {
        for (int i = 0; i < 5; i++) {
            assertThat(limitador.estaBloqueado("admin", "1.2.3.4")).isZero();
            limitador.registrarFallo("admin", "1.2.3.4");
        }
        assertThat(limitador.estaBloqueado("admin", "1.2.3.4")).isPositive();
    }

    @Test
    void losSegundosRestantesSonLosQueFaltanParaElFinDelBloqueo() {
        for (int i = 0; i < 5; i++) {
            limitador.registrarFallo("admin", "1.2.3.4");
        }
        assertThat(limitador.estaBloqueado("admin", "1.2.3.4")).isEqualTo(900);

        reloj.avanzar(Duration.ofMinutes(10));
        assertThat(limitador.estaBloqueado("admin", "1.2.3.4")).isEqualTo(300);

        reloj.avanzar(Duration.ofMinutes(5).plusSeconds(1));
        assertThat(limitador.estaBloqueado("admin", "1.2.3.4")).isZero();
    }

    @Test
    void trasLaVentanaDeBloqueoElLoginVuelveAPermitir() {
        for (int i = 0; i < 5; i++) {
            limitador.registrarFallo("admin", "1.2.3.4");
        }
        reloj.avanzar(Duration.ofMinutes(14));
        assertThat(limitador.estaBloqueado("admin", "1.2.3.4")).isPositive();

        reloj.avanzar(Duration.ofMinutes(2));
        assertThat(limitador.estaBloqueado("admin", "1.2.3.4")).isZero();
    }

    @Test
    void unExitoReiniciaSoloElContadorDelUsuarioYLaIpSigueAcumulando() {
        for (int i = 0; i < 4; i++) {
            limitador.registrarFallo("admin", "1.2.3.4");
        }
        limitador.registrarExito("admin");

        // El usuario ya no está bloqueado en solitario.
        assertThat(limitador.estaBloqueado("admin", "8.8.8.8")).isZero();

        // Pero la IP no se reinició: con el 5º fallo (4 previos + 1) se bloquea.
        limitador.registrarFallo("admin", "1.2.3.4");
        assertThat(limitador.estaBloqueado("admin", "1.2.3.4")).isPositive();

        // Un éxito posterior con otro username tampoco toca el contador de IP.
        limitador.registrarExito("otro-usuario");
        assertThat(limitador.estaBloqueado("admin", "1.2.3.4")).isPositive();
    }

    @Test
    void laIpSeBloqueaConFallosDeUsuariosDistintosAunqueCadaUnoTengaPocos() {
        for (int i = 0; i < 5; i++) {
            limitador.registrarFallo("usuario" + i, "1.2.3.4");
        }
        assertThat(limitador.estaBloqueado("cualquiera", "1.2.3.4")).isPositive();
        assertThat(limitador.estaBloqueado("usuario0", "9.9.9.9")).isZero();
    }

    @Test
    void losEspaciosDeUsuarioEIpSonIndependientes() {
        for (int i = 0; i < 5; i++) {
            limitador.registrarFallo("ana", "5.5.5.5");
        }
        // Bloqueada por el usuario (desde otra IP) y por la IP (con otro usuario).
        assertThat(limitador.estaBloqueado("ana", "6.6.6.6")).isPositive();
        assertThat(limitador.estaBloqueado("otro", "5.5.5.5")).isPositive();
        // Nada que ver con una combinación ajena.
        assertThat(limitador.estaBloqueado("otro", "7.7.7.7")).isZero();
    }

    @Test
    void elUsernameEnDistintoCaseCuentaComoElMismoYElExitoLoReinicia() {
        for (int i = 0; i < 5; i++) {
            limitador.registrarFallo(i % 2 == 0 ? "Admin" : "  admin  ", "3.3.3.3");
        }
        // Bloqueado por usuario (desde otra IP, para no mezclar con la IP).
        assertThat(limitador.estaBloqueado("ADMIN", "4.4.4.4")).isPositive();

        // Un éxito con otro case reinicia la MISMA clave.
        limitador.registrarExito("Admin");
        assertThat(limitador.estaBloqueado("admin", "4.4.4.4")).isZero();
    }

    @Test
    void lasEntradasCaducadasSeEliminanAlSuperarElTope() {
        LimitadorIntentosLogin pequeno = new LimitadorIntentosLogin(reloj, 5, 15, 3);
        pequeno.registrarFallo("a", "1.1.1.1");
        pequeno.registrarFallo("b", "2.2.2.2");
        for (int i = 0; i < 4; i++) {
            pequeno.registrarFallo("a", "1.1.1.1");
        }
        assertThat(pequeno.estaBloqueado("a", "9.9.9.9")).isPositive();

        // Caducan todas las entradas; un nuevo fallo dispara la purga.
        reloj.avanzar(Duration.ofMinutes(16));
        pequeno.registrarFallo("c", "3.3.3.3");

        // "a" ya no bloquea y un fallo nuevo reinicia su contador a 1.
        assertThat(pequeno.estaBloqueado("a", "9.9.9.9")).isZero();
        pequeno.registrarFallo("a", "4.4.4.4");
        assertThat(pequeno.estaBloqueado("a", "5.5.5.5")).isZero();
    }

    @Test
    void alSuperarElTopeNoSeInventanClavesDeUsuarioPeroLaIpSeSigueContando() {
        LimitadorIntentosLogin tope = new LimitadorIntentosLogin(reloj, 5, 15, 2);
        tope.registrarFallo("u1", "1.1.1.1");

        // Ya en el tope, la clave nueva "u2" no se crea...
        tope.registrarFallo("u2", "1.1.1.1");
        assertThat(tope.estaBloqueado("u2", "9.9.9.9")).isZero();

        // ...pero la IP se sigue contando hasta bloquearse.
        for (int i = 0; i < 4; i++) {
            tope.registrarFallo("u" + (3 + i), "1.1.1.1");
        }
        assertThat(tope.estaBloqueado("usuario-nuevo", "1.1.1.1")).isPositive();

        // Las claves de usuario existentes sí se actualizan bajo el tope.
        for (int i = 0; i < 4; i++) {
            tope.registrarFallo("u1", "2.2.2.2");
        }
        assertThat(tope.estaBloqueado("u1", "9.9.9.9")).isPositive();
    }

    @Test
    void laConcurrenciaDeFallosEsAtomicaContandoExactamenteLosNDeTodosLosHilos() throws Exception {
        int hilos = 8;
        LimitadorIntentosLogin atomico = new LimitadorIntentosLogin(reloj, hilos, 15, 10000);
        ExecutorService ejecutor = Executors.newFixedThreadPool(hilos);
        CountDownLatch listos = new CountDownLatch(hilos);
        CountDownLatch disparo = new CountDownLatch(1);
        try {
            List<Future<?>> futuros = new ArrayList<>();
            for (int i = 0; i < hilos; i++) {
                futuros.add(ejecutor.submit(() -> {
                    listos.countDown();
                    disparo.await();
                    atomico.registrarFallo("concurrente", "10.1.1.1");
                    return null;
                }));
            }
            assertThat(listos.await(5, TimeUnit.SECONDS)).isTrue();
            disparo.countDown();
            for (Future<?> futuro : futuros) {
                futuro.get(5, TimeUnit.SECONDS);
            }

            // max-intentos == hilos: si un incremento se perdiera (o se
            // contara dos veces), el contador no sería exacto y aquí fallaría.
            assertThat(atomico.estaBloqueado("concurrente", "10.1.1.1")).isPositive();
            assertThat(atomico.estaBloqueado("otro-usuario", "10.1.1.1")).isPositive();
        } finally {
            ejecutor.shutdownNow();
        }
    }

    @Test
    void elUsernameConCaracteresDeControlYSinElSeSaneaParaElLog() {
        assertThat(LimitadorIntentosLogin.sanearUsername("  Admin\nBORRAR  ")).isEqualTo("adminborrar");
        assertThat(LimitadorIntentosLogin.sanearUsername("a".repeat(50))).hasSize(30);
        assertThat(LimitadorIntentosLogin.sanearUsername("tabulador\troto")).isEqualTo("tabuladorroto");
        assertThat(LimitadorIntentosLogin.sanearUsername(null)).isEmpty();
        assertThat(LimitadorIntentosLogin.sanearUsername("   ")).isEmpty();
    }

    /** Reloj cuyo instante actual puede adelantarse para controlar el tiempo. */
    private static final class RelojControlado extends Clock {

        private Instant ahora;

        private RelojControlado(Instant inicio) {
            this.ahora = inicio;
        }

        void avanzar(Duration tiempo) {
            ahora = ahora.plus(tiempo);
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