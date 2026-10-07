package com.pachoclosystem.pachoclosystem.service;

import com.pachoclosystem.pachoclosystem.exception.NotFoundException;
import com.pachoclosystem.pachoclosystem.exception.SolicitudInvalidaException;
import com.pachoclosystem.pachoclosystem.model.Doctor;
import com.pachoclosystem.pachoclosystem.model.Enfermero;
import com.pachoclosystem.pachoclosystem.model.NivelExperiencia;
import com.pachoclosystem.pachoclosystem.model.Rol;
import com.pachoclosystem.pachoclosystem.model.Usuario;
import com.pachoclosystem.pachoclosystem.repository.TrabajadorRepositoryImpl;
import com.pachoclosystem.pachoclosystem.repository.UsuarioRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Reglas de gestión de usuarios y contraseñas, sin contexto Spring: creación con
 * cambio obligatorio, política de contraseña (10 caracteres / 72 bytes),
 * cambio de rol, cambio de estado, cambio/reset de contraseña y el invariante
 * «siempre queda al menos un ADMIN activo» (incluida su concurrencia).
 */
class UsuarioServiceGestionTest {

    private static final String PASSWORD_VALIDA = "clave-de-pruebas-10";

    private UsuarioRepositoryImpl repositorio;
    private TrabajadorService trabajadorService;
    private PasswordEncoder encoder;
    private UsuarioService servicio;

    @BeforeEach
    void preparar() {
        repositorio = new UsuarioRepositoryImpl();
        trabajadorService = new TrabajadorService(new TrabajadorRepositoryImpl(),
                new UsuarioRepositoryImpl());
        encoder = new BCryptPasswordEncoder();
        servicio = new UsuarioService(repositorio, trabajadorService, encoder);
    }

    // ------------------------------------------------------- creación con flag

    @Test
    void elAltaPorApiPuedeExigirElCambioDePasswordManteniendoLaVersionInicial() {
        Usuario usuario = servicio.crearUsuario("ana.torres", PASSWORD_VALIDA, Rol.ADMIN, null, true);

        assertThat(usuario.isDebeCambiarPassword()).isTrue();
        assertThat(usuario.getVersionToken()).isZero();
        assertThat(encoder.matches(PASSWORD_VALIDA, usuario.getPasswordHash())).isTrue();
    }

    @Test
    void laSobrecargaDeCuatroArgumentosNoExigeCambio() {
        Usuario usuario = servicio.crearUsuario("ana.torres", PASSWORD_VALIDA, Rol.ADMIN, null);

        assertThat(usuario.isDebeCambiarPassword()).isFalse();
        assertThat(usuario.getVersionToken()).isZero();
    }

    // ---------------------------------------------------- política de password

    @Test
    void passwordDemasiadoLargaEnBytesSeRechazaSinRevelarla() {
        // 73 caracteres ASCII => 73 bytes (> 72).
        String passwordLarga = "a".repeat(73);

        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.crearUsuario("ana.torres", passwordLarga, Rol.ADMIN, null))
                .withMessage("La contraseña no puede superar los 72 bytes (caracteres UTF-8).")
                .satisfies(ex -> assertThat(ex.getMessage()).doesNotContain(passwordLarga));
    }

    @Test
    void passwordMultibyteSeMideEnBytesNoEnCaracteres() {
        // 36 'ñ' son 36 caracteres pero 72 bytes: justo en el límite, válida.
        String justoEnElLimite = "ñ".repeat(36);
        assertThat(justoEnElLimite.length()).isEqualTo(36);
        servicio.crearUsuario("ana.torres", justoEnElLimite, Rol.ADMIN, null);

        // 37 'ñ' son 74 bytes: se rechaza por el máximo, no por longitud.
        String pasadaDeBytes = "ñ".repeat(37);
        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.crearUsuario("otra.persona", pasadaDeBytes, Rol.ADMIN, null))
                .withMessageContaining("72 bytes")
                .satisfies(ex -> assertThat(ex.getMessage()).doesNotContain(pasadaDeBytes));
    }

    @Test
    void passwordExactamenteSetentaYDosBytesAsciiEsValida() {
        String password = "b".repeat(72);

        Usuario usuario = servicio.crearUsuario("ana.torres", password, Rol.ADMIN, null);

        assertThat(encoder.matches(password, usuario.getPasswordHash())).isTrue();
    }

    // ------------------------------------------------------------ listar/buscar

    @Test
    void listarDevuelveLosUsuariosOrdenadosPorId() {
        servicio.crearUsuario("admin.uno", PASSWORD_VALIDA, Rol.ADMIN, null);
        servicio.crearUsuario("admin.dos", PASSWORD_VALIDA, Rol.ADMIN, null);

        assertThat(servicio.listar())
                .extracting(Usuario::getIdUsuario)
                .containsExactly("USR-0001", "USR-0002");
    }

    @Test
    void buscarPorIdInexistenteLanzaNotFound() {
        assertThatExceptionOfType(NotFoundException.class)
                .isThrownBy(() -> servicio.buscarPorId("USR-9999"))
                .withMessage("No se encontró el usuario USR-9999.");
    }

    // --------------------------------------------------------- cambio de rol

    @Test
    void cambiarRolDoctorAEnfermeroActualizaVinculoEIndice() {
        Doctor doctor = (Doctor) trabajadorService.registrarTrabajador(
                "Carlos Mena", "Doctor", "Cardiologia", null);
        Enfermero enfermero = (Enfermero) trabajadorService.registrarTrabajador(
                "Lucia Vidal", "Enfermero", null, NivelExperiencia.NOVATO);
        Usuario usuario = servicio.crearUsuario("carlos.mena", PASSWORD_VALIDA, Rol.DOCTOR,
                doctor.getIdTrabajador());

        servicio.cambiarRol(usuario.getIdUsuario(), Rol.ENFERMERO, enfermero.getIdTrabajador());

        assertThat(usuario.getRol()).isEqualTo(Rol.ENFERMERO);
        assertThat(usuario.getIdTrabajador()).isEqualTo(enfermero.getIdTrabajador());
        assertThat(repositorio.buscarPorIdTrabajador(doctor.getIdTrabajador())).isNull();
        assertThat(repositorio.buscarPorIdTrabajador(enfermero.getIdTrabajador())).isSameAs(usuario);
    }

    @Test
    void cambiarRolAAdminExigeNoTenerTrabajador() {
        Doctor doctor = (Doctor) trabajadorService.registrarTrabajador(
                "Carlos Mena", "Doctor", "Cardiologia", null);
        Usuario usuario = servicio.crearUsuario("carlos.mena", PASSWORD_VALIDA, Rol.DOCTOR,
                doctor.getIdTrabajador());

        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.cambiarRol(usuario.getIdUsuario(), Rol.ADMIN,
                        doctor.getIdTrabajador()))
                .withMessage("El usuario administrador no puede estar vinculado a un trabajador.");
    }

    @Test
    void cambiarRolAOtroRolExigeTrabajadorExistenteYDelTipoCorrecto() {
        Usuario admin = servicio.crearUsuario("ana.torres", PASSWORD_VALIDA, Rol.ADMIN, null);
        Doctor doctor = (Doctor) trabajadorService.registrarTrabajador(
                "Carlos Mena", "Doctor", "Cardiologia", null);

        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.cambiarRol(admin.getIdUsuario(), Rol.DOCTOR, null))
                .withMessage("El usuario con rol DOCTOR debe estar vinculado a un trabajador.");

        assertThatExceptionOfType(NotFoundException.class)
                .isThrownBy(() -> servicio.cambiarRol(admin.getIdUsuario(), Rol.DOCTOR, "DOC-9999"))
                .withMessage("No se encontró el trabajador DOC-9999.");

        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.cambiarRol(admin.getIdUsuario(), Rol.ENFERMERO,
                        doctor.getIdTrabajador()))
                .withMessage("El trabajador DOC-0001 no es un Enfermero.");
    }

    @Test
    void cambiarRolAUnTrabajadorYaOcupadoPorOtroUsuarioSeRechaza() {
        Doctor doctor = (Doctor) trabajadorService.registrarTrabajador(
                "Carlos Mena", "Doctor", "Cardiologia", null);
        Doctor otroDoctor = (Doctor) trabajadorService.registrarTrabajador(
                "Jorge Ruiz", "Doctor", "Neurologia", null);
        servicio.crearUsuario("carlos.mena", PASSWORD_VALIDA, Rol.DOCTOR, doctor.getIdTrabajador());
        Usuario segundo = servicio.crearUsuario("jorge.ruiz", PASSWORD_VALIDA, Rol.DOCTOR,
                otroDoctor.getIdTrabajador());

        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.cambiarRol(segundo.getIdUsuario(), Rol.DOCTOR,
                        doctor.getIdTrabajador()))
                .withMessage("El trabajador DOC-0001 ya tiene un usuario.");
    }

    @Test
    void cambiarRolDeUsuarioInexistenteLanzaNotFound() {
        assertThatExceptionOfType(NotFoundException.class)
                .isThrownBy(() -> servicio.cambiarRol("USR-9999", Rol.ADMIN, null))
                .withMessage("No se encontró el usuario USR-9999.");
    }

    // ------------------------------------------------------- cambio de estado

    @Test
    void desactivarYReactivarEsIdempotenteYRevierteElEstado() {
        // Dos admins para poder desactivar a uno sin romper el invariante.
        Usuario adminUno = servicio.crearUsuario("admin.uno", PASSWORD_VALIDA, Rol.ADMIN, null);
        Usuario adminDos = servicio.crearUsuario("admin.dos", PASSWORD_VALIDA, Rol.ADMIN, null);
        String actor = adminUno.getIdUsuario();

        servicio.cambiarEstado(adminDos.getIdUsuario(), false, actor);
        assertThat(adminDos.isActivo()).isFalse();

        // Desactivar de nuevo no es error.
        servicio.cambiarEstado(adminDos.getIdUsuario(), false, actor);
        assertThat(adminDos.isActivo()).isFalse();

        servicio.cambiarEstado(adminDos.getIdUsuario(), true, actor);
        assertThat(adminDos.isActivo()).isTrue();

        // Reactivar de nuevo no es error.
        servicio.cambiarEstado(adminDos.getIdUsuario(), true, actor);
        assertThat(adminDos.isActivo()).isTrue();
    }

    @Test
    void desactivarIncrementaLaVersionYReactivarNoLaToca() {
        // Dos admins para poder desactivar a uno sin romper el invariante.
        Usuario adminUno = servicio.crearUsuario("admin.uno", PASSWORD_VALIDA, Rol.ADMIN, null);
        Usuario adminDos = servicio.crearUsuario("admin.dos", PASSWORD_VALIDA, Rol.ADMIN, null);
        String actor = adminUno.getIdUsuario();
        assertThat(adminDos.getVersionToken()).isZero();

        servicio.cambiarEstado(adminDos.getIdUsuario(), false, actor);
        assertThat(adminDos.isActivo()).isFalse();
        assertThat(adminDos.getVersionToken()).isEqualTo(1);

        // Desactivar de nuevo no vuelve a versionar.
        servicio.cambiarEstado(adminDos.getIdUsuario(), false, actor);
        assertThat(adminDos.getVersionToken()).isEqualTo(1);

        // Reactivar restaura el acceso de la cuenta pero no la versión: los
        // tokens emitidos antes de la desactivación siguen revocados.
        servicio.cambiarEstado(adminDos.getIdUsuario(), true, actor);
        assertThat(adminDos.isActivo()).isTrue();
        assertThat(adminDos.getVersionToken()).isEqualTo(1);
    }

    @Test
    void unAdminNoPuedeDesactivarseASiMismo() {
        Usuario admin = servicio.crearUsuario("admin.uno", PASSWORD_VALIDA, Rol.ADMIN, null);

        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.cambiarEstado(admin.getIdUsuario(), false,
                        admin.getIdUsuario()))
                .withMessage("Un administrador no puede desactivarse a sí mismo.");
        assertThat(admin.isActivo()).isTrue();
    }

    @Test
    void noSePuedeDesactivarAlUltimoAdminActivo() {
        Usuario admin = servicio.crearUsuario("admin.uno", PASSWORD_VALIDA, Rol.ADMIN, null);

        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.cambiarEstado(admin.getIdUsuario(), false, "USR-9999"))
                .withMessage("No se puede desactivar al último administrador activo.");
        assertThat(admin.isActivo()).isTrue();
    }

    @Test
    void conDosAdminsSePuedeDesactivarAUno() {
        Usuario primero = servicio.crearUsuario("admin.uno", PASSWORD_VALIDA, Rol.ADMIN, null);
        Usuario segundo = servicio.crearUsuario("admin.dos", PASSWORD_VALIDA, Rol.ADMIN, null);

        servicio.cambiarEstado(segundo.getIdUsuario(), false, primero.getIdUsuario());

        assertThat(segundo.isActivo()).isFalse();
        assertThat(primero.isActivo()).isTrue();
    }

    @Test
    void noSePuedeDegradarAlUltimoAdminActivo() {
        Usuario admin = servicio.crearUsuario("admin.uno", PASSWORD_VALIDA, Rol.ADMIN, null);
        Doctor doctor = (Doctor) trabajadorService.registrarTrabajador(
                "Carlos Mena", "Doctor", "Cardiologia", null);

        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.cambiarRol(admin.getIdUsuario(), Rol.DOCTOR,
                        doctor.getIdTrabajador()))
                .withMessage("No se puede degradar al último administrador activo.");
        assertThat(admin.getRol()).isEqualTo(Rol.ADMIN);
    }

    @Test
    void reactivarUnUsuarioHuerfanoSeRechazaConMensajeClaro() {
        Enfermero enfermero = (Enfermero) trabajadorService.registrarTrabajador(
                "Lucia Vidal", "Enfermero", null, NivelExperiencia.NOVATO);
        Usuario usuario = servicio.crearUsuario("lucia.vidal", PASSWORD_VALIDA, Rol.ENFERMERO,
                enfermero.getIdTrabajador());
        servicio.cambiarEstado(usuario.getIdUsuario(), false, "USR-9999");

        // Se elimina el trabajador: el usuario queda huérfano.
        trabajadorService.eliminarTrabajador(enfermero.getIdTrabajador());

        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.cambiarEstado(usuario.getIdUsuario(), true, "USR-9999"))
                .withMessage("No se puede reactivar la cuenta: el trabajador "
                        + enfermero.getIdTrabajador() + " ya no existe.");
        assertThat(usuario.isActivo()).isFalse();
    }

    // ------------------------------------------------------ cambio de password

    @Test
    void cambiarPasswordPropiaExigeLaActualYDifiereDeLaNueva() {
        Usuario usuario = servicio.crearUsuario("ana.torres", PASSWORD_VALIDA, Rol.ADMIN, null, true);

        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.cambiarPasswordPropia(usuario.getIdUsuario(),
                        "equivocada", "otra-clave-larga"))
                .withMessage("La contraseña actual no es correcta.")
                .satisfies(ex -> assertThat(ex.getMessage()).doesNotContain("equivocada"));

        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.cambiarPasswordPropia(usuario.getIdUsuario(),
                        PASSWORD_VALIDA, PASSWORD_VALIDA))
                .withMessage("La nueva contraseña debe ser diferente de la actual.");
    }

    @Test
    void cambiarPasswordPropiaAplicaLaPolitica() {
        Usuario usuario = servicio.crearUsuario("ana.torres", PASSWORD_VALIDA, Rol.ADMIN, null);

        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.cambiarPasswordPropia(usuario.getIdUsuario(),
                        PASSWORD_VALIDA, "corta"))
                .withMessage("La contraseña debe tener al menos 10 caracteres.");
    }

    @Test
    void cambiarPasswordPropiaIncrementaLaVersionYLimpiaElFlag() {
        Usuario usuario = servicio.crearUsuario("ana.torres", PASSWORD_VALIDA, Rol.ADMIN, null, true);
        usuario.cambiarPassword(encoder.encode(PASSWORD_VALIDA), true); // versión 1 y flag (simula alta temporal)
        assertThat(usuario.getVersionToken()).isEqualTo(1);

        String nueva = "nueva-clave-segura-10";
        servicio.cambiarPasswordPropia(usuario.getIdUsuario(), PASSWORD_VALIDA, nueva);

        assertThat(usuario.getVersionToken()).isEqualTo(2);
        assertThat(usuario.isDebeCambiarPassword()).isFalse();
        assertThat(encoder.matches(nueva, usuario.getPasswordHash())).isTrue();
        assertThat(encoder.matches(PASSWORD_VALIDA, usuario.getPasswordHash())).isFalse();
    }

    @Test
    void resetearPasswordIncrementaLaVersionYDejaElCambioPendiente() {
        Usuario usuario = servicio.crearUsuario("ana.torres", PASSWORD_VALIDA, Rol.ADMIN, null);
        assertThat(usuario.getVersionToken()).isZero();

        String nueva = "temporal-clave-12345";
        servicio.resetearPassword(usuario.getIdUsuario(), nueva);

        assertThat(usuario.getVersionToken()).isEqualTo(1);
        assertThat(usuario.isDebeCambiarPassword()).isTrue();
        assertThat(encoder.matches(nueva, usuario.getPasswordHash())).isTrue();
    }

    // ----------------------------------------- concurrencia del último ADMIN

    @Test
    void dosAdminsQueSeDesactivanMutuamenteNuncaDejanElSistemaSinAdmin() throws Exception {
        Usuario primero = servicio.crearUsuario("admin.uno", PASSWORD_VALIDA, Rol.ADMIN, null);
        Usuario segundo = servicio.crearUsuario("admin.dos", PASSWORD_VALIDA, Rol.ADMIN, null);
        String idPrimero = primero.getIdUsuario();
        String idSegundo = segundo.getIdUsuario();

        int iteraciones = 200;
        ExecutorService pool = Executors.newFixedThreadPool(2);
        AtomicInteger fallos = new AtomicInteger();
        try {
            for (int i = 0; i < iteraciones; i++) {
                // Cada iteración parte de los dos admins activos.
                if (!primero.isActivo()) {
                    primero.reactivar();
                }
                if (!segundo.isActivo()) {
                    segundo.reactivar();
                }
                CountDownLatch puerta = new CountDownLatch(1);
                Future<?> a = pool.submit(() -> intentarDesactivar(idSegundo, idPrimero, puerta, fallos));
                Future<?> b = pool.submit(() -> intentarDesactivar(idPrimero, idSegundo, puerta, fallos));
                puerta.countDown();
                a.get(30, TimeUnit.SECONDS);
                b.get(30, TimeUnit.SECONDS);

                long adminsActivos = repositorio.listarTodos().stream()
                        .filter(u -> u.getRol() == Rol.ADMIN && u.isActivo())
                        .count();
                if (adminsActivos == 0) {
                    throw new AssertionError("Iteración " + i + ": no quedó ningún ADMIN activo");
                }
            }
        } finally {
            pool.shutdownNow();
        }
        // Ambos intentos pueden fallar por «último admin», pero nunca ambos triunfar.
        assertThat(fallos.get()).isGreaterThanOrEqualTo(iteraciones);
    }

    private void intentarDesactivar(String objetivo, String actor, CountDownLatch puerta, AtomicInteger fallos) {
        try {
            puerta.await();
            servicio.cambiarEstado(objetivo, false, actor);
        } catch (SolicitudInvalidaException esperado) {
            fallos.incrementAndGet();
        } catch (InterruptedException interrumpido) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrumpido);
        }
    }
}
