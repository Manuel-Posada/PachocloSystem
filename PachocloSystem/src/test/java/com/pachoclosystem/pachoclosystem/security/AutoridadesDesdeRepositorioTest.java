package com.pachoclosystem.pachoclosystem.security;

import com.pachoclosystem.pachoclosystem.controller.MockMvcBaseTest;
import com.pachoclosystem.pachoclosystem.model.Rol;
import com.pachoclosystem.pachoclosystem.model.Usuario;
import com.pachoclosystem.pachoclosystem.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Decisión de diseño: las autoridades y el rol se leen del repositorio en cada
 * petición, nunca de los claims del token. El test cambia el rol del usuario en
 * el repositorio conservando el token original (cuyo claim sigue diciendo
 * DOCTOR) y comprueba que {@code GET /api/auth/me} devuelve el rol nuevo.
 */
class AutoridadesDesdeRepositorioTest extends MockMvcBaseTest {

    private static final String PASSWORD = "password-segura-12345";

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
    void meDevuelveElRolDelRepositorioAunqueElTokenDigaDoctor() throws Exception {
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        String username = "autoridades." + System.nanoTime();
        Usuario usuario = usuarioService.crearUsuario(username, PASSWORD, Rol.DOCTOR, doctor);

        String token = tokenDelLogin(username, PASSWORD);

        // El token original certifica DOCTOR.
        assertThat(jwtDecoder.decode(token).getClaimAsString("rol")).isEqualTo("DOCTOR");

        // Cambio de rol directo en el repositorio (sin pasar por el servicio ni
        // tocar la versión del token), conservando el id y el trabajador.
        assertThat(repositorioUsuarios.cambiarRol(usuario.getIdUsuario(), Rol.ENFERMERO,
                usuario.getIdTrabajador())).isTrue();

        // El token no ha cambiado, pero la petición relee el repositorio.
        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(usuario.getIdUsuario()))
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.rol").value("ENFERMERO"));

        // Y el token sigue diciendo DOCTOR: el test discrimina entre claim y repo.
        Jwt jwtReleido = jwtDecoder.decode(token);
        assertThat(jwtReclamadoRol(jwtReleido)).isEqualTo("DOCTOR");
    }

    private String jwtReclamadoRol(Jwt jwt) {
        return jwt.getClaimAsString("rol");
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