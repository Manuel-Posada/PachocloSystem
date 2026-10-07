package com.pachoclosystem.pachoclosystem.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.pachoclosystem.pachoclosystem.exception.SolicitudInvalidaException;
import com.pachoclosystem.pachoclosystem.model.Rol;
import com.pachoclosystem.pachoclosystem.model.Usuario;
import com.pachoclosystem.pachoclosystem.repository.TrabajadorRepositoryEnMemoria;
import com.pachoclosystem.pachoclosystem.repository.UsuarioRepositoryEnMemoria;
import com.pachoclosystem.pachoclosystem.service.TrabajadorService;
import com.pachoclosystem.pachoclosystem.service.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Comportamiento del arranque del administrador inicial, sin arrancar la
 * aplicación completa: se inyecta un entorno de prueba con las mismas
 * propiedades (app.admin.password = ADMIN_PASSWORD) y se capturan los logs.
 */
class AdminInicialTest {

    private UsuarioRepositoryEnMemoria repositorio;
    private UsuarioService servicio;
    private MockEnvironment entorno;
    private AdminInicial adminInicial;
    private ListAppender<ILoggingEvent> registro;

    @BeforeEach
    void preparar() {
        repositorio = new UsuarioRepositoryEnMemoria();
        servicio = new UsuarioService(repositorio,
                new TrabajadorService(new TrabajadorRepositoryEnMemoria(), new UsuarioRepositoryEnMemoria()),
                new BCryptPasswordEncoder());
        entorno = new MockEnvironment();
        adminInicial = new AdminInicial(servicio, repositorio, entorno);

        registro = new ListAppender<>();
        registro.start();
        ((Logger) LoggerFactory.getLogger(AdminInicial.class)).addAppender(registro);
    }

    @AfterEach
    void soltarRegistro() {
        ((Logger) LoggerFactory.getLogger(AdminInicial.class)).detachAppender(registro);
    }

    @Test
    void conPasswordDeEntornoCreaElAdminSinLoguearLaPassword() {
        String passwordDeEntorno = passwordDeEntorno();
        entorno.setProperty(AdminInicial.PROP_USERNAME, "admin.pruebas");
        entorno.setProperty(AdminInicial.PROP_PASSWORD, passwordDeEntorno);

        ejecutarArranque();

        Usuario admin = servicio.buscarPorUsername("admin.pruebas");
        assertThat(admin.getRol()).isEqualTo(Rol.ADMIN);
        assertThat(admin.getIdTrabajador()).isNull();
        assertThat(admin.getPasswordHash()).isNotEqualTo(passwordDeEntorno);
        assertThat(new BCryptPasswordEncoder().matches(passwordDeEntorno, admin.getPasswordHash())).isTrue();
        // Con contraseña definida en el entorno, el primer acceso no exige cambio.
        assertThat(admin.isDebeCambiarPassword()).isFalse();

        // Ni la contraseña del entorno ni el hash aparecen en ningún log de arranque.
        assertThat(mensajesDeLog()).noneMatch(m -> m.contains(passwordDeEntorno));
        assertThat(mensajesDeLog()).noneMatch(m -> m.contains(admin.getPasswordHash()));
        assertThat(warnings()).isEmpty();
    }

    @Test
    void sinPasswordGeneraUnaAleatoriaDeVeinteCaracteresYLogueaUnSoloWarn() {
        entorno.setProperty(AdminInicial.PROP_USERNAME, "admin.temporal");
        // Sin app.admin.password: el entorno no define la contraseña.

        ejecutarArranque();

        List<ILoggingEvent> avisos = warnings();
        assertThat(avisos).as("exactamente un WARN con la contraseña temporal").hasSize(1);
        assertThat(avisos.get(0).getFormattedMessage())
                .contains("TEMPORAL")
                .contains("admin.temporal");

        // La contraseña generada se extrae del propio mensaje (argumento 2).
        String passwordGenerada = String.valueOf(avisos.get(0).getArgumentArray()[1]);
        assertThat(passwordGenerada).hasSize(AdminInicial.LONGITUD_PASSWORD_GENERADA);
        assertThat(passwordGenerada.chars().allMatch(
                c -> AdminInicial.ALFABETO_SIN_AMBIGUEDADES.indexOf(c) >= 0)).isTrue();
        assertThat(passwordGenerada).matches("[a-zA-Z2-9]{20}");

        Usuario admin = servicio.buscarPorUsername("admin.temporal");
        assertThat(new BCryptPasswordEncoder().matches(passwordGenerada, admin.getPasswordHash())).isTrue();
        assertThat(admin.getPasswordHash()).isNotEqualTo(passwordGenerada);
        // Contraseña aleatoria vista en el log: la cuenta nace bloqueada y exige
        // cambiarla en el primer acceso.
        assertThat(admin.isDebeCambiarPassword()).isTrue();

        // El hash nunca aparece en los logs.
        assertThat(mensajesDeLog()).noneMatch(m -> m.contains(admin.getPasswordHash()));
    }

    @Test
    void passwordDeEntornoCortaFallaElArranqueConMensajeClaro() {
        String passwordCorta = "corta123";
        entorno.setProperty(AdminInicial.PROP_USERNAME, "admin.pruebas");
        entorno.setProperty(AdminInicial.PROP_PASSWORD, passwordCorta);

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(this::ejecutarArranque)
                .withMessageContaining("al menos 10 caracteres")
                .satisfies(excepcion -> assertThat(excepcion.getMessage()).doesNotContain(passwordCorta));

        assertThat(repositorio.listarTodos()).isEmpty();
        assertThat(warnings()).isEmpty();
    }

    @Test
    void passwordDeEntornoDeMasDe72BytesFallaElArranqueSinMostrarla() {
        String passwordLarga = "ñ".repeat(37);
        entorno.setProperty(AdminInicial.PROP_USERNAME, "admin.pruebas");
        entorno.setProperty(AdminInicial.PROP_PASSWORD, passwordLarga);

        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(this::ejecutarArranque)
                .withMessageContaining("72 bytes")
                .satisfies(excepcion -> assertThat(excepcion.getMessage()).doesNotContain(passwordLarga));

        assertThat(repositorio.listarTodos()).isEmpty();
    }

    @Test
    void passwordDeEntornoConEspaciosEnBlancoSeTrataComoNoDefinida() {
        entorno.setProperty(AdminInicial.PROP_USERNAME, "admin.temporal");
        entorno.setProperty(AdminInicial.PROP_PASSWORD, "   ");

        ejecutarArranque();

        assertThat(repositorio.buscarPorUsername("admin.temporal")).isNotNull();
        assertThat(warnings()).hasSize(1);
    }

    @Test
    void siElUsuarioYaExisteNoSeVuelveACrearNiSeLogueaLaPassword() {
        servicio.crearUsuario("admin.pruebas", "otra-password-larga", Rol.ADMIN, null);
        entorno.setProperty(AdminInicial.PROP_USERNAME, "admin.pruebas");
        entorno.setProperty(AdminInicial.PROP_PASSWORD, passwordDeEntorno());

        ejecutarArranque();

        assertThat(repositorio.listarTodos()).hasSize(1);
        assertThat(warnings()).isEmpty();
        assertThat(mensajesDeLog()).anyMatch(m -> m.contains("ya existe"));
    }

    @Test
    void passwordGeneradaCumpleLaPoliticaDeDiezCaracteres() {
        String generada = AdminInicial.generarPasswordAleatoria();

        assertThat(generada).hasSize(20);
        assertThat(generada.length()).isGreaterThanOrEqualTo(UsuarioService.LONGITUD_MINIMA_PASSWORD);
        assertThat(generada).doesNotContain("0", "O", "1", "l", "I");
    }

    private void ejecutarArranque() {
        adminInicial.run(new DefaultApplicationArguments());
    }

    private List<String> mensajesDeLog() {
        return registro.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    private List<ILoggingEvent> warnings() {
        return registro.list.stream()
                .filter(e -> e.getLevel().isGreaterOrEqual(ch.qos.logback.classic.Level.WARN))
                .toList();
    }

    /** Password de prueba generada en memoria: nunca se escribe en el repositorio. */
    private static String passwordDeEntorno() {
        return "pw-" + Long.toHexString(System.nanoTime()) + "-entorno";
    }
}
