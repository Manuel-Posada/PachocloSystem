package com.pachoclosystem.medicamentos.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Validaciones estrictas del perfil {@code prod} ({@code SPRING_PROFILES_ACTIVE=prod}).
 * Sin el perfil (desarrollo y tests) no actúa y todo funciona como siempre.
 *
 * <p>Se ejecuta antes de crear cualquier bean (también antes de conectar con la
 * base de datos) y, si algo falta, el servicio no arranca. El error enumera todos
 * los problemas a la vez con el nombre de cada variable, nunca su valor.</p>
 *
 * <ul>
 *   <li>{@code MEDICAMENTOS_API_KEY} de al menos {@value #LONGITUD_MINIMA_CLAVE}
 *       caracteres: en producción la API nunca queda sin clave de servicio.</li>
 *   <li>{@code MEDICAMENTOS_DB_URL} que no apunte a localhost y
 *       {@code MEDICAMENTOS_DB_PASSWORD} definida.</li>
 * </ul>
 */
@Component
@Profile(ValidacionProduccion.PERFIL)
public class ValidacionProduccion implements BeanFactoryPostProcessor, EnvironmentAware {

    public static final String PERFIL = "prod";
    static final int LONGITUD_MINIMA_CLAVE = 32;

    private Environment entorno;

    @Override
    public void setEnvironment(Environment entorno) {
        this.entorno = entorno;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory fabrica) {
        validar(entorno);
    }

    /** Lanza {@link IllegalStateException} con todos los problemas, o nada si no hay ninguno. */
    static void validar(Environment entorno) {
        List<String> errores = new ArrayList<>();
        String claveServicio = entorno.getProperty("medicamentos.api-key");
        if (vacia(claveServicio) || claveServicio.trim().length() < LONGITUD_MINIMA_CLAVE) {
            errores.add("MEDICAMENTOS_API_KEY es obligatoria, con al menos " + LONGITUD_MINIMA_CLAVE
                    + " caracteres, y debe ser la misma en PachocloSystem.");
        }
        if (apuntaALocalhost(entorno.getProperty("spring.datasource.url"))) {
            errores.add("MEDICAMENTOS_DB_URL debe ser la base de datos de producción, no localhost.");
        }
        if (vacia(entorno.getProperty("spring.datasource.password"))) {
            errores.add("MEDICAMENTOS_DB_PASSWORD es obligatoria.");
        }
        if (!errores.isEmpty()) {
            throw new IllegalStateException("Configuración de producción incompleta (perfil prod): "
                    + String.join(" ", errores));
        }
    }

    private static boolean vacia(String valor) {
        return valor == null || valor.isBlank();
    }

    /** Sin valor, o con host localhost / 127.x / ::1 (también dentro de una URL JDBC). */
    static boolean apuntaALocalhost(String url) {
        if (vacia(url)) {
            return true;
        }
        String sinJdbc = url.trim().toLowerCase(Locale.ROOT).replaceFirst("^jdbc:", "");
        String host;
        try {
            host = URI.create(sinJdbc).getHost();
        } catch (IllegalArgumentException malformada) {
            return false;
        }
        if (host == null) {
            return false;
        }
        String sinCorchetes = host.replace("[", "").replace("]", "");
        return sinCorchetes.equals("localhost") || sinCorchetes.startsWith("127.") || sinCorchetes.equals("::1");
    }
}
