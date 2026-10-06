package com.pachoclosystem.pachoclosystem.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.pachoclosystem.pachoclosystem.model.Rol;
import com.pachoclosystem.pachoclosystem.model.Usuario;
import com.pachoclosystem.pachoclosystem.repository.IUsuarioRepository;
import com.pachoclosystem.pachoclosystem.service.UsuarioService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Arranque real de la aplicación con una contraseña de administrador
 * proporcionada por el entorno (propiedad de prueba): el admin se crea, su
 * contraseña coincide con la del entorno y ni la contraseña ni el hash aparecen
 * en los logs de arranque.
 *
 * <p>El registro de logs se abre en {@code @DynamicPropertySource}, que se
 * ejecuta durante la preparación del contexto y, por tanto, antes de que
 * {@link AdminInicial} emita sus logs de arranque.</p>
 */
@SpringBootTest
class AdminArranqueTest {

    /** Contraseña de prueba generada en memoria: nunca se escribe en el repo. */
    private static final String PASSWORD_DE_PRUEBA = "pwd-" + Long.toHexString(System.nanoTime()) + "-x";

    private static final ListAppender<ILoggingEvent> REGISTRO = new ListAppender<>();

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private IUsuarioRepository repositorio;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private Environment entorno;

    @AfterAll
    static void cerrarRegistroDeLogs() {
        ((Logger) LoggerFactory.getLogger(AdminInicial.class)).detachAppender(REGISTRO);
        REGISTRO.stop();
    }

    @DynamicPropertySource
    static void passwordDePrueba(DynamicPropertyRegistry registro) {
        // El registro de logs se abre AQUÍ (fase de preparación del contexto, justo
        // antes de refrescarlo) para capturar los logs que emite AdminInicial al
        // arrancar: si se hiciera en @BeforeAll, Spring Boot podría reiniciar la
        // configuración de logback y quitar el apéndice.
        if (!REGISTRO.isStarted()) {
            REGISTRO.start();
            ((Logger) LoggerFactory.getLogger(AdminInicial.class)).addAppender(REGISTRO);
        }
        registro.add(AdminInicial.PROP_USERNAME, () -> "admin.arranque");
        registro.add(AdminInicial.PROP_PASSWORD, () -> PASSWORD_DE_PRUEBA);
    }

    @Test
    void elAdminSeCreaAlArrancarYSuPasswordEsLaDelEntorno() {
        assertThat(entorno.getProperty(AdminInicial.PROP_PASSWORD)).isEqualTo(PASSWORD_DE_PRUEBA);

        Usuario admin = usuarioService.buscarPorUsername("admin.arranque");

        assertThat(admin.getRol()).isEqualTo(Rol.ADMIN);
        assertThat(admin.getIdTrabajador()).isNull();
        assertThat(admin.isActivo()).isTrue();
        assertThat(admin.getPasswordHash()).isNotEqualTo(PASSWORD_DE_PRUEBA).startsWith("$2");
        assertThat(encoder.matches(PASSWORD_DE_PRUEBA, admin.getPasswordHash())).isTrue();
        assertThat(repositorio.listarTodos()).containsExactly(admin);
    }

    @Test
    void niLosLogsDeArranqueNiElToStringContienenLaPasswordNiElHash() {
        Usuario admin = usuarioService.buscarPorUsername("admin.arranque");
        List<String> mensajes = REGISTRO.list.stream().map(ILoggingEvent::getFormattedMessage).toList();

        assertThat(mensajes).isNotEmpty();
        // Ni la contraseña del entorno ni el hash aparecen en los logs de arranque.
        assertThat(mensajes).noneMatch(m -> m.contains(PASSWORD_DE_PRUEBA));
        assertThat(mensajes).noneMatch(m -> m.contains(admin.getPasswordHash()));
        // Con contraseña del entorno no se emite el WARN de contraseña temporal.
        assertThat(REGISTRO.list)
                .noneMatch(e -> e.getLevel().isGreaterOrEqual(ch.qos.logback.classic.Level.WARN));

        assertThat(admin.toString())
                .doesNotContain(admin.getPasswordHash())
                .doesNotContain("passwordHash");
    }
}
