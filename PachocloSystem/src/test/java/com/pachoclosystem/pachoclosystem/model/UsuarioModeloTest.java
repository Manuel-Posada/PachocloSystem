package com.pachoclosystem.pachoclosystem.model;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Propiedades del modelo Usuario que no deben filtrarse jamás. */
class UsuarioModeloTest {

    private static final String HASH = "$2a$10$abcdefghijklmnopqrstuv";

    @Test
    void toStringNoIncluyeElPasswordHash() {
        Usuario usuario = new Usuario("USR-0001", "ana.torres", HASH, Rol.ADMIN, null);

        String texto = usuario.toString();

        assertThat(texto)
                .contains("USR-0001")
                .contains("ana.torres")
                .contains("ADMIN")
                .doesNotContain(HASH)
                .doesNotContain("passwordHash")
                .doesNotContain("$2a$");
    }

    @Test
    void laSerializacionJsonNoIncluyeElPasswordHash() throws Exception {
        Usuario usuario = new Usuario("USR-0001", "ana.torres", HASH, Rol.DOCTOR, "DOC-0001");

        String json = new ObjectMapper().writeValueAsString(usuario);

        assertThat(json)
                .contains("ana.torres")
                .doesNotContain(HASH)
                .doesNotContain("passwordHash")
                .doesNotContain("$2a$");
    }

    @Test
    void elUsernameSeNormalizaAMinusculasYSoloSeQuitaEspacios() {
        assertThat(Usuario.normalizarUsername("  Ana.Torres  ")).isEqualTo("ana.torres");
        assertThat(Usuario.normalizarUsername(null)).isNull();
        assertThat(new Usuario("USR-0001", " ANA.TORRES ", HASH, Rol.ADMIN, null).getUsername())
                .isEqualTo("ana.torres");
    }

    @Test
    void patronUsernameAdmiteLoEsperado() {
        assertThat(Usuario.PATRON_USERNAME.matcher("ana.torres").matches()).isTrue();
        assertThat(Usuario.PATRON_USERNAME.matcher("a_b-c.99").matches()).isTrue();
        assertThat(Usuario.PATRON_USERNAME.matcher("abc").matches()).isTrue();
        assertThat(Usuario.PATRON_USERNAME.matcher("ab").matches()).isFalse();
        assertThat(Usuario.PATRON_USERNAME.matcher("con espacios").matches()).isFalse();
        assertThat(Usuario.PATRON_USERNAME.matcher("ñola").matches()).isFalse();
        assertThat(Usuario.PATRON_USERNAME.matcher("a".repeat(31)).matches()).isFalse();
        assertThat(Usuario.PATRON_USERNAME.matcher("a".repeat(30)).matches()).isTrue();
    }

    @Test
    void soloSePuedeDesactivar() {
        Usuario usuario = new Usuario("USR-0001", "ana.torres", HASH, Rol.ADMIN, null);
        assertThat(usuario.isActivo()).isTrue();

        usuario.desactivar();

        assertThat(usuario.isActivo()).isFalse();
    }

    @Test
    void sinGetterDePasswordClaro() {
        Usuario usuario = new Usuario("USR-0001", "ana.torres", HASH, Rol.ADMIN, null);

        // El modelo no expone ningún método con la contraseña en claro.
        assertThat(Usuario.class.getDeclaredMethods())
                .extracting(java.lang.reflect.Method::getName)
                .noneMatch(nombre -> nombre.toLowerCase().contains("passwordclara")
                        || nombre.equals("getPassword"));
        assertThat(usuario.getPasswordHash()).isEqualTo(HASH);
    }
}
