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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base común de los tests MockMvc: arranca la aplicación completa y vacía los
 * repositorios en memoria antes de cada test, de modo que cada prueba es
 * independiente del orden de ejecución y de los datos creados por otras pruebas.
 *
 * <p>Ofrece tokens JWT reales para los tres roles ({@link #performComo(Rol, MockHttpServletRequestBuilder)}):
 * el administrador lo crea {@code AdminInicial} al arrancar y los usuarios
 * DOCTOR/ENFERMERO se crean bajo demanda vinculados a trabajadores reales del
 * test (la autorización necesita que el rol exista en el repositorio).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class MockMvcBaseTest {

    /** Contraseña (de prueba) de los usuarios creados por la fontanería de tests. */
    private static final String PASSWORD_PRUEBA = "password-segura-12345";

    /** Sufijo único y corto para los usernames de prueba (respeta el máximo de 30). */
    private static final AtomicLong CONTADOR_USERNAME = new AtomicLong();

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
    protected UsuarioService servicioUsuarios;

    /** Token de apoyo por rol (uno por test, creado bajo demanda). */
    private final Map<Rol, String> tokensPorRol = new EnumMap<>(Rol.class);

    /** Token por trabajador (uno por test) para atribuir registros a un autor concreto. */
    private final Map<String, String> tokensPorTrabajador = new HashMap<>();

    @BeforeEach
    void limpiarRepositorios() {
        repositorioPacientes.obtenerTodos()
                .forEach(paciente -> repositorioPacientes.eliminar(paciente.getIdPaciente()));
        repositorioTrabajadores.obtenerTodos()
                .forEach(trabajador -> repositorioTrabajadores.eliminarTrabajador(trabajador.getIdTrabajador()));
    }

    /**
     * Inyecta en la petición la cabecera {@code Authorization} con el token del
     * administrador creado por {@code AdminInicial} al arrancar el contexto.
     */
    protected MockHttpServletRequestBuilder autenticado(MockHttpServletRequestBuilder peticion) {
        return peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin());
    }

    /** Ejecuta una petición ya autenticada con el token del administrador. */
    protected ResultActions perform(MockHttpServletRequestBuilder peticion) throws Exception {
        return mockMvc.perform(autenticado(peticion));
    }

    /** Ejecuta una petición autenticada con un token real del rol indicado. */
    protected ResultActions performComo(Rol rol, MockHttpServletRequestBuilder peticion) throws Exception {
        return mockMvc.perform(autenticadoComo(rol, peticion));
    }

    /** Añade a la petición el token bearer del rol indicado (sin ejecutarla). */
    protected MockHttpServletRequestBuilder autenticadoComo(Rol rol, MockHttpServletRequestBuilder peticion) {
        return peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDeRol(rol));
    }

    /** Ejecuta una petición con un token JWT ya obtenido. */
    protected ResultActions performConToken(String token, MockHttpServletRequestBuilder peticion) throws Exception {
        return mockMvc.perform(peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    /** Ejecuta una petición con el token real del trabajador autor indicado. */
    protected ResultActions performComoAutor(Rol rol, String idTrabajador,
                                             MockHttpServletRequestBuilder peticion) throws Exception {
        return performConToken(tokenDeTrabajador(rol, idTrabajador), peticion);
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

    /**
     * Token JWT de un usuario del rol indicado. Para ADMIN es el administrador;
     * para DOCTOR/ENFERMERO se crea (una vez por test) un trabajador del tipo
     * correcto y su usuario vinculado.
     */
    protected String tokenDeRol(Rol rol) {
        if (rol == Rol.ADMIN) {
            return tokenAdmin();
        }
        String existente = tokensPorRol.get(rol);
        if (existente != null) {
            return existente;
        }
        String token = tokenDeTrabajador(rol, crearTrabajadorDeApoyo(rol));
        tokensPorRol.put(rol, token);
        return token;
    }

    /**
     * Token del usuario del rol indicado vinculado al trabajador {@code idTrabajador}.
     * El usuario se crea (una vez por trabajador y test) con un username único.
     */
    protected String tokenDeTrabajador(Rol rol, String idTrabajador) {
        String existente = tokensPorTrabajador.get(idTrabajador);
        if (existente != null) {
            return existente;
        }
        String username = rol.name().toLowerCase() + "." + CONTADOR_USERNAME.incrementAndGet();
        Usuario usuario = servicioUsuarios.crearUsuario(username, PASSWORD_PRUEBA, rol, idTrabajador);
        String token = tokenService.generarToken(usuario);
        tokensPorTrabajador.put(idTrabajador, token);
        return token;
    }

    /** Lee un valor del cuerpo de la respuesta (siempre en UTF-8). */
    protected String leer(MvcResult resultado, String expresion) throws Exception {
        return JsonPath.read(
                resultado.getResponse().getContentAsString(StandardCharsets.UTF_8), expresion);
    }

    /** Registra un paciente válido y devuelve su ID (p. ej. PAC-0001). Solo DOCTOR puede darlo de alta. */
    protected String registrarPaciente(String nombre, int edad, int habitacion) throws Exception {
        MvcResult resultado = performComo(Rol.DOCTOR, post("/api/pacientes")
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

    /**
     * Agrega un registro al historial de un paciente y devuelve su ID. La
     * petición se autentica con un token real del trabajador autor, que es el
     * autor que el servicio toma del usuario autenticado (el cuerpo ya no lleva
     * {@code idAutor}).
     */
    protected String registrarRegistro(String idPaciente, Rol rol, String idAutor, String cuerpo) throws Exception {
        MvcResult resultado = performConToken(tokenDeTrabajador(rol, idAutor),
                post("/api/pacientes/{id}/historial", idPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn();
        return leer(resultado, "$.idRegistro");
    }

    /** Crea el trabajador de apoyo (sin usuario) para cuando solo se necesita el rol. */
    private String crearTrabajadorDeApoyo(Rol rol) {
        try {
            return switch (rol) {
                case DOCTOR -> registrarDoctor("Doctor Apoyo", "General");
                case ENFERMERO -> registrarEnfermero("Enfermero Apoyo", NivelExperiencia.NOVATO);
                default -> throw new IllegalArgumentException("Rol sin trabajador de apoyo: " + rol);
            };
        } catch (Exception fallo) {
            throw new IllegalStateException("No se pudo crear el trabajador de apoyo para " + rol, fallo);
        }
    }
}