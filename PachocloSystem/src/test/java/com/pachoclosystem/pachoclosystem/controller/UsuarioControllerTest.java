package com.pachoclosystem.pachoclosystem.controller;

import com.pachoclosystem.pachoclosystem.model.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code /api/usuarios}: solo ADMIN, sin hashes en las respuestas, 409 en los
 * conflictos y efecto inmediato de desactivar, activar y restablecer contraseña
 * sobre el login y los tokens.
 *
 * <p>El contexto (y su repositorio de usuarios) se comparte entre tests y no
 * se vacía: cada test usa usernames únicos y nunca desactiva al admin.</p>
 */
class UsuarioControllerTest extends MockMvcBaseTest {

    private static final String PASSWORD = "clave-de-pruebas-10";

    // ------------------------------------------------------------------ alta

    @Test
    void elAdminCreaUnDoctorVinculadoYLaRespuestaNoIncluyeElHash() throws Exception {
        String idDoctor = registrarDoctor();
        String username = unico("doc");

        MvcResult resultado = perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alta(username, PASSWORD, "DOCTOR", idDoctor)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.rol").value("DOCTOR"))
                .andExpect(jsonPath("$.idTrabajador").value(idDoctor))
                .andExpect(jsonPath("$.activo").value(true))
                .andReturn();

        String id = leer(resultado, "$.idUsuario");
        assertThat(resultado.getResponse().getHeader(HttpHeaders.LOCATION))
                .isEqualTo("/api/usuarios/" + id);
        comprobarSinSecretos(resultado);
        login(username, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void listarIncluyeLosUsuariosSinHashesYFiltraPorQ() throws Exception {
        String username = crearAdmin();

        MvcResult todos = perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username == 'admin')]").exists())
                .andExpect(jsonPath("$[?(@.username == '%s')].activo".formatted(username)).value(true))
                .andReturn();
        comprobarSinSecretos(todos);

        perform(get("/api/usuarios").param("q", username.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value(username));
    }

    @Test
    void usernameDuplicadoResponde409() throws Exception {
        String username = crearAdmin();

        perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alta(username.toUpperCase(), PASSWORD, "ADMIN", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensajes[0]")
                        .value("Ya existe un usuario con el username " + username + "."));
    }

    @Test
    void unTrabajadorConUsuarioNoPuedeTenerOtroYResponde409() throws Exception {
        String idDoctor = registrarDoctor();
        crearUsuario(unico("doc"), "DOCTOR", idDoctor);

        perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alta(unico("doc"), PASSWORD, "DOCTOR", idDoctor)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensajes[0]")
                        .value("El trabajador " + idDoctor + " ya tiene un usuario."));
    }

    @Test
    void altaInvalidaDevuelveTodosLosErroresSinIncluirLaPassword() throws Exception {
        MvcResult resultado = perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alta("x", "corta-123", "JEFE", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes.length()").value(3))
                .andReturn();

        assertThat(cuerpo(resultado))
                .contains("El username debe tener entre 3 y 30 caracteres")
                .contains("La contraseña debe tener al menos 10 caracteres.")
                .contains("Debe seleccionar un rol (ADMIN, DOCTOR o ENFERMERO).")
                .doesNotContain("corta-123");
    }

    @Test
    void passwordDeMasDe72BytesResponde400YNo500() throws Exception {
        perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alta(unico("adm"), "ñ".repeat(37), "ADMIN", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value(
                        "La contraseña no puede ocupar más de 72 bytes (las letras con tilde y la ñ ocupan 2)."));
    }

    @Test
    void elVinculoExigeUnTrabajadorExistenteYDelTipoDelRol() throws Exception {
        String idDoctor = registrarDoctor();

        perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alta(unico("enf"), PASSWORD, "ENFERMERO", idDoctor)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("El trabajador " + idDoctor + " no es un Enfermero."));

        perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alta(unico("doc"), PASSWORD, "DOCTOR", "DOC-9999")))
                .andExpect(status().isNotFound());
    }

    // --------------------------------------------------- desactivar y activar

    @Test
    void desactivarCortaElLoginYElTokenYActivarLosDevuelve() throws Exception {
        String username = crearAdmin();
        String id = idDe(username);
        String token = tokenDe(username);

        perform(patch("/api/usuarios/{id}/desactivar", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        perform(patch("/api/usuarios/{id}/desactivar", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        login(username, PASSWORD).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());

        perform(patch("/api/usuarios/{id}/activar", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
        perform(patch("/api/usuarios/{id}/activar", id))
                .andExpect(status().isOk());
        login(username, PASSWORD).andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void elAdminNoPuedeDesactivarseASiMismo() throws Exception {
        perform(patch("/api/usuarios/{id}/desactivar", idDe("admin")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensajes[0]").value("No puede desactivar su propio usuario."));

        assertThat(repositorioUsuarios.buscarPorUsername("admin").isActivo()).isTrue();
    }

    @Test
    void noSeReactivaUnUsuarioCuyoTrabajadorSeElimino() throws Exception {
        String idDoctor = registrarDoctor();
        String username = unico("doc");
        crearUsuario(username, "DOCTOR", idDoctor);
        // Eliminar el trabajador desactiva su usuario en cascada.
        perform(delete("/api/trabajadores/{id}", idDoctor))
                .andExpect(status().isNoContent());

        perform(patch("/api/usuarios/{id}/activar", idDe(username)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensajes[0]").value("No se puede activar el usuario " + username
                        + ": su trabajador " + idDoctor + " ya no existe."));
    }

    @Test
    void cambiarElEstadoDeUnUsuarioInexistenteResponde404() throws Exception {
        perform(patch("/api/usuarios/USR-9999/desactivar")).andExpect(status().isNotFound());
        perform(patch("/api/usuarios/USR-9999/activar")).andExpect(status().isNotFound());
    }

    // ------------------------------------------------- restablecer contraseña

    @Test
    void restablecerLaPasswordHaceQueSoloValgaLaNueva() throws Exception {
        String username = crearAdmin();

        perform(patch("/api/usuarios/{id}/password", idDe(username))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"otra-clave-nueva-1\"}"))
                .andExpect(status().isNoContent());

        login(username, PASSWORD).andExpect(status().isUnauthorized());
        login(username, "otra-clave-nueva-1").andExpect(status().isOk());
    }

    @Test
    void restablecerLaPasswordInvalidaLosTokensAnterioresYElLoginNuevoVale() throws Exception {
        String username = crearAdmin();
        String tokenAnterior = leer(login(username, PASSWORD).andReturn(), "$.token");

        perform(patch("/api/usuarios/{id}/password", idDe(username))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"otra-clave-nueva-1\"}"))
                .andExpect(status().isNoContent());

        comprobar401Uniforme(conToken(get("/api/auth/me"), tokenAnterior));
        String tokenNuevo = leer(login(username, "otra-clave-nueva-1").andReturn(), "$.token");
        mockMvc.perform(conToken(get("/api/auth/me"), tokenNuevo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    void unAdminQueRestableceSuPropiaPasswordDebeVolverAIniciarSesion() throws Exception {
        String username = crearAdmin();
        String suToken = leer(login(username, PASSWORD).andReturn(), "$.token");

        // Restablece su propia contraseña con su propio token: la operación se completa...
        mockMvc.perform(conToken(patch("/api/usuarios/{id}/password", idDe(username)), suToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"otra-clave-nueva-1\"}"))
                .andExpect(status().isNoContent());

        // ...pero ese token deja de valer, también para seguir administrando.
        comprobar401Uniforme(conToken(get("/api/usuarios"), suToken));
        login(username, PASSWORD).andExpect(status().isUnauthorized());
        String tokenNuevo = leer(login(username, "otra-clave-nueva-1").andReturn(), "$.token");
        mockMvc.perform(conToken(get("/api/usuarios"), tokenNuevo))
                .andExpect(status().isOk());
    }

    @Test
    void restablecerAplicaLaPoliticaSinDevolverLaPassword() throws Exception {
        String username = crearAdmin();

        MvcResult resultado = perform(patch("/api/usuarios/{id}/password", idDe(username))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"corta-123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("La contraseña debe tener al menos 10 caracteres."))
                .andReturn();

        assertThat(cuerpo(resultado)).doesNotContain("corta-123");
        login(username, PASSWORD).andExpect(status().isOk());
        perform(patch("/api/usuarios/USR-9999/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"otra-clave-nueva-1\"}"))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------- acceso

    static Stream<MockHttpServletRequestBuilder> todasLasOperaciones() {
        return Stream.of(
                get("/api/usuarios"),
                post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(alta("intruso.x", PASSWORD, "ADMIN", null)),
                patch("/api/usuarios/USR-0001/desactivar"),
                patch("/api/usuarios/USR-0001/activar"),
                patch("/api/usuarios/USR-0001/password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"otra-clave-nueva-1\"}"));
    }

    @ParameterizedTest
    @MethodSource("todasLasOperaciones")
    void unDoctorRecibe403UniformeEnTodasLasOperaciones(MockHttpServletRequestBuilder peticion)
            throws Exception {
        String username = unico("doc");
        crearUsuario(username, "DOCTOR", registrarDoctor());

        MvcResult resultado = mockMvc.perform(peticion
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(username)))
                .andExpect(status().isForbidden())
                .andReturn();

        assertThat(cuerpo(resultado))
                .contains("\"status\":403")
                .contains("\"error\":\"Forbidden\"")
                .contains("No tiene permisos para realizar esta operación.")
                .doesNotContain("Exception");
        assertThat(repositorioUsuarios.buscarPorUsername("intruso.x")).isNull();
    }

    @ParameterizedTest
    @MethodSource("todasLasOperaciones")
    void sinTokenResponde401Uniforme(MockHttpServletRequestBuilder peticion) throws Exception {
        MvcResult resultado = sinToken(peticion)
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andReturn();

        assertThat(cuerpo(resultado)).contains("\"status\":401");
    }

    // ------------------------------------------------------------ utilidades

    /** Username único por test: el repositorio de usuarios no se vacía entre tests. */
    private static String unico(String prefijo) {
        return prefijo + "." + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private static String alta(String username, String password, String rol, String idTrabajador) {
        return """
                {"username":"%s","password":"%s","rol":"%s","idTrabajador":%s}"""
                .formatted(username, password, rol,
                        idTrabajador == null ? "null" : "\"" + idTrabajador + "\"");
    }

    private String crearAdmin() throws Exception {
        String username = unico("adm");
        crearUsuario(username, "ADMIN", null);
        return username;
    }

    private void crearUsuario(String username, String rol, String idTrabajador) throws Exception {
        perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alta(username, PASSWORD, rol, idTrabajador)))
                .andExpect(status().isCreated());
    }

    private String registrarDoctor() throws Exception {
        MvcResult resultado = perform(post("/api/trabajadores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Eva Mora","rol":"Doctor","especialidad":"Cardiologia"}"""))
                .andExpect(status().isCreated())
                .andReturn();
        return leer(resultado, "$.idTrabajador");
    }

    private String idDe(String username) {
        return repositorioUsuarios.buscarPorUsername(username).getIdUsuario();
    }

    private String tokenDe(String username) {
        Usuario usuario = repositorioUsuarios.buscarPorUsername(username);
        return tokenService.generarToken(usuario);
    }

    private ResultActions login(String username, String password)
            throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)));
    }

    private static MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder peticion,
                                                          String token) {
        return peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private void comprobar401Uniforme(MockHttpServletRequestBuilder peticion) throws Exception {
        MvcResult resultado = mockMvc.perform(peticion)
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.mensajes[0]").value("Debe autenticarse para acceder a este recurso."))
                .andReturn();
        assertThat(cuerpo(resultado)).doesNotContain("Exception").doesNotContain("credenciales");
    }

    private static String cuerpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static void comprobarSinSecretos(MvcResult resultado) throws Exception {
        assertThat(cuerpo(resultado))
                .doesNotContain("password")
                .doesNotContain("passwordHash")
                .doesNotContain("$2a$")
                .doesNotContain(PASSWORD);
    }
}
