package com.pachoclosystem.pachoclosystem.repository;

import com.pachoclosystem.pachoclosystem.controller.MockMvcBaseTest;
import com.pachoclosystem.pachoclosystem.dto.RegistroResponse;
import com.pachoclosystem.pachoclosystem.dto.TrabajadorResponse;
import com.pachoclosystem.pachoclosystem.model.AutorRegistro;
import com.pachoclosystem.pachoclosystem.model.Doctor;
import com.pachoclosystem.pachoclosystem.model.Enfermero;
import com.pachoclosystem.pachoclosystem.model.NivelExperiencia;
import com.pachoclosystem.pachoclosystem.model.Paciente;
import com.pachoclosystem.pachoclosystem.model.RegistroClinico;
import com.pachoclosystem.pachoclosystem.model.Rol;
import com.pachoclosystem.pachoclosystem.model.TipoRegistro;
import com.pachoclosystem.pachoclosystem.model.TrabajadorHospital;
import com.pachoclosystem.pachoclosystem.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Implementaciones JDBC de los repositorios contra la base PostgreSQL de
 * pruebas: lo que se guarda se relee de la base (no de un objeto en memoria),
 * los ids salen de las secuencias y las restricciones de la tabla actúan.
 */
class RepositoriosJdbcTest extends MockMvcBaseTest {

    private static final String HASH = "$2a$10$de.mentira.para.el.repositorio.jdbc";

    @Autowired
    private IRegistroClinicoRepository repositorioRegistros;

    @Autowired
    private IHistorialIdempotenciaRepository repositorioIdempotencia;

    // ---------------------------------------------------------------- pacientes

    @Test
    void pacientesSeGuardanYSeActualizanEnLaBase() {
        assertThat(repositorioPacientes.generarNuevoId()).isEqualTo("PAC-0001");
        assertThat(repositorioPacientes.generarNuevoId()).isEqualTo("PAC-0002");
        assertThat(repositorioPacientes.guardarPaciente(new Paciente("PAC-0002", "Ana Torres", 30, 101))).isTrue();
        assertThat(repositorioPacientes.guardarPaciente(new Paciente("PAC-0001", "Luis Gil", 40, 102))).isTrue();

        assertThat(repositorioPacientes.actualizarDatos("PAC-0002", "Ana Torres Ruiz", 31, 201)).isTrue();
        assertThat(repositorioPacientes.actualizarHabitacion("PAC-0001", 305)).isTrue();

        Paciente ana = repositorioPacientes.buscarPorId("PAC-0002");
        assertThat(ana.getNombre()).isEqualTo("Ana Torres Ruiz");
        assertThat(ana.getEdad()).isEqualTo(31);
        assertThat(ana.getHabitacion()).isEqualTo(201);
        assertThat(ana.isActivo()).isTrue();
        assertThat(repositorioPacientes.buscarPorId("PAC-0001").getHabitacion()).isEqualTo(305);
        // En orden de alta, no de id.
        assertThat(repositorioPacientes.obtenerTodos()).extracting(Paciente::getIdPaciente)
                .containsExactly("PAC-0002", "PAC-0001");
        assertThat(repositorioPacientes.buscarPorId("PAC-9999")).isNull();
    }

    @Test
    void unPacienteDadoDeBajaNoSeEditaNiSeDaDeBajaOtraVez() {
        repositorioPacientes.guardarPaciente(new Paciente("PAC-0001", "Ana Torres", 30, 101));

        assertThat(repositorioPacientes.darDeBaja("PAC-0001")).isTrue();
        assertThat(repositorioPacientes.darDeBaja("PAC-0001")).isFalse();
        assertThat(repositorioPacientes.actualizarDatos("PAC-0001", "Otro", 20, 102)).isFalse();
        assertThat(repositorioPacientes.actualizarHabitacion("PAC-0001", 102)).isFalse();

        Paciente baja = repositorioPacientes.buscarPorId("PAC-0001");
        assertThat(baja.isActivo()).isFalse();
        assertThat(baja.getNombre()).isEqualTo("Ana Torres");
        assertThat(repositorioPacientes.darDeBaja("PAC-9999")).isFalse();
    }

    @Test
    void idDePacienteRepetidoNoSeGuarda() {
        assertThat(repositorioPacientes.guardarPaciente(new Paciente("PAC-0001", "Ana Torres", 30, 101))).isTrue();
        assertThat(repositorioPacientes.guardarPaciente(new Paciente("PAC-0001", "Luis Gil", 40, 102))).isFalse();
        assertThat(repositorioPacientes.buscarPorId("PAC-0001").getNombre()).isEqualTo("Ana Torres");
    }

    // ------------------------------------------------------------- trabajadores

    @Test
    void trabajadoresDeAmbosTiposConSusPropiasSecuencias() {
        assertThat(repositorioTrabajadores.generarNuevoId("DOC")).isEqualTo("DOC-0001");
        assertThat(repositorioTrabajadores.generarNuevoId("ENF")).isEqualTo("ENF-0001");
        assertThat(repositorioTrabajadores.generarNuevoId("DOC")).isEqualTo("DOC-0002");
        repositorioTrabajadores.guardarTrabajador(new Enfermero("ENF-0001", "Maria Lopez", NivelExperiencia.AVANZADO));
        repositorioTrabajadores.guardarTrabajador(new Doctor("DOC-0001", "Carlos Mena", "Cardiologia"));

        TrabajadorHospital doctor = repositorioTrabajadores.buscarPorId("DOC-0001");
        TrabajadorHospital enfermero = repositorioTrabajadores.buscarPorId("ENF-0001");
        assertThat(doctor).isInstanceOf(Doctor.class);
        assertThat(((Doctor) doctor).getEspecialidad()).isEqualTo("Cardiologia");
        assertThat(enfermero).isInstanceOf(Enfermero.class);
        assertThat(((Enfermero) enfermero).getNivelExperiencia()).isEqualTo(NivelExperiencia.AVANZADO);
        assertThat(repositorioTrabajadores.obtenerTodos()).extracting(TrabajadorHospital::getIdTrabajador)
                .containsExactly("ENF-0001", "DOC-0001");
    }

    @Test
    void actualizarYEliminarTrabajador() {
        repositorioTrabajadores.guardarTrabajador(new Doctor("DOC-0001", "Carlos Mena", "Cardiologia"));

        assertThat(repositorioTrabajadores.actualizarTrabajador(
                new Doctor("DOC-0001", "Carlos Mena Ruiz", "Neurologia"))).isTrue();
        Doctor releido = (Doctor) repositorioTrabajadores.buscarPorId("DOC-0001");
        assertThat(releido.getNombreCompleto()).isEqualTo("Carlos Mena Ruiz");
        assertThat(releido.getEspecialidad()).isEqualTo("Neurologia");

        assertThat(repositorioTrabajadores.eliminarTrabajador("DOC-0001")).isTrue();
        assertThat(repositorioTrabajadores.eliminarTrabajador("DOC-0001")).isFalse();
        assertThat(repositorioTrabajadores.buscarPorId("DOC-0001")).isNull();
        assertThat(repositorioTrabajadores.actualizarTrabajador(
                new Doctor("DOC-0001", "Nadie", "General"))).isFalse();
    }

    // ----------------------------------------------------------------- usuarios

    @Test
    void usuarioSeGuardaConSusCredencialesYElUsernameEsUnico() {
        Usuario usuario = new Usuario("USR-9001", "ana.torres", HASH, Rol.ADMIN, null, true);

        assertThat(repositorioUsuarios.guardar(usuario)).isTrue();
        assertThat(repositorioUsuarios.guardar(new Usuario("USR-9002", "ana.torres", HASH, Rol.ADMIN, null)))
                .isFalse();

        Usuario releido = repositorioUsuarios.buscarPorUsername("  ANA.Torres ");
        assertThat(releido).isEqualTo(usuario).isNotSameAs(usuario);
        assertThat(releido.getCredenciales().hash()).isEqualTo(HASH);
        assertThat(releido.getCredenciales().version()).isZero();
        assertThat(releido.getCredenciales().debeCambiarPassword()).isTrue();
        assertThat(releido.isActivo()).isTrue();
        assertThat(repositorioUsuarios.existePorUsername("ana.torres")).isTrue();
        assertThat(repositorioUsuarios.buscarPorId("USR-9002")).isNull();
    }

    @Test
    void unTrabajadorSoloPuedeTenerUnUsuario() {
        repositorioUsuarios.guardar(new Usuario("USR-9001", "carlos.mena", HASH, Rol.DOCTOR, "DOC-0001"));

        assertThat(repositorioUsuarios.guardar(new Usuario("USR-9002", "otro.doctor", HASH, Rol.DOCTOR, "DOC-0001")))
                .isFalse();
        repositorioUsuarios.guardar(new Usuario("USR-9003", "maria.lopez", HASH, Rol.ENFERMERO, "ENF-0001"));
        assertThat(repositorioUsuarios.cambiarRol("USR-9003", Rol.DOCTOR, "DOC-0001")).isFalse();
        assertThat(repositorioUsuarios.buscarPorId("USR-9003").getIdTrabajador()).isEqualTo("ENF-0001");
        assertThat(repositorioUsuarios.buscarPorIdTrabajador("DOC-0001").getIdUsuario()).isEqualTo("USR-9001");
    }

    @Test
    void desactivarSubeLaVersionUnaSolaVezYCambiarPasswordLaVuelveASubir() {
        repositorioUsuarios.guardar(new Usuario("USR-9001", "ana.torres", HASH, Rol.ADMIN, null));

        assertThat(repositorioUsuarios.desactivar("USR-9001")).isTrue();
        assertThat(repositorioUsuarios.desactivar("USR-9001")).isFalse();
        Usuario inactivo = repositorioUsuarios.buscarPorId("USR-9001");
        assertThat(inactivo.isActivo()).isFalse();
        assertThat(inactivo.getCredenciales().version()).isEqualTo(1);

        repositorioUsuarios.reactivar("USR-9001");
        repositorioUsuarios.cambiarPassword("USR-9001", "$2a$10$otro.hash", true);
        Usuario reactivado = repositorioUsuarios.buscarPorId("USR-9001");
        assertThat(reactivado.isActivo()).isTrue();
        assertThat(reactivado.getCredenciales().version()).isEqualTo(2);
        assertThat(reactivado.getCredenciales().hash()).isEqualTo("$2a$10$otro.hash");
        assertThat(reactivado.getCredenciales().debeCambiarPassword()).isTrue();
    }

    @Test
    void laBaseRechazaUnAdministradorConTrabajador() {
        assertThatExceptionOfType(DataIntegrityViolationException.class).isThrownBy(() ->
                jdbc.sql("INSERT INTO usuarios (id_usuario, username, password_hash, rol, id_trabajador) "
                        + "VALUES ('USR-9001', 'x.admin', 'h', 'ADMIN', 'DOC-0001')").update());
    }

    // ---------------------------------------------------------------- registros

    @Test
    void registroGuardaLaCopiaDelAutorYLaFechaExacta() {
        repositorioPacientes.guardarPaciente(new Paciente("PAC-0001", "Ana Torres", 30, 101));
        Doctor autor = new Doctor("DOC-0001", "Carlos Mena", "Cardiologia");
        RegistroClinico registro = new RegistroClinico(TipoRegistro.MEDICACION, "Paracetamol", autor, "MED-0001", 2);

        repositorioRegistros.insertar("PAC-0001", registro);

        RegistroClinico releido = repositorioRegistros.listarPorPaciente("PAC-0001").getFirst();
        assertThat(releido.getIdRegistro()).isEqualTo(registro.getIdRegistro());
        assertThat(releido.getFecha()).isEqualTo(registro.getFecha());
        assertThat(releido.getTipo()).isEqualTo(TipoRegistro.MEDICACION);
        assertThat(releido.getContenido()).isEqualTo("Paracetamol");
        assertThat(releido.getAutor()).isEqualTo(
                new AutorRegistro("DOC-0001", "Carlos Mena", "Doctor", "Cardiologia", null));
        assertThat(releido.getIdMedicamento()).isEqualTo("MED-0001");
        assertThat(releido.getCantidad()).isEqualTo(2);
    }

    @Test
    void registroDeEnfermeroSinMedicacion() {
        repositorioPacientes.guardarPaciente(new Paciente("PAC-0001", "Ana Torres", 30, 101));
        RegistroClinico registro = new RegistroClinico(TipoRegistro.EVOLUCION, "Estable",
                new Enfermero("ENF-0001", "Maria Lopez", NivelExperiencia.NOVATO));

        repositorioRegistros.insertar("PAC-0001", registro);

        RegistroClinico releido = repositorioRegistros.listarPorPaciente("PAC-0001").getFirst();
        assertThat(releido.getAutor()).isEqualTo(
                new AutorRegistro("ENF-0001", "Maria Lopez", "Enfermero", null, NivelExperiencia.NOVATO));
        assertThat(releido.getIdMedicamento()).isNull();
        assertThat(releido.getCantidad()).isNull();
    }

    @Test
    void elHistorialGlobalExcluyeBajasYOrdenaPorFecha() {
        repositorioPacientes.guardarPaciente(new Paciente("PAC-0001", "Ana Torres", 30, 101));
        repositorioPacientes.guardarPaciente(new Paciente("PAC-0002", "Luis Gil", 40, 102));
        repositorioPacientes.guardarPaciente(new Paciente("PAC-0003", "Eva Ruiz", 50, 103));
        Doctor autor = new Doctor("DOC-0001", "Carlos Mena", "Cardiologia");
        LocalDateTime base = LocalDateTime.of(2026, 6, 15, 10, 0);
        insertar("PAC-0002", base.plusMinutes(2), autor);
        insertar("PAC-0001", base.plusMinutes(1), autor);
        insertar("PAC-0003", base, autor);
        // Misma fecha en dos pacientes: primero el paciente dado de alta antes.
        insertar("PAC-0002", base.plusMinutes(3), autor);
        insertar("PAC-0001", base.plusMinutes(3), autor);
        repositorioPacientes.darDeBaja("PAC-0003");

        List<IRegistroClinicoRepository.RegistroDePaciente> todos = repositorioRegistros.listarDePacientesActivos();

        assertThat(todos).extracting(IRegistroClinicoRepository.RegistroDePaciente::idPaciente)
                .containsExactly("PAC-0001", "PAC-0002", "PAC-0001", "PAC-0002");
        assertThat(todos.getFirst().nombrePaciente()).isEqualTo("Ana Torres");
        // El paciente dado de baja conserva su historial.
        assertThat(repositorioRegistros.listarPorPaciente("PAC-0003")).hasSize(1);
    }

    @Test
    void unRegistroNecesitaUnPacienteExistente() {
        RegistroClinico registro = new RegistroClinico(TipoRegistro.EVOLUCION, "Estable",
                new Doctor("DOC-0001", "Carlos Mena", "Cardiologia"));

        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(() -> repositorioRegistros.insertar("PAC-9999", registro));
    }

    // -------------------------------------------------------------- idempotencia

    @Test
    void laClaveGuardaElRegistroComoJsonYLoDevuelveIgual() {
        Instant ahora = Instant.parse("2026-06-15T10:00:00.123456Z");
        RegistroResponse registro = new RegistroResponse("reg-1", "PAC-0001", "Ana Torres",
                LocalDateTime.of(2026, 6, 15, 10, 0, 0, 123_456_000), TipoRegistro.MEDICACION,
                new TrabajadorResponse("DOC-0001", "Carlos Mena", "Doctor",
                        "Cardiologia", null),
                "Paracetamol", new RegistroResponse.MedicacionResponse("MED-0001", 2));

        repositorioIdempotencia.guardar("clave-uno-0000000", "USR-0001", "PAC-0001", "h".repeat(64), null, ahora);
        assertThat(repositorioIdempotencia.buscarVigente("clave-uno-0000000", ahora.minusSeconds(1)))
                .hasValueSatisfying(uso -> assertThat(uso.registro()).isNull());

        repositorioIdempotencia.guardar("clave-uno-0000000", "USR-0001", "PAC-0001", "h".repeat(64), registro,
                ahora);
        IHistorialIdempotenciaRepository.UsoGuardado uso =
                repositorioIdempotencia.buscarVigente("clave-uno-0000000", ahora.minusSeconds(1)).orElseThrow();
        assertThat(uso.registro()).isEqualTo(registro);
        assertThat(uso.usadaEn()).isEqualTo(ahora);
        assertThat(uso.idUsuario()).isEqualTo("USR-0001");
        assertThat(uso.huella()).isEqualTo("h".repeat(64));
        assertThat(repositorioIdempotencia.contarVigentes(ahora.minusSeconds(1))).isEqualTo(1);
    }

    @Test
    void lasClavesCaducadasNoSeDevuelvenYSePurgan() {
        Instant ahora = Instant.parse("2026-06-15T10:00:00Z").truncatedTo(ChronoUnit.MICROS);
        repositorioIdempotencia.guardar("vieja-0000000000000", "USR-0001", "PAC-0001", "h".repeat(64), null,
                ahora.minusSeconds(3600));
        repositorioIdempotencia.guardar("nueva-0000000000000", "USR-0001", "PAC-0001", "h".repeat(64), null, ahora);

        // Límite = momento de uso: ya caducada.
        assertThat(repositorioIdempotencia.buscarVigente("vieja-0000000000000", ahora.minusSeconds(3600))).isEmpty();
        assertThat(repositorioIdempotencia.purgarCaducadas(ahora.minusSeconds(3600))).isEqualTo(1);
        assertThat(repositorioIdempotencia.contarVigentes(Instant.EPOCH)).isEqualTo(1);
    }

    private void insertar(String idPaciente, LocalDateTime fecha, TrabajadorHospital autor) {
        repositorioRegistros.insertar(idPaciente, new RegistroClinico(UUID.randomUUID().toString(), fecha,
                AutorRegistro.de(autor), TipoRegistro.EVOLUCION, "Estable", null, null));
    }
}
