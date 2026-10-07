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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests unitarios del {@link LimitadorIntentosLogin} con reloj controlado y los
 * umbrales por defecto (5 por usuario + IP, 50 por IP, 100 por usuario y
 * ventana de 15 minutos): bloqueo, segundos restantes y recuperación; que
 * atacar una cuenta desde una IP no bloquee a esa cuenta desde otra IP ni a
 * otros usuarios de la misma IP; los umbrales por IP y por usuario; el reinicio
 * por éxito; la caducidad, el tope de entradas y la atomicidad.
 */
class LimitadorIntentosLoginTest {

    private static final Instant INICIO = Instant.parse("2026-01-01T00:00:00Z");

    private RelojControlado reloj;
    private LimitadorIntentosLogin limitador;

    @BeforeEach
    void prepararLimitador() {
        reloj = new RelojControlado(INICIO);
        limitador = limitador(10000);
    }

    private LimitadorIntentosLogin limitador(int maxEntradas) {
        return new LimitadorIntentosLogin(reloj, 5, 50, 100, 15, maxEntradas);
    }

    private void fallar(LimitadorIntentosLogin destino, String usuario, String ip, int veces) {
        for (int i = 0; i < veces; i++) {
            destino.registrarFallo(usuario, ip);
        }
    }

    // ------------------------------------------------- bloqueo y recuperación

    @Test
    void cincoFallosDelMismoUsuarioDesdeLaMismaIpBloqueanEsePar() {
        for (int i = 0; i < 5; i++) {
            assertThat(limitador.estaBloqueado("admin", "1.2.3.4")).isZero();
            limitador.registrarFallo("admin", "1.2.3.4");
        }
        assertThat(limitador.estaBloqueado("admin", "1.2.3.4")).isPositive();
    }

    @Test
    void losSegundosRestantesSonLosQueFaltanParaElFinDelBloqueo() {
        fallar(limitador, "admin", "1.2.3.4", 5);
        assertThat(limitador.estaBloqueado("admin", "1.2.3.4")).isEqualTo(900);

        reloj.avanzar(Duration.ofMinutes(10));
        assertThat(limitador.estaBloqueado("admin", "1.2.3.4")).isEqualTo(300);

        reloj.avanzar(Duration.ofMinutes(5));
        assertThat(limitador.estaBloqueado("admin", "1.2.3.4")).isZero();
    }

    @Test
    void trasLaVentanaElParSeRecuperaYVuelveAContarDesdeCero() {
        fallar(limitador, "admin", "1.2.3.4", 5);
        reloj.avanzar(Duration.ofMinutes(15).plusSeconds(1));
        assertThat(limitador.estaBloqueado("admin", "1.2.3.4")).isZero();

        // El contador arranca de nuevo: 4 fallos más no bloquean, el 5.º sí.
        fallar(limitador, "admin", "1.2.3.4", 4);
        assertThat(limitador.estaBloqueado("admin", "1.2.3.4")).isZero();
        limitador.registrarFallo("admin", "1.2.3.4");
        assertThat(limitador.estaBloqueado("admin", "1.2.3.4")).isPositive();
    }

    // -------------------------------------- no se bloquea a usuarios legítimos

    @Test
    void atacarUnaCuentaDesdeUnaIpNoLaBloqueaDesdeOtraIp() {
        // El atacante conoce el username "admin" y falla 5 veces (y sigue: los
        // intentos bloqueados ni siquiera llegan a registrarse en AuthService).
        fallar(limitador, "admin", "203.0.113.66", 5);

        assertThat(limitador.estaBloqueado("admin", "203.0.113.66")).isPositive();
        assertThat(limitador.estaBloqueado("admin", "198.51.100.7")).isZero();
    }

    @Test
    void atacarUnUsuarioNoBloqueaAOtroUsuarioDeLaMismaIp() {
        fallar(limitador, "victima", "10.0.0.1", 5);

        assertThat(limitador.estaBloqueado("victima", "10.0.0.1")).isPositive();
        assertThat(limitador.estaBloqueado("otro.usuario", "10.0.0.1")).isZero();
    }

    @Test
    void variosUsuariosQueCompartenIpSoloSeBloqueanASiMismos() {
        // Una oficina o un proxy: 9 usuarios se equivocan 4 veces cada uno desde
        // la misma IP (36 fallos, por debajo del umbral de la IP).
        for (int i = 0; i < 9; i++) {
            fallar(limitador, "usuario." + i, "192.0.2.10", 4);
        }
        for (int i = 0; i < 9; i++) {
            assertThat(limitador.estaBloqueado("usuario." + i, "192.0.2.10")).isZero();
        }
        // Uno de ellos insiste y se bloquea; el resto, y quien no ha fallado, no.
        limitador.registrarFallo("usuario.0", "192.0.2.10");
        assertThat(limitador.estaBloqueado("usuario.0", "192.0.2.10")).isPositive();
        assertThat(limitador.estaBloqueado("usuario.1", "192.0.2.10")).isZero();
        assertThat(limitador.estaBloqueado("recien.llegado", "192.0.2.10")).isZero();
    }

    // ------------------------------------------ fuerza bruta: IP y cuenta

    @Test
    void unaIpQuePruebaMuchosUsuariosSeBloqueaAlLlegarASuUmbral() {
        // Password spraying: 49 usernames distintos, un fallo cada uno.
        for (int i = 0; i < 49; i++) {
            limitador.registrarFallo("probado." + i, "203.0.113.99");
        }
        assertThat(limitador.estaBloqueado("nuevo", "203.0.113.99")).isZero();

        limitador.registrarFallo("probado.49", "203.0.113.99");
        assertThat(limitador.estaBloqueado("nuevo", "203.0.113.99")).isPositive();
        // Solo esa IP: desde otra, los mismos usuarios siguen pudiendo entrar.
        assertThat(limitador.estaBloqueado("probado.0", "198.51.100.1")).isZero();
    }

    @Test
    void unaCuentaAtacadaDesdeMuchasIpsSeBloqueaAlLlegarASuUmbral() {
        // Fuerza bruta distribuida: 20 IPs x 5 fallos = 100 fallos contra admin.
        for (int ip = 1; ip <= 19; ip++) {
            fallar(limitador, "admin", "203.0.113." + ip, 5);
        }
        assertThat(limitador.estaBloqueado("admin", "198.51.100.200")).isZero();

        fallar(limitador, "admin", "203.0.113.20", 5);
        assertThat(limitador.estaBloqueado("admin", "198.51.100.200")).isPositive();
        // Otras cuentas no se ven afectadas.
        assertThat(limitador.estaBloqueado("otra.cuenta", "198.51.100.200")).isZero();

        reloj.avanzar(Duration.ofMinutes(15).plusSeconds(1));
        assertThat(limitador.estaBloqueado("admin", "198.51.100.200")).isZero();
    }

    @Test
    void losUmbralesDeIpYDeUsuarioNoPuedenSerMenoresQueElDelPar() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LimitadorIntentosLogin(reloj, 5, 4, 100, 15, 10000))
                .withMessageContaining("max-intentos-ip");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LimitadorIntentosLogin(reloj, 5, 50, 4, 15, 10000))
                .withMessageContaining("max-intentos-usuario");
    }

    // ----------------------------------------------------------------- éxito

    @Test
    void unExitoReiniciaSoloElParYNoLosContadoresDeIpNiDeCuenta() {
        fallar(limitador, "admin", "1.2.3.4", 4);
        limitador.registrarExito("admin", "1.2.3.4");

        // El par vuelve a empezar: 4 fallos más no bloquean.
        fallar(limitador, "admin", "1.2.3.4", 4);
        assertThat(limitador.estaBloqueado("admin", "1.2.3.4")).isZero();

        // Pero la IP sigue sumando todos sus fallos (8 de 50): 42 más la bloquean.
        for (int i = 0; i < 42; i++) {
            limitador.registrarFallo("probado." + i, "1.2.3.4");
        }
        assertThat(limitador.estaBloqueado("cualquiera", "1.2.3.4")).isPositive();
    }

    @Test
    void elExitoDesdeOtraIpNoDesbloqueaAlAtacante() {
        fallar(limitador, "admin", "203.0.113.66", 5);
        limitador.registrarExito("admin", "198.51.100.7");

        assertThat(limitador.estaBloqueado("admin", "203.0.113.66")).isPositive();
    }

    @Test
    void elUsernameEnDistintoCaseCuentaComoElMismo() {
        for (int i = 0; i < 5; i++) {
            limitador.registrarFallo(i % 2 == 0 ? "Admin" : "  admin  ", "3.3.3.3");
        }
        assertThat(limitador.estaBloqueado("ADMIN", "3.3.3.3")).isPositive();

        limitador.registrarExito("Admin", "3.3.3.3");
        assertThat(limitador.estaBloqueado("admin", "3.3.3.3")).isZero();
    }

    // ------------------------------------------------ memoria y concurrencia

    @Test
    void lasEntradasCaducadasSeEliminanAlSuperarElTope() {
        // Tope de 3 entradas: los fallos de "a" ocupan usuario, IP y par.
        LimitadorIntentosLogin pequeno = limitador(3);
        fallar(pequeno, "a", "1.1.1.1", 5);
        assertThat(pequeno.estaBloqueado("a", "1.1.1.1")).isPositive();

        // Caducan todas las entradas; un nuevo fallo dispara la purga.
        reloj.avanzar(Duration.ofMinutes(16));
        assertThat(pequeno.estaBloqueado("a", "1.1.1.1")).isZero();
        fallar(pequeno, "c", "3.3.3.3", 5);

        // Sin la purga, "c" no habría podido crear sus claves ni bloquearse.
        assertThat(pequeno.estaBloqueado("c", "3.3.3.3")).isPositive();
    }

    @Test
    void alSuperarElTopeNoSeCreanClavesDeUsuarioPeroLaIpSeSigueContando() {
        // Tope de 3 entradas: el primer fallo crea usuario, IP y par.
        LimitadorIntentosLogin tope = limitador(3);
        tope.registrarFallo("u1", "1.1.1.1");

        // Ya en el tope, "u2" no crea claves: aunque falle 5 veces no se bloquea su par...
        fallar(tope, "u2", "1.1.1.1", 5);
        assertThat(tope.estaBloqueado("u2", "2.2.2.2")).isZero();

        // ...pero la IP se sigue contando hasta su umbral (6 fallos ya; 44 más).
        fallar(tope, "u3", "1.1.1.1", 44);
        assertThat(tope.estaBloqueado("usuario-nuevo", "1.1.1.1")).isPositive();

        // Las claves existentes sí se actualizan bajo el tope: el par de u1 llega a 5.
        LimitadorIntentosLogin otro = limitador(3);
        otro.registrarFallo("u1", "9.9.9.9");
        fallar(otro, "u1", "9.9.9.9", 4);
        assertThat(otro.estaBloqueado("u1", "9.9.9.9")).isPositive();
    }

    @Test
    void laConcurrenciaDeFallosEsAtomicaContandoExactamenteLosNDeTodosLosHilos() throws Exception {
        int hilos = 8;
        LimitadorIntentosLogin atomico = new LimitadorIntentosLogin(reloj, hilos, hilos, hilos, 15, 10000);
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

            // Los tres umbrales == hilos: si un incremento se perdiera (o se
            // contara dos veces), algún contador no sería exacto y aquí fallaría.
            assertThat(atomico.estaBloqueado("concurrente", "10.1.1.1")).isPositive();
            assertThat(atomico.estaBloqueado("otro-usuario", "10.1.1.1")).isPositive();
            assertThat(atomico.estaBloqueado("concurrente", "10.9.9.9")).isPositive();
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
