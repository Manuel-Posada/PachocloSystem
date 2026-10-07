package com.pachoclosystem.pachoclosystem;

import com.pachoclosystem.pachoclosystem.dto.RegistroRequest;
import com.pachoclosystem.pachoclosystem.dto.RegistroResponse;
import com.pachoclosystem.pachoclosystem.model.Paciente;
import com.pachoclosystem.pachoclosystem.model.Rol;
import com.pachoclosystem.pachoclosystem.model.TipoRegistro;
import com.pachoclosystem.pachoclosystem.model.Usuario;
import com.pachoclosystem.pachoclosystem.security.AuthService;
import com.pachoclosystem.pachoclosystem.security.JwtTokenService;
import com.pachoclosystem.pachoclosystem.security.JwtUsuarioAuthenticationConverter;
import com.pachoclosystem.pachoclosystem.service.HistorialClinicoService;
import com.pachoclosystem.pachoclosystem.service.HistorialIdempotenteService;
import com.pachoclosystem.pachoclosystem.service.PacienteService;
import com.pachoclosystem.pachoclosystem.service.TrabajadorService;
import com.pachoclosystem.pachoclosystem.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Persistencia real: lo creado en un arranque de la aplicación sigue ahí en el
 * siguiente (usuarios, pacientes, trabajadores, registros y claves de
 * idempotencia), y un token emitido antes del reinicio sigue valiendo con el
 * mismo {@code app.jwt.secret}.
 */
class ReinicioPersistenciaTest {

    private static final String PASSWORD = "password-segura-reinicio-1";

    private static ConfigurableApplicationContext arrancar(String... argumentos) {
        String[] todos = new String[argumentos.length + 1];
        todos[0] = "--server.port=0";
        System.arraycopy(argumentos, 0, todos, 1, argumentos.length);
        return new SpringApplicationBuilder(PachocloSystemApplication.class).run(todos);
    }

    @Test
    void losDatosYLasClavesSobrevivenAUnReinicio() {
        String clave = UUID.randomUUID().toString();
        RegistroRequest peticion = new RegistroRequest(TipoRegistro.EVOLUCION, null, "Paciente estable",
                null, null, null);
        String idPaciente;
        String idBaja;
        String idDoctor;
        String token;
        RegistroResponse creado;

        // Primer arranque: esquema limpio.
        try (ConfigurableApplicationContext primero = arrancar()) {
            PacienteService pacientes = primero.getBean(PacienteService.class);
            idPaciente = pacientes.registrarPaciente("Ana Torres", 30, 101).getIdPaciente();
            idBaja = pacientes.registrarPaciente("Luis Gil", 40, 102).getIdPaciente();
            pacientes.eliminarPaciente(idBaja);
            idDoctor = primero.getBean(TrabajadorService.class)
                    .registrarTrabajador("Carlos Mena", "Doctor", "Cardiologia", null).getIdTrabajador();
            Usuario doctor = primero.getBean(UsuarioService.class)
                    .crearUsuario("carlos.mena", PASSWORD, Rol.DOCTOR, idDoctor);
            token = primero.getBean(JwtTokenService.class).generarToken(doctor);
            creado = primero.getBean(HistorialIdempotenteService.class)
                    .agregarRegistro(doctor, idPaciente, peticion, clave).registro();
        }

        // Segundo arranque sobre los mismos datos.
        try (ConfigurableApplicationContext segundo =
                     arrancar("--" + EsquemaLimpioEnTestsConfig.PROP_CONSERVAR_DATOS + "=true")) {
            PacienteService pacientes = segundo.getBean(PacienteService.class);
            Paciente ana = pacientes.obtenerPaciente(idPaciente);
            assertThat(ana.getNombre()).isEqualTo("Ana Torres");
            assertThat(ana.getHabitacion()).isEqualTo(101);
            assertThat(pacientes.listarPacientes(null)).extracting(Paciente::getIdPaciente)
                    .containsExactly(idPaciente);
            assertThat(segundo.getBean(TrabajadorService.class).obtenerTrabajador(idDoctor).getNombreCompleto())
                    .isEqualTo("Carlos Mena");

            List<RegistroResponse> historial = segundo.getBean(HistorialClinicoService.class)
                    .obtenerRegistrosPorPaciente(idPaciente, null);
            assertThat(historial).containsExactly(creado);

            // La misma clave devuelve el mismo registro sin crear otro.
            Usuario doctor = segundo.getBean(UsuarioService.class).buscarPorUsername("carlos.mena");
            HistorialIdempotenteService.Resultado repetido = segundo.getBean(HistorialIdempotenteService.class)
                    .agregarRegistro(doctor, idPaciente, peticion, clave);
            assertThat(repetido.repetido()).isTrue();
            assertThat(repetido.registro()).isEqualTo(creado);
            assertThat(segundo.getBean(HistorialClinicoService.class)
                    .obtenerRegistrosPorPaciente(idPaciente, null)).hasSize(1);

            // El token anterior sigue valiendo y la contraseña también.
            JwtUsuarioAuthenticationConverter conversor = segundo.getBean(JwtUsuarioAuthenticationConverter.class);
            JwtDecoder decodificador = segundo.getBean(JwtDecoder.class);
            assertThatNoException().isThrownBy(() -> conversor.convert(decodificador.decode(token)));
            assertThat(segundo.getBean(AuthService.class).iniciarSesion("carlos.mena", PASSWORD, "10.0.0.9").token())
                    .isNotBlank();
        }
    }
}
