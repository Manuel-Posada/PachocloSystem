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
 * <p>Cuenta los fallos en tres espacios de claves, cada uno con su umbral y la
 * misma ventana de {@code app.login.bloqueo-minutos}:
 * <ul>
 *   <li><strong>usuario + IP</strong> ({@code app.login.max-intentos}, 5): la
 *       protección principal contra la fuerza bruta. Solo bloquea a quien falla,
 *       desde su IP: un atacante que conoce el username {@code admin} se
 *       bloquea a sí mismo, no al administrador que entra desde otra IP. Un
 *       login correcto reinicia este contador.</li>
 *   <li><strong>IP</strong> ({@code app.login.max-intentos-ip}, 50): frena a
 *       una sola fuente que prueba muchos usernames (password spraying). El
 *       umbral es alto para que una IP compartida (una oficina, un proxy) no
 *       quede bloqueada por los fallos de unos pocos. Nunca se reinicia con un
 *       éxito.</li>
 *   <li><strong>usuario</strong> desde cualquier IP
 *       ({@code app.login.max-intentos-usuario}, 100): frena la fuerza bruta
 *       distribuida contra una cuenta. Como cada par usuario + IP se corta a
 *       los 5 fallos, bloquear así una cuenta exige fallar desde al menos
 *       100 / 5 = 20 IPs distintas. Tampoco se reinicia con un éxito.</li>
 * </ul>
 * La IP es la que resuelve {@link ResolutorIpCliente}: la del socket, salvo
 * que la petición llegue de un proxy de confianza configurado.</p>
 *
 * <p>Una clave queda <strong>bloqueada</strong> al acumular su umbral de
 * fallos dentro de la ventana tras el último fallo; el bloqueo se deshace solo
 * al agotarse esa ventana. {@link #estaBloqueado(String, String)} devuelve los
 * segundos restantes del bloqueo más largo (0 si no hay ninguno) y se usa para
 * responder 429 con {@code Retry-After}; un intento durante el bloqueo
 * <em>no</em> registra un nuevo fallo y por tanto no extiende la cuenta
 * atrás.</p>
 *
 * <p>Atomicidad: todas las lecturas/escrituras de los contadores pasan por
 * {@code compute}/{@code removeIf} de {@link ConcurrentHashMap}. La memoria
 * está acotada por {@code app.login.max-entradas}: al alcanzar el tope se
 * purgan las entradas caducadas y, si aun así se sigue superando, se dejan de
 * crear claves nuevas de usuario y de usuario + IP (las existentes sí se
 * actualizan), pero la IP se cuenta siempre: inventar usernames no debe
 * desproteger nada.</p>
 */
@Component
public class LimitadorIntentosLogin {

    /** Fallos de una clave dentro de la ventana y momento del último fallo. */
    private record EstadoClave(int fallos, Instant ultimoFallo) {
    }

    /** Clave del contador principal: username normalizado e IP del cliente. */
    private record UsuarioEnIp(String usuario, String ip) {
    }

    private final Clock reloj;
    private final int maxIntentos;
    private final int maxIntentosIp;
    private final int maxIntentosUsuario;
    private final Duration ventana;
    private final int maxEntradas;

    private final Map<UsuarioEnIp, EstadoClave> porUsuarioEnIp = new ConcurrentHashMap<>();
    private final Map<String, EstadoClave> porIp = new ConcurrentHashMap<>();
    private final Map<String, EstadoClave> porUsuario = new ConcurrentHashMap<>();

    public LimitadorIntentosLogin(Clock reloj,
                                  @Value("${app.login.max-intentos:5}") int maxIntentos,
                                  @Value("${app.login.max-intentos-ip:50}") int maxIntentosIp,
                                  @Value("${app.login.max-intentos-usuario:100}") int maxIntentosUsuario,
                                  @Value("${app.login.bloqueo-minutos:15}") long bloqueoMinutos,
                                  @Value("${app.login.max-entradas:10000}") int maxEntradas) {
        if (maxIntentos < 1) {
            throw new IllegalArgumentException("app.login.max-intentos debe ser al menos 1.");
        }
        if (maxIntentosIp < maxIntentos) {
            throw new IllegalArgumentException(
                    "app.login.max-intentos-ip no puede ser menor que app.login.max-intentos.");
        }
        if (maxIntentosUsuario < maxIntentos) {
            throw new IllegalArgumentException(
                    "app.login.max-intentos-usuario no puede ser menor que app.login.max-intentos.");
        }
        if (bloqueoMinutos < 1) {
            throw new IllegalArgumentException("app.login.bloqueo-minutos debe ser al menos 1.");
        }
        if (maxEntradas < 1) {
            throw new IllegalArgumentException("app.login.max-entradas debe ser al menos 1.");
        }
        this.reloj = reloj;
        this.maxIntentos = maxIntentos;
        this.maxIntentosIp = maxIntentosIp;
        this.maxIntentosUsuario = maxIntentosUsuario;
        this.ventana = Duration.ofMinutes(bloqueoMinutos);
        this.maxEntradas = maxEntradas;
    }

    /**
     * Segundos que quedan del bloqueo más largo que afecta a este usuario desde
     * esta IP (0 si no hay ninguno). No registra ningún fallo ni extiende el
     * bloqueo.
     */
    public long estaBloqueado(String usuario, String ip) {
        Instant ahora = reloj.instant();
        String claveUsuario = normalizar(usuario);
        String claveIp = normalizarIp(ip);
        long segundos = segundosRestantes(porUsuario, claveUsuario, maxIntentosUsuario, ahora);
        segundos = Math.max(segundos, segundosRestantes(porIp, claveIp, maxIntentosIp, ahora));
        if (claveUsuario != null && claveIp != null) {
            segundos = Math.max(segundos, segundosRestantes(
                    porUsuarioEnIp, new UsuarioEnIp(claveUsuario, claveIp), maxIntentos, ahora));
        }
        return segundos;
    }

    /** Cuenta un fallo de login en el par usuario + IP, en la IP y en el usuario. */
    public void registrarFallo(String usuario, String ip) {
        Instant ahora = reloj.instant();
        String claveUsuario = normalizar(usuario);
        String claveIp = normalizarIp(ip);
        if (entradas() >= maxEntradas) {
            purgarCaducadas(ahora);
        }
        // Memoria al tope: no se crean claves nuevas de usuario ni de par (un
        // atacante puede inventar usernames), pero la IP se cuenta siempre.
        boolean alTope = entradas() >= maxEntradas;
        if (claveIp != null) {
            porIp.compute(claveIp, (clave, estado) -> incrementar(estado, ahora));
        }
        if (claveUsuario != null) {
            contar(porUsuario, claveUsuario, alTope, ahora);
            if (claveIp != null) {
                contar(porUsuarioEnIp, new UsuarioEnIp(claveUsuario, claveIp), alTope, ahora);
            }
        }
    }

    /**
     * Un login correcto reinicia SOLO el contador de ese usuario desde esa IP.
     * Los de la IP y del usuario no se tocan: un acierto no debe dar más
     * intentos a quien prueba otros usernames desde la misma IP ni a quien
     * ataca la cuenta desde otras IPs.
     */
    public void registrarExito(String usuario, String ip) {
        String claveUsuario = normalizar(usuario);
        String claveIp = normalizarIp(ip);
        if (claveUsuario != null && claveIp != null) {
            porUsuarioEnIp.remove(new UsuarioEnIp(claveUsuario, claveIp));
        }
    }

    /** Vacía todos los contadores. Para pruebas aisladas y mantenimiento. */
    public void reiniciar() {
        porUsuarioEnIp.clear();
        porIp.clear();
        porUsuario.clear();
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

    private int entradas() {
        return porUsuarioEnIp.size() + porIp.size() + porUsuario.size();
    }

    private <K> void contar(Map<K, EstadoClave> mapa, K clave, boolean alTope, Instant ahora) {
        if (alTope && !mapa.containsKey(clave)) {
            return;
        }
        mapa.compute(clave, (k, estado) -> incrementar(estado, ahora));
    }

    private <K> long segundosRestantes(Map<K, EstadoClave> mapa, K clave, int umbral, Instant ahora) {
        if (clave == null) {
            return 0;
        }
        EstadoClave estado = mapa.get(clave);
        if (estado == null || estado.fallos() < umbral || caducada(estado, ahora)) {
            return 0;
        }
        long restanteMs = estado.ultimoFallo().toEpochMilli()
                + ventana.toMillis() - ahora.toEpochMilli();
        // Ceil a segundos; siempre > 0 porque la entrada no ha caducado.
        return (restanteMs + 999) / 1000;
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
        porUsuarioEnIp.entrySet().removeIf(entrada -> caducada(entrada.getValue(), ahora));
        porIp.entrySet().removeIf(entrada -> caducada(entrada.getValue(), ahora));
        porUsuario.entrySet().removeIf(entrada -> caducada(entrada.getValue(), ahora));
    }

    /** Username normalizado como {@link Usuario#normalizarUsername}; {@code null} si queda vacío. */
    private static String normalizar(String username) {
        String normalizado = Usuario.normalizarUsername(username);
        return normalizado == null || normalizado.isEmpty() ? null : normalizado;
    }

    private static String normalizarIp(String ip) {
        return ip == null || ip.isBlank() ? null : ip.trim();
    }
}
