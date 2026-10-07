package com.pachoclosystem.pachoclosystem.security;

import com.pachoclosystem.pachoclosystem.config.ClaveFirmaJwt;
import com.pachoclosystem.pachoclosystem.controller.MockMvcBaseTest;
import com.pachoclosystem.pachoclosystem.model.NivelExperiencia;
import com.pachoclosystem.pachoclosystem.model.Rol;
import com.pachoclosystem.pachoclosystem.model.Usuario;
import com.pachoclosystem.pachoclosystem.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tokens de acceso: 401 uniforme (con WWW-Authenticate: Bearer) para peticiones
 * sin token o con token malformado, expirado, firmado con otra clave, con
 * algoritmo none o con sub inexistente; revalidación por petición (usuario
 * desactivado) y cascada de desactivación end-to-end.
 */
class AccesoTokenTest extends MockMvcBaseTest {

    private static final String PASSWORD_ADMIN = "pwd-de-prueba-solo-tests-12345";
    private static final String PASSWORD_USUARIO = "password-segura-12345";
    private static final String MENSAJE_401 = "Debe autenticarse para acceder a este recurso.";

    @Autowired
    private ClaveFirmaJwt firma;

    @Autowired
    private UsuarioService usuarioService;

    @Test
    void accesoSinTokenDevuelve401ConCuerpoUniformeYCabeceraBearer() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/pacientes"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.mensajes[0]").value(MENSAJE_401))
                .andReturn();
        comprobarCuerpoUniforme(resultado);
    }

    @Test
    void tokenMalformadoDevuelve401Uniforme() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/pacientes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer no.es.un.jwt.valido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensajes[0]").value(MENSAJE_401))
                .andReturn();
        comprobarCuerpoUniforme(resultado);
    }

    @Test
    void tokenExpiradoDevuelve401Uniforme() throws Exception {
        Instant ahora = Instant.now();
        String token = firmarConLaClaveApp(JwtClaimsSet.builder()
                .subject(idAdmin())
                .issuer("pachoclosystem")
                .issuedAt(ahora.minusSeconds(7200))
                .expiresAt(ahora.minusSeconds(3600))
                .build());

        MvcResult resultado = mockMvc.perform(get("/api/pacientes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensajes[0]").value(MENSAJE_401))
                .andReturn();
        comprobarCuerpoUniforme(resultado);
    }

    @Test
    void tokenFirmadoConOtraClaveDevuelve401Uniforme() throws Exception {
        String token = firmar(claimsValidas(), claveAleatoria());

        MvcResult resultado = mockMvc.perform(get("/api/pacientes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensajes[0]").value(MENSAJE_401))
                .andReturn();
        comprobarCuerpoUniforme(resultado);
    }

    @Test
    void tokenConAlgoritmoNoneDevuelve401Uniforme() throws Exception {
        String cabecera = base64Url("{\"alg\":\"none\",\"typ\":\"JWT\"}");
        String cuerpo = base64Url("""
                {"sub":"%s","iss":"pachoclosystem","exp":%d}"""
                .formatted(idAdmin(), Instant.now().plusSeconds(3600).getEpochSecond()));
        String token = cabecera + "." + cuerpo + ".";

        MvcResult resultado = mockMvc.perform(get("/api/pacientes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensajes[0]").value(MENSAJE_401))
                .andReturn();
        comprobarCuerpoUniforme(resultado);
    }

    @Test
    void tokenConSubInexistenteDevuelve401Uniforme() throws Exception {
        Instant ahora = Instant.now();
        String token = firmarConLaClaveApp(JwtClaimsSet.builder()
                .subject("USR-9999")
                .issuer("pachoclosystem")
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(3600))
                .build());

        MvcResult resultado = mockMvc.perform(get("/api/pacientes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensajes[0]").value(MENSAJE_401))
                .andReturn();
        comprobarCuerpoUniforme(resultado);
    }

    @Test
    void desactivarAlUsuarioInvalidaSuTokenExistente() throws Exception {
        String enfermera = registrarEnfermero("Maria Lopez", NivelExperiencia.AVANZADO);
        String username = "revocada." + System.nanoTime();
        Usuario usuario = usuarioService.crearUsuario(username, PASSWORD_USUARIO, Rol.ENFERMERO,
                enfermera);
        String token = tokenService.generarToken(usuario);

        mockMvc.perform(get("/api/pacientes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        repositorioUsuarios.desactivar(usuario.getIdUsuario());

        mockMvc.perform(get("/api/pacientes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensajes[0]").value(MENSAJE_401));
    }

    @Test
    void eliminarElTrabajadorDesactivaElUsuarioYSuTokenYSuLoginDejanDeValer() throws Exception {
        String enfermera = registrarEnfermero("Maria Lopez", NivelExperiencia.AVANZADO);
        String username = "enfermera." + System.nanoTime();
        usuarioService.crearUsuario(username, PASSWORD_USUARIO, Rol.ENFERMERO, enfermera);

        // El usuario puede entrar y usar su token.
        String token = tokenDelLogin(username, PASSWORD_USUARIO);
        mockMvc.perform(get("/api/pacientes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        // Se elimina al trabajador: la cascada desactiva al usuario vinculado.
        perform(delete("/api/trabajadores/{id}", enfermera))
                .andExpect(status().isNoContent());

        // El token ya emitido deja de valer (revalidación por petición).
        mockMvc.perform(get("/api/pacientes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensajes[0]").value(MENSAJE_401));

        // Y ya no puede volver a iniciar sesión.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}""".formatted(username, PASSWORD_USUARIO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensajes[0]").value("Credenciales inválidas."));
    }

    private void comprobarCuerpoUniforme(MvcResult resultado) throws Exception {
        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(cuerpo)
                .contains("\"status\":401")
                .contains("\"error\":\"Unauthorized\"")
                .contains(MENSAJE_401)
                .doesNotContain("Exception")
                .doesNotContain("trace")
                .doesNotContain("org.springframework")
                .doesNotContain("com.pachoclosystem");
    }

    private String idAdmin() {
        return repositorioUsuarios.buscarPorUsername("admin").getIdUsuario();
    }

    private JwtClaimsSet claimsValidas() {
        Instant ahora = Instant.now();
        return JwtClaimsSet.builder()
                .subject(idAdmin())
                .issuer("pachoclosystem")
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(3600))
                .build();
    }

    private String firmarConLaClaveApp(JwtClaimsSet claims) {
        return firmar(claims, firma.clave());
    }

    private String firmar(JwtClaimsSet claims, SecretKey clave) {
        JwtEncoder codificador = NimbusJwtEncoder.withSecretKey(clave)
                .algorithm(MacAlgorithm.HS256)
                .build();
        return codificador.encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private SecretKey claveAleatoria() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    private String base64Url(String texto) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }

    private String tokenDelLogin(String username, String password) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}""".formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return leer(resultado, "$.token");
    }
}