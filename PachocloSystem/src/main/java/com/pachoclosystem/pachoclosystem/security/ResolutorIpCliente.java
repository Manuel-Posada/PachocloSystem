package com.pachoclosystem.pachoclosystem.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * IP del cliente para el límite de intentos de login.
 *
 * <p>Por defecto es {@code getRemoteAddr()}, la del socket, y
 * {@code X-Forwarded-For} se ignora: cualquiera puede escribir esa cabecera.
 * Solo si la petición llega de un proxy de confianza
 * ({@code app.login.proxies-confiables}, IPs exactas tal como las da
 * {@code getRemoteAddr()}) se lee {@code X-Forwarded-For} de derecha a
 * izquierda saltando los proxies de confianza: la primera IP que no es de
 * confianza es la que vio el último proxy propio, y es la del cliente. Lo que
 * está más a la izquierda lo pudo escribir el propio cliente y no se usa.</p>
 *
 * <p>Si esa entrada no tiene forma de IP, o la cabecera falta o solo contiene
 * proxies de confianza, se usa {@code getRemoteAddr()}. Nunca se resuelven
 * nombres por DNS.</p>
 */
@Component
public class ResolutorIpCliente {

    static final String CABECERA = "X-Forwarded-For";

    private static final Pattern IPV4 = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");
    private static final Pattern IPV6 = Pattern.compile("^[0-9a-fA-F:.]*:[0-9a-fA-F:.]*$");

    private final Set<String> proxiesConfiables;

    public ResolutorIpCliente(@Value("${app.login.proxies-confiables:}") String configuracion) {
        Set<String> proxies = new LinkedHashSet<>();
        if (configuracion != null) {
            for (String parte : configuracion.split(",")) {
                String ip = parte.trim();
                if (ip.isEmpty()) {
                    continue;
                }
                if (!esIp(ip)) {
                    throw new IllegalStateException("app.login.proxies-confiables "
                            + "(APP_LOGIN_PROXIES_CONFIABLES) solo admite IPs literales separadas por "
                            + "comas; no es válido: '" + sanear(ip) + "'.");
                }
                proxies.add(ip.toLowerCase(Locale.ROOT));
            }
        }
        this.proxiesConfiables = Collections.unmodifiableSet(proxies);
    }

    /** IP del cliente de la petición (ver la descripción de la clase). */
    public String ipDe(HttpServletRequest peticion) {
        String remota = peticion.getRemoteAddr();
        if (remota == null || !esConfiable(remota)) {
            return remota;
        }
        List<String> saltos = new ArrayList<>();
        var cabeceras = peticion.getHeaders(CABECERA);
        while (cabeceras != null && cabeceras.hasMoreElements()) {
            for (String salto : cabeceras.nextElement().split(",")) {
                saltos.add(salto.trim());
            }
        }
        for (int i = saltos.size() - 1; i >= 0; i--) {
            String salto = saltos.get(i);
            if (esConfiable(salto)) {
                continue;
            }
            return esIp(salto) ? salto : remota;
        }
        return remota;
    }

    private boolean esConfiable(String ip) {
        return proxiesConfiables.contains(ip.toLowerCase(Locale.ROOT));
    }

    private static boolean esIp(String valor) {
        return valor.length() <= 45 && (IPV4.matcher(valor).matches() || IPV6.matcher(valor).matches());
    }

    /** Sin caracteres de control, para que el mensaje de arranque no inyecte líneas. */
    private static String sanear(String valor) {
        StringBuilder limpio = new StringBuilder(valor.length());
        for (char caracter : valor.toCharArray()) {
            limpio.append(Character.isISOControl(caracter) ? '?' : caracter);
        }
        return limpio.toString();
    }
}
