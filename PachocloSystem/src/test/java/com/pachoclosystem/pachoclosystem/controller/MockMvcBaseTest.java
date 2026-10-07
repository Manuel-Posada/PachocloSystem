package com.pachoclosystem.pachoclosystem.controller;

import com.jayway.jsonpath.JsonPath;
import com.pachoclosystem.pachoclosystem.model.NivelExperiencia;
import com.pachoclosystem.pachoclosystem.model.Rol;
import com.pachoclosystem.pachoclosystem.model.Usuario;
import com.pachoclosystem.pachoclosystem.repository.IPacienteRepository;
import com.pachoclosystem.pachoclosystem.repository.ITrabajadoresRepository;
import com.pachoclosystem.pachoclosystem.repository.IUsuarioRepository;
import com.pachoclosystem.pachoclosystem.security.JwtTokenService;
import com.pachoclosystem.pachoclosystem.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base común de los tests MockMvc: arranca la aplicación completa y vacía los
 * repositorios en memoria antes de cada test, de modo que cada prueba es
 * independiente del orden de ejecución y de los datos creados por otras pruebas.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class MockMvcBaseTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected IPacienteRepository repositorioPacientes;

    @Autowired
    protected ITrabajadoresRepository repositorioTrabajadores;

    @Autowired
    protected IUsuarioRepository repositorioUsuarios;

    @Autowired
    protected JwtTokenService tokenService;

    @Autowired
    protected UsuarioService usuarioService;

    @BeforeEach
    void limpiarRepositorios() {
        repositorioPacientes.obtenerTodos()
                .forEach(paciente -> repositorioPacientes.eliminar(paciente.getIdPaciente()));
        repositorioTrabajadores.obtenerTodos()
                .forEach(trabajador -> repositorioTrabajadores.eliminarTrabajador(trabajador.getIdTrabajador()));
    }

    /**
     * Fontanería de autenticación para los tests existentes: inyecta en la
     * petición la cabecera {@code Authorization} con un token JWT real (firmado
     * con el mismo encoder que usa el login) del administrador creado por
     * {@code AdminInicial} al arrancar el contexto. Ningún assert cambia.
     */
    protected MockHttpServletRequestBuilder autenticado(MockHttpServletRequestBuilder peticion) {
        return peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin());
    }

    /** Ejecuta una petición ya autenticada con el token del administrador. */
    protected ResultActions perform(MockHttpServletRequestBuilder peticion) throws Exception {
        return mockMvc.perform(autenticado(peticion));
    }

    /** Ejecuta una petición sin cabecera {@code Authorization} (para probar el 401). */
    protected ResultActions sinToken(MockHttpServletRequestBuilder peticion) throws Exception {
        return mockMvc.perform(peticion);
    }

    /** Token JWT del usuario administrador (la contraseña de prueba está en application.properties de test). */
    protected String tokenAdmin() {
        Usuario admin = repositorioUsuarios.buscarPorUsername("admin");
        if (admin == null) {
            throw new IllegalStateException(
                    "No existe el usuario administrador 'admin' en el contexto de prueba.");
        }
        return tokenService.generarToken(admin);
    }

    /** Lee un valor del cuerpo de la respuesta (siempre en UTF-8). */
    protected String leer(MvcResult resultado, String expresion) throws Exception {
        return JsonPath.read(
                resultado.getResponse().getContentAsString(StandardCharsets.UTF_8), expresion);
    }

    /** Registra un paciente válido y devuelve su ID (p. ej. PAC-0001). */
    protected String registrarPaciente(String nombre, int edad, int habitacion) throws Exception {
        MvcResult resultado = perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s","edad":%d,"habitacion":%d}
                                """.formatted(nombre, edad, habitacion)))
                .andExpect(status().isCreated())
                .andReturn();
        return leer(resultado, "$.idPaciente");
    }

    /** Registra un doctor válido y devuelve su ID (p. ej. DOC-0001). */
    protected String registrarDoctor(String nombre, String especialidad) throws Exception {
        MvcResult resultado = perform(post("/api/trabajadores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s","rol":"Doctor","especialidad":"%s"}
                                """.formatted(nombre, especialidad)))
                .andExpect(status().isCreated())
                .andReturn();
        return leer(resultado, "$.idTrabajador");
    }

    /** Registra un enfermero válido y devuelve su ID (p. ej. ENF-0001). */
    protected String registrarEnfermero(String nombre, NivelExperiencia nivel) throws Exception {
        MvcResult resultado = perform(post("/api/trabajadores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s","rol":"Enfermero","nivelExperiencia":"%s"}
                                """.formatted(nombre, nivel)))
                .andExpect(status().isCreated())
                .andReturn();
        return leer(resultado, "$.idTrabajador");
    }

    /** Agrega un registro al historial de un paciente y devuelve su ID. */
    protected String registrarRegistro(String idPaciente, String cuerpo) throws Exception {
        MvcResult resultado = postHistorial(idPaciente, cuerpo)
                .andExpect(status().isCreated())
                .andReturn();
        return leer(resultado, "$.idRegistro");
    }

    /**
     * Crea un registro firmado por el trabajador del campo {@code idAutor} del
     * cuerpo: el autor sale del token, así que se usa el token de su usuario.
     */
    protected ResultActions postHistorial(String idPaciente, String cuerpo) throws Exception {
        String idAutor = JsonPath.read(cuerpo, "$.idAutor");
        return postHistorialComo(idAutor, idPaciente, cuerpo);
    }

    /** Crea un registro con el token del usuario del trabajador indicado. */
    protected ResultActions postHistorialComo(String idTrabajador, String idPaciente, String cuerpo)
            throws Exception {
        return mockMvc.perform(post("/api/pacientes/{id}/historial", idPaciente)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDeTrabajador(idTrabajador))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    /**
     * Token del usuario vinculado a ese trabajador (DOCTOR o ENFERMERO según su
     * ID); si aún no tiene usuario, lo crea con un username único (el
     * repositorio de usuarios no se vacía entre tests).
     */
    protected String tokenDeTrabajador(String idTrabajador) {
        Usuario usuario = repositorioUsuarios.buscarPorIdTrabajador(idTrabajador);
        if (usuario == null) {
            Rol rol = idTrabajador.startsWith("DOC") ? Rol.DOCTOR : Rol.ENFERMERO;
            String username = "t." + idTrabajador.toLowerCase(Locale.ROOT) + "."
                    + UUID.randomUUID().toString().substring(0, 8);
            usuario = usuarioService.crearUsuario(username, "clave-de-pruebas-10", rol, idTrabajador);
        }
        return tokenService.generarToken(usuario);
    }
}
