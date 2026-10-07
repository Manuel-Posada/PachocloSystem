package com.pachoclosystem.pachoclosystem.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Modelo de usuario ampliado: versión de token, cambio de rol con coherencia de
 * vínculo, reactivación idempotente y flag de cambio obligatorio de contraseña.
 * Sin contexto Spring.
 */
class UsuarioVersionTokenModeloTest {

    private static final String HASH = "$2a$10$abcdefghijklmnopqrstuv";

    private static Usuario admin(String id) {
        return new Usuario(id, "admin." + id, HASH, Rol.ADMIN, null);
    }

    @Test
    void porDefectoNoDebeCambiarPasswordYSinVersionDeToken() {
        Usuario usuario = admin("USR-0001");

        assertThat(usuario.isActivo()).isTrue();
        assertThat(usuario.isDebeCambiarPassword()).isFalse();
        assertThat(usuario.getVersionToken()).isZero();
    }

    @Test
    void desactivarYReactivarSonIdempotentes() {
        Usuario usuario = admin("USR-0001");

        usuario.desactivar();
        usuario.desactivar();
        assertThat(usuario.isActivo()).isFalse();

        usuario.reactivar();
        usuario.reactivar();
        assertThat(usuario.isActivo()).isTrue();
    }

    @Test
    void desactivarIncrementaLaVersionSoloLaPrimeraVez() {
        Usuario usuario = admin("USR-0001");
        assertThat(usuario.getVersionToken()).isZero();

        // Desactivar un usuario activo revoca sus tokens (versión +1).
        usuario.desactivar();
        assertThat(usuario.getVersionToken()).isEqualTo(1);

        // Desactivar a un usuario ya inactivo es idempotente: no cambia nada.
        usuario.desactivar();
        usuario.desactivar();
        assertThat(usuario.getVersionToken()).isEqualTo(1);
    }

    @Test
    void reactivarNoTocaLaVersionDelToken() {
        Usuario usuario = admin("USR-0001");
        usuario.desactivar();
        assertThat(usuario.getVersionToken()).isEqualTo(1);

        // Reactivar NO restaura la versión: el token anterior sigue revocado.
        usuario.reactivar();
        assertThat(usuario.isActivo()).isTrue();
        assertThat(usuario.getVersionToken()).isEqualTo(1);

        usuario.reactivar();
        assertThat(usuario.getVersionToken()).isEqualTo(1);
    }

    @Test
    void cambiarRolAdminExigeNoTenerTrabajador() {
        Usuario usuario = new Usuario("USR-0001", "carlos.mena", HASH, Rol.DOCTOR, "DOC-0001");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> usuario.cambiarRol(Rol.ADMIN, "DOC-0001"))
                .withMessageContaining("no puede estar vinculado a un trabajador");
        assertThat(usuario.getRol()).isEqualTo(Rol.DOCTOR);
        assertThat(usuario.getIdTrabajador()).isEqualTo("DOC-0001");
    }

    @Test
    void cambiarRolDoctorOEnfermeroExigeTrabajador() {
        Usuario usuario = admin("USR-0001");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> usuario.cambiarRol(Rol.DOCTOR, null))
                .withMessageContaining("debe estar vinculado a un trabajador");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> usuario.cambiarRol(Rol.ENFERMERO, "  "))
                .withMessageContaining("debe estar vinculado a un trabajador");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> usuario.cambiarRol(null, null))
                .withMessageContaining("El rol es obligatorio");
    }

    @Test
    void cambiarRolAplicaLaCoherenciaYNormalizaElTrabajador() {
        Usuario usuario = new Usuario("USR-0001", "carlos.mena", HASH, Rol.DOCTOR, "DOC-0001");

        usuario.cambiarRol(Rol.ENFERMERO, "  ENF-0002  ");

        assertThat(usuario.getRol()).isEqualTo(Rol.ENFERMERO);
        assertThat(usuario.getIdTrabajador()).isEqualTo("ENF-0002");

        usuario.cambiarRol(Rol.ADMIN, null);
        assertThat(usuario.getRol()).isEqualTo(Rol.ADMIN);
        assertThat(usuario.getIdTrabajador()).isNull();
    }

    @Test
    void cambiarPasswordIncrementaLaVersionYFijaElFlag() {
        Usuario usuario = admin("USR-0001");
        assertThat(usuario.getVersionToken()).isZero();

        usuario.cambiarPassword("$2a$10$nuevo.hash.de.prueba.abcdefghij", true);

        assertThat(usuario.getPasswordHash()).isEqualTo("$2a$10$nuevo.hash.de.prueba.abcdefghij");
        assertThat(usuario.getVersionToken()).isEqualTo(1);
        assertThat(usuario.isDebeCambiarPassword()).isTrue();

        usuario.cambiarPassword("$2a$10$otro.hash.de.prueba.abcdefghijkl", false);

        assertThat(usuario.getVersionToken()).isEqualTo(2);
        assertThat(usuario.isDebeCambiarPassword()).isFalse();
    }

    @Test
    void cambiarPasswordRechazaHashVacio() {
        Usuario usuario = admin("USR-0001");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> usuario.cambiarPassword("  ", true));
        assertThat(usuario.getVersionToken()).isZero();
    }

    @Test
    void toStringSigueSinIncluirElHash() {
        Usuario usuario = new Usuario("USR-0001", "carlos.mena", HASH, Rol.DOCTOR, "DOC-0001");
        usuario.cambiarPassword("$2a$10$nuevo.hash.de.prueba.abcdefghij", true);

        assertThat(usuario.toString())
                .contains("USR-0001")
                .contains("carlos.mena")
                .contains("DOCTOR")
                .contains("versionToken=1")
                .doesNotContain("$2a$")
                .doesNotContain("passwordHash")
                .doesNotContain("$2a$10$nuevo");
    }
}
