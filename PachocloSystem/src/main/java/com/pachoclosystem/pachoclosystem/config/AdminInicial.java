package com.pachoclosystem.pachoclosystem.config;

import com.pachoclosystem.pachoclosystem.model.Rol;
import com.pachoclosystem.pachoclosystem.repository.IUsuarioRepository;
import com.pachoclosystem.pachoclosystem.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Crea el usuario administrador inicial al arrancar la aplicación.
 *
 * <ul>
 *   <li>{@code app.admin.username} (o la variable {@code ADMIN_USERNAME}),
 *       por defecto {@code admin}.</li>
 *   <li>{@code app.admin.password} (o la variable {@code ADMIN_PASSWORD}): si
 *       está definida se usa tal cual (mínimo 10 caracteres); si no lo está, se
 *       genera una contraseña aleatoria temporal que se muestra <strong>una
 *       sola vez</strong> en el log a nivel WARN. En ambos casos el arranque
 *       crea el administrador; solo cuando la contraseña es aleatoria la cuenta
 *       queda bloqueada (cambio de contraseña obligatorio en el primer acceso),
 *       porque esa contraseña ha quedado escrita en el log.</li>
 * </ul>
 *
 * <p>La contraseña del entorno nunca se escribe en el log, y el hash nunca se
 * loguea. Si el usuario ya existe en la base de datos, no se vuelve a crear ni
 * se modifica: {@code ADMIN_PASSWORD} solo se aplica la primera vez, al crearlo
 * (después, la contraseña se cambia desde la aplicación).</p>
 */
@Component
public class AdminInicial implements ApplicationRunner {

    public static final String PROP_USERNAME = "app.admin.username";
    public static final String PROP_PASSWORD = "app.admin.password";

    /** Alfabeto sin caracteres ambiguos (sin 0/O, 1/l/I ni o/i). */
    static final String ALFABETO_SIN_AMBIGUEDADES =
            "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    static final int LONGITUD_PASSWORD_GENERADA = 20;

    private static final Logger LOG = LoggerFactory.getLogger(AdminInicial.class);
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final UsuarioService usuarioService;
    private final IUsuarioRepository repositorio;
    private final Environment entorno;

    public AdminInicial(UsuarioService usuarioService, IUsuarioRepository repositorio, Environment entorno) {
        this.usuarioService = usuarioService;
        this.repositorio = repositorio;
        this.entorno = entorno;
    }

    @Override
    public void run(ApplicationArguments args) {
        String username = entorno.getProperty(PROP_USERNAME, "admin").trim();
        String password = entorno.getProperty(PROP_PASSWORD, "");
        boolean passwordGenerada = false;

        if (password == null || password.isBlank()) {
            password = generarPasswordAleatoria();
            passwordGenerada = true;
        } else if (password.length() < UsuarioService.LONGITUD_MINIMA_PASSWORD) {
            // La app falla al arrancar: la configuración no cumple la política.
            throw new IllegalStateException("No se puede crear el usuario administrador: la contraseña "
                    + "(app.admin.password / ADMIN_PASSWORD) debe tener al menos "
                    + UsuarioService.LONGITUD_MINIMA_PASSWORD + " caracteres.");
        }

        if (repositorio.existePorUsername(username)) {
            LOG.info("El usuario administrador '{}' ya existe; no se vuelve a crear.", username);
            return;
        }

        // Si el username de la propiedad no es válido, crearUsuario lanza
        // SolicitudInvalidaException y el arranque falla con su mensaje.
        // Con contraseña aleatoria la cuenta nace con el cambio obligatorio:
        // esa contraseña ha quedado en el log, así que debe sustituirse en el
        // primer acceso. Con la contraseña del entorno no se exige el cambio.
        usuarioService.crearUsuario(username, password, Rol.ADMIN, null, passwordGenerada);

        if (passwordGenerada) {
            LOG.warn("Se ha creado el usuario administrador '{}' con una contraseña ALEATORIA TEMPORAL: "
                            + "'{}'. Es la única vez que aparece: cópiala ahora. La cuenta nace BLOQUEADA "
                            + "y no podrá operar hasta que cambie esta contraseña en el primer acceso. "
                            + "Queda registrada en el log, por lo que solo debe usarse en desarrollo "
                            + "(define la variable de entorno ADMIN_PASSWORD para evitarlo).",
                    username, password);
        } else {
            LOG.info("Usuario administrador '{}' creado con la contraseña definida en el entorno.", username);
        }
    }

    /** Contraseña aleatoria de 20 caracteres alfanuméricos sin ambigüedades. */
    static String generarPasswordAleatoria() {
        StringBuilder password = new StringBuilder(LONGITUD_PASSWORD_GENERADA);
        for (int i = 0; i < LONGITUD_PASSWORD_GENERADA; i++) {
            password.append(ALFABETO_SIN_AMBIGUEDADES.charAt(
                    ALEATORIO.nextInt(ALFABETO_SIN_AMBIGUEDADES.length())));
        }
        return password.toString();
    }
}
