package com.pachoclosystem.pachoclosystem.controller;

import com.pachoclosystem.pachoclosystem.config.ClaveFirmaJwt;
import com.pachoclosystem.pachoclosystem.model.NivelExperiencia;
import com.pachoclosystem.pachoclosystem.model.Usuario;
import com.pachoclosystem.pachoclosystem.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autoría y permisos al crear registros del historial: el autor es siempre el
 * trabajador del usuario autenticado (releído del repositorio), el ADMIN no
 * puede firmar registros y el enfermero no crea diagnósticos.
 */
class HistorialAutoriaTest extends MockMvcBaseTest {

    @Autowired
    private JwtEncoder codificador;

    @Autowired
    private ClaveFirmaJwt firma;

    private static String registro(String tipo, String idAutor) {
        String autor = idAutor == null ? "" : ",\"idAutor\":\"" + idAutor + "\"";
        if (tipo.equals("SIGNOS_VITALES")) {
            return """
                    {"tipo":"SIGNOS_VITALES"%s,"signosVitales":{"temperatura":36.5,"frecCardiaca":80,
                    "presionSistolica":120,"presionDiastolica":80,"frecRespiratoria":16,"saturacion":98}}"""
                    .formatted(autor);
        }
        return "{\"tipo\":\"%s\"%s,\"contenido\":\"Paciente estable y sin cambios\"}".formatted(tipo, autor);
    }

    private ResultActions crearConToken(String token, String paciente, String cuerpo) throws Exception {
        return mockMvc.perform(post("/api/pacientes/{id}/historial", paciente)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    @Test
    void sinIdAutorElAutorEsElTrabajadorDelUsuarioAutenticado() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");

        postHistorialComo(doctor, paciente, registro("DIAGNOSTICO", null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.autor.idTrabajador").value(doctor))
                .andExpect(jsonPath("$.autor.nombreCompleto").value("Carlos Mena"));
    }

    @Test
    void unIdAutorDeOtroTrabajadorExistenteSeRechazaCon400() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        String otroDoctor = registrarDoctor("Eva Mora", "Pediatria");

        postHistorialComo(doctor, paciente, registro("DIAGNOSTICO", otroDoctor))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensajes[0]").value("El idAutor enviado no coincide con el trabajador "
                        + "de su usuario: el autor de un registro es siempre quien inicia sesión."));
        // No se guardó nada.
        perform(get("/api/pacientes/{id}/historial", paciente))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void elAutorSaleDelRepositorioYNoDelClaimIdTrabajadorDelToken() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");
        String otroDoctor = registrarDoctor("Eva Mora", "Pediatria");
        tokenDeTrabajador(doctor);
        Usuario usuario = repositorioUsuarios.buscarPorIdTrabajador(doctor);

        // Token válido y bien firmado del usuario de Carlos, pero con el claim
        // idTrabajador de Eva: el autor debe seguir siendo Carlos.
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(usuario.getIdUsuario())
                .claim("username", usuario.getUsername())
                .claim("rol", "DOCTOR")
                .claim("idTrabajador", otroDoctor)
                .claim(JwtTokenService.CLAIM_CREDENCIALES, usuario.getCredenciales().marca())
                .issuer(firma.emisor())
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(600))
                .build();
        String token = codificador.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        crearConToken(token, paciente, registro("EVOLUCION", null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.autor.idTrabajador").value(doctor));
    }

    @Test
    void elAdminNoPuedeCrearRegistrosPorqueNoTieneTrabajador() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);

        crearConToken(tokenAdmin(), paciente, registro("EVOLUCION", null))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.mensajes[0]")
                        .value("Solo un usuario vinculado a un trabajador puede crear registros."));
    }

    @Test
    void elEnfermeroNoPuedeCrearDiagnosticos() throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String enfermero = registrarEnfermero("Lucia Vidal", NivelExperiencia.AVANZADO);

        postHistorialComo(enfermero, paciente, registro("DIAGNOSTICO", null))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.mensajes[0]")
                        .value("Su rol no puede crear registros de tipo DIAGNOSTICO."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"EVOLUCION", "SIGNOS_VITALES", "MEDICACION"})
    void elEnfermeroCreaEvolucionSignosVitalesYMedicacion(String tipo) throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String enfermero = registrarEnfermero("Lucia Vidal", NivelExperiencia.AVANZADO);

        postHistorialComo(enfermero, paciente, registro(tipo, enfermero))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value(tipo))
                .andExpect(jsonPath("$.autor.idTrabajador").value(enfermero));
    }

    @ParameterizedTest
    @ValueSource(strings = {"DIAGNOSTICO", "EVOLUCION", "SIGNOS_VITALES", "MEDICACION"})
    void elDoctorCreaLosCuatroTipos(String tipo) throws Exception {
        String paciente = registrarPaciente("Ana Torres", 30, 101);
        String doctor = registrarDoctor("Carlos Mena", "Cardiologia");

        postHistorialComo(doctor, paciente, registro(tipo, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value(tipo));
    }
}
