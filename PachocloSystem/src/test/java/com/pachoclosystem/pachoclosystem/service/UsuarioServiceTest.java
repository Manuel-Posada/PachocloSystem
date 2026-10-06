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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/** Reglas de negocio de los usuarios, sin contexto Spring. */
class UsuarioServiceTest {

    private static final String PASSWORD_VALIDA = "clave-de-pruebas-10";

    private UsuarioRepositoryImpl repositorio;
    private TrabajadorService trabajadorService;
    private PasswordEncoder encoder;
    private UsuarioService servicio;

    @BeforeEach
    void preparar() {
        repositorio = new UsuarioRepositoryImpl();
        trabajadorService = new TrabajadorService(new TrabajadorRepositoryImpl());
        encoder = new BCryptPasswordEncoder();
        servicio = new UsuarioService(repositorio, trabajadorService, encoder);
    }

    // ---------------------------------------------------------------- camino feliz

    @Test
    void crearAdminGeneraIdUsrYGuardaSoloElHash() {
        Usuario admin = servicio.crearUsuario("Ana.Torres", PASSWORD_VALIDA, Rol.ADMIN, null);

        assertThat(admin.getIdUsuario()).isEqualTo("USR-0001");
        assertThat(admin.getUsername()).isEqualTo("ana.torres");
        assertThat(admin.getRol()).isEqualTo(Rol.ADMIN);
        assertThat(admin.getIdTrabajador()).isNull();
        assertThat(admin.isActivo()).isTrue();

        // La contraseña en claro nunca se guarda: solo el hash BCrypt.
        assertThat(admin.getPasswordHash()).startsWith("$2").isNotEqualTo(PASSWORD_VALIDA);
        assertThat(encoder.matches(PASSWORD_VALIDA, admin.getPasswordHash())).isTrue();
        assertThat(encoder.matches("otra-clave-distinta", admin.getPasswordHash())).isFalse();
    }

    @Test
    void crearAdminNormalizaElUsernameAMinusculas() {
        Usuario admin = servicio.crearUsuario("  ADMIN.Local  ", PASSWORD_VALIDA, Rol.ADMIN, null);

        assertThat(admin.getUsername()).isEqualTo("admin.local");
        assertThat(servicio.buscarPorUsername("ADMIN.LOCAL").getIdUsuario()).isEqualTo("USR-0001");
        assertThat(repositorio.existePorUsername("Admin.Local")).isTrue();
    }

    @Test
    void crearDoctorVinculadoAlTrabajadorCorrespondiente() {
        Doctor doctor = (Doctor) trabajadorService.registrarTrabajador(
                "Carlos Mena", "Doctor", "Cardiologia", null);

        Usuario usuario = servicio.crearUsuario("carlos.mena", PASSWORD_VALIDA, Rol.DOCTOR,
                doctor.getIdTrabajador());

        assertThat(usuario.getIdTrabajador()).isEqualTo("DOC-0001");
        assertThat(repositorio.buscarPorIdTrabajador("DOC-0001")).isSameAs(usuario);
        assertThat(repositorio.listarTodos()).containsExactly(usuario);
    }

    @Test
    void crearEnfermeroVinculadoAlTrabajadorCorrespondiente() {
        Enfermero enfermero = (Enfermero) trabajadorService.registrarTrabajador(
                "Lucia Vidal", "Enfermero", null, NivelExperiencia.NOVATO);

        Usuario usuario = servicio.crearUsuario("lucia.vidal", PASSWORD_VALIDA, Rol.ENFERMERO,
                enfermero.getIdTrabajador());

        assertThat(usuario.getRol()).isEqualTo(Rol.ENFERMERO);
        assertThat(usuario.getIdTrabajador()).isEqualTo("ENF-0001");
        assertThat(repositorio.buscarPorIdTrabajador("ENF-0001")).isSameAs(usuario);
    }

    @Test
    void sePermitenVariosAdministradoresSinVincular() {
        servicio.crearUsuario("admin.primero", PASSWORD_VALIDA, Rol.ADMIN, null);
        servicio.crearUsuario("admin.segundo", PASSWORD_VALIDA, Rol.ADMIN, null);

        assertThat(repositorio.listarTodos()).hasSize(2);
    }

    // ------------------------------------------------------------------ regla a)

    @Test
    void usernameInvalidoRechazaFormatoNoPermitido() {
        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.crearUsuario("ab", PASSWORD_VALIDA, Rol.ADMIN, null))
                .withMessageContaining("El username debe tener entre 3 y 30 caracteres");

        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.crearUsuario("con espacios", PASSWORD_VALIDA, Rol.ADMIN, null))
                .withMessageContaining("El username debe tener entre 3 y 30 caracteres");

        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.crearUsuario("username.demasiado.largo.para.el.formato",
                        PASSWORD_VALIDA, Rol.ADMIN, null))
                .withMessageContaining("El username debe tener entre 3 y 30 caracteres");
    }

    @Test
    void usernameRepetidoRechazaConMayusculasDistintas() {
        servicio.crearUsuario("ana.torres", PASSWORD_VALIDA, Rol.ADMIN, null);

        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.crearUsuario("ANA.TORRES", PASSWORD_VALIDA, Rol.ADMIN, null))
                .withMessage("Ya existe un usuario con el username ana.torres.");

        assertThat(repositorio.listarTodos()).hasSize(1);
    }

    @Test
    void usernameRepetidoNoConsumeIdDeUsuario() {
        servicio.crearUsuario("ana.torres", PASSWORD_VALIDA, Rol.ADMIN, null);

        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.crearUsuario("ana.torres", PASSWORD_VALIDA, Rol.ADMIN, null));

        assertThat(servicio.crearUsuario("otro.usuario", PASSWORD_VALIDA, Rol.ADMIN, null).getIdUsuario())
                .isEqualTo("USR-0002");
    }

    // ------------------------------------------------------------------ regla b)

    @Test
    void passwordCortaRechazaSinRevelarLaPassword() {
        String passwordCorta = "secreto1";

        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.crearUsuario("ana.torres", passwordCorta, Rol.ADMIN, null))
                .withMessage("La contraseña debe tener al menos 10 caracteres.")
                // El mensaje nunca contiene la contraseña intentada.
                .satisfies(excepcion -> assertThat(excepcion.getMessage())
                        .doesNotContain(passwordCorta)
                        .doesNotContain("secreto"));
    }

    @Test
    void passwordNulaSeTrataComoInvalida() {
        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.crearUsuario("ana.torres", null, Rol.ADMIN, null))
                .withMessage("La contraseña debe tener al menos 10 caracteres.");
    }

    @Test
    void passwordExactamenteDiezCaracteresEsValida() {
        Usuario usuario = servicio.crearUsuario("ana.torres", "1234567890", Rol.ADMIN, null);

        assertThat(encoder.matches("1234567890", usuario.getPasswordHash())).isTrue();
    }

    // ------------------------------------------------------------------ regla c)

    @Test
    void adminNoPuedeEstarVinculadoATrabajador() {
        Doctor doctor = (Doctor) trabajadorService.registrarTrabajador(
                "Carlos Mena", "Doctor", "Cardiologia", null);

        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.crearUsuario("ana.torres", PASSWORD_VALIDA, Rol.ADMIN,
                        doctor.getIdTrabajador()))
                .withMessage("El usuario administrador no puede estar vinculado a un trabajador.");
    }

    // ------------------------------------------------------------------ regla d)

    @Test
    void doctorRequiereTrabajadorVinculado() {
        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.crearUsuario("carlos.mena", PASSWORD_VALIDA, Rol.DOCTOR, null))
                .withMessage("El usuario con rol DOCTOR debe estar vinculado a un trabajador.");
    }

    @Test
    void doctorConTrabajadorInexistenteLanzaNotFound() {
        assertThatExceptionOfType(NotFoundException.class)
                .isThrownBy(() -> servicio.crearUsuario("carlos.mena", PASSWORD_VALIDA, Rol.DOCTOR,
                        "DOC-9999"))
                .withMessage("No se encontró el trabajador DOC-9999.");
    }

    @Test
    void doctorConTrabajadorQueNoEsDoctorRechaza() {
        Enfermero enfermero = (Enfermero) trabajadorService.registrarTrabajador(
                "Lucia Vidal", "Enfermero", null, NivelExperiencia.NOVATO);

        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.crearUsuario("carlos.mena", PASSWORD_VALIDA, Rol.DOCTOR,
                        enfermero.getIdTrabajador()))
                .withMessage("El trabajador ENF-0001 no es un Doctor.");
    }

    // ------------------------------------------------------------------ regla e)

    @Test
    void enfermeroRequiereTrabajadorVinculado() {
        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.crearUsuario("lucia.vidal", PASSWORD_VALIDA, Rol.ENFERMERO, null))
                .withMessage("El usuario con rol ENFERMERO debe estar vinculado a un trabajador.");
    }

    @Test
    void enfermeroConTrabajadorQueNoEsEnfermeroRechaza() {
        Doctor doctor = (Doctor) trabajadorService.registrarTrabajador(
                "Carlos Mena", "Doctor", "Cardiologia", null);

        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.crearUsuario("lucia.vidal", PASSWORD_VALIDA, Rol.ENFERMERO,
                        doctor.getIdTrabajador()))
                .withMessage("El trabajador DOC-0001 no es un Enfermero.");
    }

    // ------------------------------------------------------------------ regla f)

    @Test
    void unTrabajadorNoPuedeTenerDosUsuarios() {
        Doctor doctor = (Doctor) trabajadorService.registrarTrabajador(
                "Carlos Mena", "Doctor", "Cardiologia", null);
        servicio.crearUsuario("carlos.mena", PASSWORD_VALIDA, Rol.DOCTOR, doctor.getIdTrabajador());

        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.crearUsuario("otro.nombre", PASSWORD_VALIDA, Rol.DOCTOR,
                        doctor.getIdTrabajador()))
                .withMessage("El trabajador DOC-0001 ya tiene un usuario.");

        assertThat(repositorio.listarTodos()).hasSize(1);
    }

    // ------------------------------------------------------------------ rol nulo)

    @Test
    void rolNuloRechaza() {
        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.crearUsuario("ana.torres", PASSWORD_VALIDA, null, null))
                .withMessage("Debe seleccionar un rol (ADMIN, DOCTOR o ENFERMERO).");
    }

    // ------------------------------------------------------------- buscarPorUsername

    @Test
    void buscarPorUsernameDevuelveElUsuarioSinImportarMayusculas() {
        servicio.crearUsuario("ana.torres", PASSWORD_VALIDA, Rol.ADMIN, null);

        assertThat(servicio.buscarPorUsername("Ana.Torres").getIdUsuario()).isEqualTo("USR-0001");
    }

    @Test
    void buscarPorUsernameInexistenteLanzaNotFound() {
        assertThatExceptionOfType(NotFoundException.class)
                .isThrownBy(() -> servicio.buscarPorUsername("Nadie"))
                .withMessage("No se encontró el usuario nadie.");
    }
}
