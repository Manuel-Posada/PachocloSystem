package com.pachoclosystem.pachoclosystem.controller;

import com.pachoclosystem.pachoclosystem.model.Rol;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Comprueba que la cadena de seguridad real se comporta exactamente como la
 * matriz ({@link MatrizAutorizacion}), cruzando <strong>cada</strong> endpoint
 * de la matriz con cada estado de sesión (sin token, ADMIN, DOCTOR y
 * ENFERMERO):
 *
 * <ul>
 *   <li>Sin token, cualquier celda no pública responde <strong>401 exacto</strong>
 *       (el bloqueo antecede a la validación del cuerpo y a la búsqueda del
 *       recurso: no hay 400/404 que se cuelen antes del permiso).</li>
 *   <li>Sesión autenticada sin permiso responde <strong>403 exacto</strong>.</li>
 *   <li>Celda permitida responde un estado de negocio (200, 201, 204, 400 o
 *       404), nunca 401/403.</li>
 *   <li>Todos los fallos se acumulan y se reportan de una vez, identificando
 *       cada uno con «sesión + clave del endpoint».</li>
 * </ul>
 */
class MatrizVsCadenaTest extends MockMvcBaseTest {

    private static final String PASSWORD_ADMIN = "pwd-de-prueba-solo-tests-12345";
    private static final String PACIENTE_VALIDO =
            "{\"nombre\":\"Ana Torres\",\"edad\":30,\"habitacion\":101}";
    private static final String TRABAJADOR_VALIDO =
            "{\"nombre\":\"Carlos Mena\",\"rol\":\"Doctor\",\"especialidad\":\"Cardiologia\"}";
    private static final String SIGNOS_VITALES = """
            {"tipo":"SIGNOS_VITALES","signosVitales":{
            "temperatura":36.5,"frecCardiaca":80,"presionSistolica":120,
            "presionDiastolica":80,"frecRespiratoria":16,"saturacion":98}}""";

    /** Estados de sesión que se cruzan con la matriz. */
    enum Sesion {
        SIN_TOKEN,
        ADMIN,
        DOCTOR,
        ENFERMERO
    }

    @Test
    void cadaEndpointDeLaMatrizSeComportaConLaCadenaRealSegunSuPolitica() throws Exception {
        SoftAssertions suaves = new SoftAssertions();
        for (MatrizAutorizacion.Endpoint endpoint : MatrizAutorizacion.ENDPOINTS) {
            for (Sesion sesion : Sesion.values()) {
                int estado = estadoDe(endpoint, sesion);
                String contexto = String.format("%s %s", sesion, endpoint.clave());

                if (permiteElAcceso(endpoint.acceso(), sesion)) {
                    // Celda permitida: la petición cruza la cadena y llega al
                    // controlador, que responde con su estado de negocio
                    // (200/201/204/400/404), nunca 401/403.
                    suaves.assertThat(estado)
                            .as("permitido %s: la petición debería cruzar la cadena", contexto)
                            .isNotIn(401, 403);
                } else if (sesion == Sesion.SIN_TOKEN) {
                    // Sin token, toda celda no pública bloquea con 401 exacto.
                    suaves.assertThat(estado)
                            .as("sin token %s: el endpoint debería dar 401 exacto", contexto)
                            .isEqualTo(401);
                } else {
                    // Sesión autenticada denegada: 403 exacto, nunca 400/404.
                    suaves.assertThat(estado)
                            .as("sin permiso %s: el endpoint debería dar 403 exacto", contexto)
                            .isEqualTo(403);
                }
            }
        }
        // Acumula todos los desajustes matriz vs cadena antes de fallar.
        suaves.assertAll();
    }

    /** ¿La política del endpoint deja operar a esta sesión (sin 401/403)? */
    private static boolean permiteElAcceso(MatrizAutorizacion.Acceso acceso, Sesion sesion) {
        return switch (acceso) {
            case PUBLICO -> true;
            case AUTENTICADO -> sesion != Sesion.SIN_TOKEN;
            case ADMIN -> sesion == Sesion.ADMIN;
            case DOCTOR -> sesion == Sesion.DOCTOR;
            case DOCTOR_O_ENFERMERO -> sesion == Sesion.DOCTOR || sesion == Sesion.ENFERMERO;
        };
    }

    /** Ejecuta la petición del endpoint (reconstruida por sesión) y devuelve el estado. */
    private int estadoDe(MatrizAutorizacion.Endpoint endpoint, Sesion sesion) throws Exception {
        MockHttpServletRequestBuilder peticion = peticionDe(endpoint);
        return switch (sesion) {
            case SIN_TOKEN -> mockMvc.perform(peticion).andReturn().getResponse().getStatus();
            case ADMIN -> mockMvc.perform(autenticadoComo(Rol.ADMIN, peticion)).andReturn().getResponse().getStatus();
            case DOCTOR -> mockMvc.perform(autenticadoComo(Rol.DOCTOR, peticion)).andReturn().getResponse().getStatus();
            case ENFERMERO -> mockMvc.perform(autenticadoComo(Rol.ENFERMERO, peticion)).andReturn().getResponse().getStatus();
        };
    }

    /**
     * Petición con la que se sondea cada endpoint. Los cuerpos son válidos para
     * evitar que la validación enmascare un resultado: cuando el rol sí está
     * permitido, la petición llega al controlador y responde un estado de
     * negocio (200/201/204/400/404), nunca un 401/403 de la cadena.
     */
    private static MockHttpServletRequestBuilder peticionDe(MatrizAutorizacion.Endpoint endpoint) {
        return switch (endpoint.clave()) {
            case "POST /api/auth/login" -> post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"admin\",\"password\":\"" + PASSWORD_ADMIN + "\"}");
            case "GET /api/auth/me" -> get("/api/auth/me");
            case "POST /api/auth/password" -> post("/api/auth/password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"passwordActual\":\"incorrecta\",\"passwordNueva\":\"nueva-clave-segura\"}");
            case "GET /api/pacientes" -> get("/api/pacientes");
            case "GET /api/pacientes/{id}" -> get("/api/pacientes/PAC-9999");
            case "POST /api/pacientes" -> post("/api/pacientes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(PACIENTE_VALIDO);
            case "PUT /api/pacientes/{id}" -> put("/api/pacientes/PAC-9999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(PACIENTE_VALIDO);
            case "PATCH /api/pacientes/{id}/habitacion" -> patch("/api/pacientes/PAC-9999/habitacion")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"habitacion\":102}");
            case "DELETE /api/pacientes/{id}" -> delete("/api/pacientes/PAC-9999");
            case "GET /api/historial" -> get("/api/historial");
            case "GET /api/pacientes/{id}/historial" -> get("/api/pacientes/PAC-9999/historial");
            case "POST /api/pacientes/{id}/historial" -> post("/api/pacientes/PAC-9999/historial")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(SIGNOS_VITALES);
            case "GET /api/trabajadores" -> get("/api/trabajadores");
            case "GET /api/trabajadores/{id}" -> get("/api/trabajadores/DOC-9999");
            case "POST /api/trabajadores" -> post("/api/trabajadores")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TRABAJADOR_VALIDO);
            case "PUT /api/trabajadores/{id}" -> put("/api/trabajadores/DOC-9999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TRABAJADOR_VALIDO);
            case "DELETE /api/trabajadores/{id}" -> delete("/api/trabajadores/DOC-9999");
            case "GET /api/usuarios" -> get("/api/usuarios");
            case "GET /api/usuarios/{id}" -> get("/api/usuarios/USR-9999");
            case "POST /api/usuarios" -> post("/api/usuarios")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"matriz." + System.nanoTime()
                            + "\",\"password\":\"matriz-12345678\",\"rol\":\"ADMIN\"}");
            case "PATCH /api/usuarios/{id}/rol" -> patch("/api/usuarios/USR-9999/rol")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"rol\":\"DOCTOR\"}");
            case "PATCH /api/usuarios/{id}/estado" -> patch("/api/usuarios/USR-9999/estado")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"activo\":true}");
            case "POST /api/usuarios/{id}/password-reset" -> post("/api/usuarios/USR-9999/password-reset")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"password\":\"nueva-clave-segura\"}");
            default -> throw new IllegalArgumentException(
                    "Endpoint sin sonda en la matriz: " + endpoint.clave());
        };
    }
}