package com.pachoclosystem.pachoclosystem.security;

import com.pachoclosystem.pachoclosystem.controller.MockMvcBaseTest;
import com.pachoclosystem.pachoclosystem.model.NivelExperiencia;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static com.pachoclosystem.pachoclosystem.security.AutorizacionPorRolTest.Acceso.DENEGADO;
import static com.pachoclosystem.pachoclosystem.security.AutorizacionPorRolTest.Acceso.PERMITIDO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Tabla de permisos por rol: cada fila es una operación y lo que puede hacer
 * cada rol. Es la misma tabla que la del README.
 *
 * <p>PERMITIDO significa que la petición supera la autorización: puede acabar
 * en 2xx, 400, 404 o 503 (MedicamentosService no corre en los tests), pero
 * nunca en 401 ni 403. DENEGADO es un 403 con el cuerpo de error uniforme.
 * Las operaciones usan IDs inexistentes y cuerpos vacíos para no cambiar
 * datos.</p>
 */
class AutorizacionPorRolTest extends MockMvcBaseTest {

    enum Acceso { PERMITIDO, DENEGADO }

    private static final String EVOLUCION = """
            {"tipo":"EVOLUCION","contenido":"Paciente estable y sin cambios"}""";
    private static final String DIAGNOSTICO = """
            {"tipo":"DIAGNOSTICO","contenido":"Hipertension leve"}""";

    /** Operación, cuerpo y acceso esperado para ADMIN, DOCTOR y ENFERMERO. */
    private record Fila(HttpMethod metodo, String ruta, String cuerpo,
                        Acceso admin, Acceso doctor, Acceso enfermero) {
    }

    private static final List<Fila> TABLA = List.of(
            // Sesión
            new Fila(HttpMethod.GET, "/api/auth/me", null, PERMITIDO, PERMITIDO, PERMITIDO),
            // Pacientes
            new Fila(HttpMethod.GET, "/api/pacientes", null, PERMITIDO, PERMITIDO, PERMITIDO),
            new Fila(HttpMethod.GET, "/api/pacientes/PAC-9999", null, PERMITIDO, PERMITIDO, PERMITIDO),
            new Fila(HttpMethod.POST, "/api/pacientes", "{}", PERMITIDO, PERMITIDO, DENEGADO),
            new Fila(HttpMethod.PUT, "/api/pacientes/PAC-9999", "{}", PERMITIDO, PERMITIDO, DENEGADO),
            new Fila(HttpMethod.PATCH, "/api/pacientes/PAC-9999/habitacion", "{}", PERMITIDO, PERMITIDO, PERMITIDO),
            new Fila(HttpMethod.DELETE, "/api/pacientes/PAC-9999", null, PERMITIDO, DENEGADO, DENEGADO),
            // Historial clínico (el ADMIN no tiene trabajador para firmar; el enfermero no diagnostica)
            new Fila(HttpMethod.GET, "/api/historial", null, PERMITIDO, PERMITIDO, PERMITIDO),
            new Fila(HttpMethod.GET, "/api/pacientes/PAC-9999/historial", null, PERMITIDO, PERMITIDO, PERMITIDO),
            new Fila(HttpMethod.POST, "/api/pacientes/PAC-9999/historial", EVOLUCION, DENEGADO, PERMITIDO, PERMITIDO),
            new Fila(HttpMethod.POST, "/api/pacientes/PAC-9999/historial", DIAGNOSTICO, DENEGADO, PERMITIDO, DENEGADO),
            // Trabajadores
            new Fila(HttpMethod.GET, "/api/trabajadores", null, PERMITIDO, PERMITIDO, DENEGADO),
            new Fila(HttpMethod.GET, "/api/trabajadores/DOC-9999", null, PERMITIDO, PERMITIDO, DENEGADO),
            new Fila(HttpMethod.POST, "/api/trabajadores", "{}", PERMITIDO, DENEGADO, DENEGADO),
            new Fila(HttpMethod.PUT, "/api/trabajadores/DOC-9999", "{}", PERMITIDO, DENEGADO, DENEGADO),
            new Fila(HttpMethod.DELETE, "/api/trabajadores/DOC-9999", null, PERMITIDO, DENEGADO, DENEGADO),
            // Medicamentos
            new Fila(HttpMethod.GET, "/api/medicamentos", null, PERMITIDO, PERMITIDO, PERMITIDO),
            new Fila(HttpMethod.GET, "/api/medicamentos/MED-9999", null, PERMITIDO, PERMITIDO, PERMITIDO),
            new Fila(HttpMethod.GET, "/api/medicamentos/stock-bajo", null, PERMITIDO, PERMITIDO, PERMITIDO),
            new Fila(HttpMethod.GET, "/api/medicamentos/por-vencer", null, PERMITIDO, PERMITIDO, PERMITIDO),
            new Fila(HttpMethod.GET, "/api/medicamentos/vencidos", null, PERMITIDO, PERMITIDO, PERMITIDO),
            new Fila(HttpMethod.POST, "/api/medicamentos", "{}", PERMITIDO, DENEGADO, DENEGADO),
            new Fila(HttpMethod.PUT, "/api/medicamentos/MED-9999", "{}", PERMITIDO, DENEGADO, DENEGADO),
            new Fila(HttpMethod.DELETE, "/api/medicamentos/MED-9999", null, PERMITIDO, DENEGADO, DENEGADO),
            new Fila(HttpMethod.POST, "/api/medicamentos/MED-9999/entradas", "{}", PERMITIDO, DENEGADO, DENEGADO),
            new Fila(HttpMethod.POST, "/api/medicamentos/MED-9999/salidas", "{}", PERMITIDO, DENEGADO, PERMITIDO),
            // Usuarios
            new Fila(HttpMethod.GET, "/api/usuarios", null, PERMITIDO, DENEGADO, DENEGADO),
            new Fila(HttpMethod.POST, "/api/usuarios", "{}", PERMITIDO, DENEGADO, DENEGADO),
            new Fila(HttpMethod.PATCH, "/api/usuarios/USR-9999/desactivar", null, PERMITIDO, DENEGADO, DENEGADO),
            new Fila(HttpMethod.PATCH, "/api/usuarios/USR-9999/activar", null, PERMITIDO, DENEGADO, DENEGADO),
            new Fila(HttpMethod.PATCH, "/api/usuarios/USR-9999/password", "{}", PERMITIDO, DENEGADO, DENEGADO));

    static Stream<Arguments> casos() {
        return TABLA.stream().flatMap(f -> Stream.of(
                Arguments.of("ADMIN", f, f.admin()),
                Arguments.of("DOCTOR", f, f.doctor()),
                Arguments.of("ENFERMERO", f, f.enfermero())));
    }

    @ParameterizedTest(name = "{0} {1} -> {2}")
    @MethodSource("casos")
    void tablaDePermisos(String rol, Fila fila, Acceso esperado) throws Exception {
        MockHttpServletRequestBuilder peticion = request(fila.metodo(), fila.ruta())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDeRol(rol));
        if (fila.cuerpo() != null) {
            peticion.contentType(MediaType.APPLICATION_JSON).content(fila.cuerpo());
        }

        MvcResult resultado = mockMvc.perform(peticion).andReturn();
        int estado = resultado.getResponse().getStatus();
        String cuerpo = resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);

        if (esperado == PERMITIDO) {
            assertThat(estado).as("%s %s %s: %s", rol, fila.metodo(), fila.ruta(), cuerpo)
                    .isNotIn(401, 403);
        } else {
            assertThat(estado).as("%s %s %s", rol, fila.metodo(), fila.ruta()).isEqualTo(403);
            assertThat(cuerpo)
                    .contains("\"status\":403")
                    .contains("\"error\":\"Forbidden\"")
                    .contains("\"mensajes\":[\"")
                    .doesNotContain("Exception")
                    .doesNotContain("com.pachoclosystem");
        }
    }

    static Stream<Fila> filas() {
        return TABLA.stream();
    }

    @ParameterizedTest(name = "sin token {0} -> 401")
    @MethodSource("filas")
    void sinTokenTodasLasOperacionesResponden401Uniforme(Fila fila) throws Exception {
        MockHttpServletRequestBuilder peticion = request(fila.metodo(), fila.ruta());
        if (fila.cuerpo() != null) {
            peticion.contentType(MediaType.APPLICATION_JSON).content(fila.cuerpo());
        }

        MvcResult resultado = mockMvc.perform(peticion).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        assertThat(resultado.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
        assertThat(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("\"status\":401")
                .contains("\"error\":\"Unauthorized\"")
                .contains("Debe autenticarse para acceder a este recurso.");
    }

    /** Token del admin inicial, o de un doctor/enfermero nuevo con su usuario. */
    private String tokenDeRol(String rol) throws Exception {
        return switch (rol) {
            case "ADMIN" -> tokenAdmin();
            case "DOCTOR" -> tokenDeTrabajador(registrarDoctor("Carlos Mena", "Cardiologia"));
            default -> tokenDeTrabajador(registrarEnfermero("Lucia Vidal", NivelExperiencia.AVANZADO));
        };
    }
}
