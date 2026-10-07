package com.pachoclosystem.pachoclosystem.security;

import com.pachoclosystem.pachoclosystem.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Límite de intentos fallidos de login en memoria (sin base de datos y sin
 * estado global estático).
 *
 * <p>Mantiene dos espacios de claves independientes:
 * <ul>
 *   <li><strong>usuario</strong>: la clave es el username normalizado igual que
 *       {@link Usuario#normalizarUsername} (minúsculas, sin espacios alrededor),
 *       de modo que «Admin» y «admin» cuenten como la misma clave. Un login
 *       correcto reinicia <em>solo</em> este contador.</li>
 *   <li><strong>ip</strong>: la clave es la IP de {@code getRemoteAddr()}; es la
 *       protección principal porque no la puede inventar un atacante y
 *       <em>nunca</em> se reinicia con los éxitos de un usuario.</li>
 * </ul>
 * Un usuario o una IP quedan <strong>bloqueados</strong> al acumular
 * {@code app.login.max-intentos} fallos dentro de la ventana de
 * {@code app.login.bloqueo-minutos} tras el último fallo; el bloqueo se deshace
 * solo al agotarse esa ventana. {@link #estaBloqueado(String, String)} devuelve
 * los segundos restantes (0 si no hay bloqueo) y se usa para responder 429 con
 * {@code Retry-After}; un intento durante el bloqueo <em>no</em> registra un
 * nuevo fallo y por tanto no extiende la cuenta atrás.</p>
 *
 * <p>Atomicidad: todas las lecturas/escrituras del contador pasan por
 * {@code compute}/{@code removeIf} de {@link ConcurrentHashMap}; nunca se lee,
 * comprueba y escribe sin una operación atómica. La memoria está acotada por
 * {@code app.login.max-entradas}: al alcanzar el tope se purgan las entradas
 * caducadas (ventana de conteo) y, si aun así se sigue superando, se deja de
 * crear claves de <em>usuario</em> nuevas (las ya existentes sí se actualizan)
 * pero la <em>IP</em> se sigue contando siempre, porque inventar usernames no
 * debe impedir que la IP quede protegida.</p>
 */
@Component
public class LimitadorIntentosLogin {

    /** Fallos de una clave dentro de la ventana y momento del último fallo. */
    private record EstadoClave(int fallos, Instant ultimoFallo) {
    }

    private final Clock reloj;
    private final int maxIntentos;
    private final Duration ventana;
    private final int maxEntradas;

    private final Map<String, EstadoClave> porUsuario = new ConcurrentHashMap<>();
    private final Map<String, EstadoClave> porIp = new ConcurrentHashMap<>();

    public LimitadorIntentosLogin(Clock reloj,
                                  @Value("${app.login.max-intentos:5}") int maxIntentos,
                                  @Value("${app.login.bloqueo-minutos:15}") long bloqueoMinutos,
                                  @Value("${app.login.max-entradas:10000}") int maxEntradas) {
        if (maxIntentos < 1) {
            throw new IllegalArgumentException("app.login.max-intentos debe ser al menos 1.");
        }
        if (bloqueoMinutos < 1) {
            throw new IllegalArgumentException("app.login.bloqueo-minutos debe ser al menos 1.");
        }
        if (maxEntradas < 1) {
            throw new IllegalArgumentException("app.login.max-entradas debe ser al menos 1.");
        }
        this.reloj = reloj;
        this.maxIntentos = maxIntentos;
        this.ventana = Duration.ofMinutes(bloqueoMinutos);
        this.maxEntradas = maxEntradas;
    }

    /**
     * Segundos que quedan de bloqueo para el usuario o para la IP (0 si ninguno
     * está bloqueado). No registra ningún fallo ni extiende el bloqueo.
     */
    public long estaBloqueado(String usuario, String ip) {
        Instant ahora = reloj.instant();
        long segundosUsuario = segundosRestantes(porUsuario, normalizar(usuario), ahora);
        long segundosIp = segundosRestantes(porIp, ip, ahora);
        return Math.max(segundosUsuario, segundosIp);
    }

    /** Cuenta un fallo de login en el usuario y en la IP. */
    public void registrarFallo(String usuario, String ip) {
        Instant ahora = reloj.instant();
        String claveUsuario = normalizar(usuario);
        if (porUsuario.size() + porIp.size() >= maxEntradas) {
            purgarCaducadas(ahora);
        }
        if (porUsuario.size() + porIp.size() >= maxEntradas) {
            // Memoria al tope: se actualizan las claves de usuario existentes
            // pero NO se crean nuevas; la IP se cuenta siempre.
            registrarFalloIp(ip, ahora);
            if (claveUsuario != null && porUsuario.containsKey(claveUsuario)) {
                porUsuario.compute(claveUsuario, (clave, estado) -> incrementar(estado, ahora));
            }
            return;
        }
        registrarFalloUsuario(usuario, ahora);
        registrarFalloIp(ip, ahora);
    }

    /**
     * Reinicia SOLO el contador del usuario (un login correcto). El contador de
     * la IP no se toca: otros usuarios desde esa misma IP deben seguir
     * acumulando sus fallos.
     */
    public void registrarExito(String usuario) {
        String claveUsuario = normalizar(usuario);
        if (claveUsuario != null) {
            porUsuario.remove(claveUsuario);
        }
    }

    /** Vacía ambos espacios de claves. Para pruebas aisladas y mantenimiento. */
    public void reiniciar() {
        porUsuario.clear();
        porIp.clear();
    }

    /**
     * Username seguro para LOS LOGS: normalizado a minúsculas, sin espacios
     * alrededor, sin caracteres de control (evita inyección de saltos de línea)
     * y truncado a 30 caracteres. La contraseña nunca se loguea.
     */
    public static String sanearUsername(String username) {
        String normalizado = Usuario.normalizarUsername(username);
        if (normalizado == null) {
            return "";
        }
        StringBuilder limpieza = new StringBuilder(normalizado.length());
        for (int i = 0; i < normalizado.length() && limpieza.length() < 30; i++) {
            char caracter = normalizado.charAt(i);
            if (Character.isISOControl(caracter)) {
                continue;
            }
            limpieza.append(caracter);
        }
        return limpieza.toString();
    }

    private long segundosRestantes(Map<String, EstadoClave> mapa, String clave, Instant ahora) {
        if (clave == null || clave.isBlank()) {
            return 0;
        }
        EstadoClave estado = mapa.get(clave);
        if (estado == null || estado.fallos() < maxIntentos || caducada(estado, ahora)) {
            return 0;
        }
        long restanteMs = estado.ultimoFallo().toEpochMilli()
                + ventana.toMillis() - ahora.toEpochMilli();
        // Ceil a segundos; siempre > 0 porque la entrada no ha caducado.
        return (restanteMs + 999) / 1000;
    }

    private void registrarFalloUsuario(String usuario, Instant ahora) {
        String clave = normalizar(usuario);
        if (clave == null) {
            return;
        }
        porUsuario.compute(clave, (k, estado) -> incrementar(estado, ahora));
    }

    private void registrarFalloIp(String ip, Instant ahora) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        porIp.compute(ip, (k, estado) -> incrementar(estado, ahora));
    }

    /** Devuelve el estado incrementado; si no existía o caducó, arranca en 1. */
    private EstadoClave incrementar(EstadoClave estado, Instant ahora) {
        if (estado == null || caducada(estado, ahora)) {
            return new EstadoClave(1, ahora);
        }
        return new EstadoClave(estado.fallos() + 1, ahora);
    }

    private boolean caducada(EstadoClave estado, Instant ahora) {
        return !estado.ultimoFallo().plus(ventana).isAfter(ahora);
    }

    private void purgarCaducadas(Instant ahora) {
        porUsuario.entrySet().removeIf(entrada -> caducada(entrada.getValue(), ahora));
        porIp.entrySet().removeIf(entrada -> caducada(entrada.getValue(), ahora));
    }

    private static String normalizar(String username) {
        return Usuario.normalizarUsername(username);
    }
}