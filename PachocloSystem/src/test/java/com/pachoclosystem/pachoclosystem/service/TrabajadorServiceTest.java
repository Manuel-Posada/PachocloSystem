package com.pachoclosystem.pachoclosystem.service;

import com.pachoclosystem.pachoclosystem.dto.RegistroResponse;
import com.pachoclosystem.pachoclosystem.exception.NotFoundException;
import com.pachoclosystem.pachoclosystem.exception.SolicitudInvalidaException;
import com.pachoclosystem.pachoclosystem.model.Doctor;
import com.pachoclosystem.pachoclosystem.model.Enfermero;
import com.pachoclosystem.pachoclosystem.model.NivelExperiencia;
import com.pachoclosystem.pachoclosystem.model.Paciente;
import com.pachoclosystem.pachoclosystem.model.TipoRegistro;
import com.pachoclosystem.pachoclosystem.model.TrabajadorHospital;
import com.pachoclosystem.pachoclosystem.repository.PacienteRepositoryImpl;
import com.pachoclosystem.pachoclosystem.repository.TrabajadorRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/** Reglas de negocio de los trabajadores, sin contexto Spring. */
class TrabajadorServiceTest {

    private PacienteRepositoryImpl repositorioPacientes;
    private TrabajadorRepositoryImpl repositorio;
    private PacienteService servicioPacientes;
    private TrabajadorService servicio;

    @BeforeEach
    void preparar() {
        repositorioPacientes = new PacienteRepositoryImpl();
        repositorio = new TrabajadorRepositoryImpl();
        servicioPacientes = new PacienteService(repositorioPacientes);
        servicio = new TrabajadorService(repositorio);
    }

    @Test
    void registrarDoctorGeneraIdsConPrefijoDoc() {
        TrabajadorHospital primero = servicio.registrarTrabajador(
                "Carlos Mena", "Doctor", "Cardiologia", null);
        TrabajadorHospital segundo = servicio.registrarTrabajador(
                "Ana Ruiz", "Doctor", "Neurologia", null);

        assertThat(primero.getIdTrabajador()).isEqualTo("DOC-0001");
        assertThat(segundo.getIdTrabajador()).isEqualTo("DOC-0002");
        assertThat(primero).isInstanceOf(Doctor.class);
        assertThat(((Doctor) primero).getEspecialidad()).isEqualTo("Cardiologia");
    }

    @Test
    void registrarEnfermeroGeneraIdsConPrefijoEnf() {
        TrabajadorHospital primero = servicio.registrarTrabajador(
                "Maria Lopez", "Enfermero", null, NivelExperiencia.AVANZADO);
        TrabajadorHospital segundo = servicio.registrarTrabajador(
                "Jose Rios", "Enfermero", null, NivelExperiencia.NOVATO);

        assertThat(primero.getIdTrabajador()).isEqualTo("ENF-0001");
        assertThat(segundo.getIdTrabajador()).isEqualTo("ENF-0002");
        assertThat(primero).isInstanceOf(Enfermero.class);
        assertThat(((Enfermero) primero).getNivelExperiencia()).isEqualTo(NivelExperiencia.AVANZADO);
    }

    @Test
    void losContadoresDeIdSonIndependientesPorRol() {
        TrabajadorHospital enfermero = servicio.registrarTrabajador(
                "Maria Lopez", "Enfermero", null, NivelExperiencia.AVANZADO);
        TrabajadorHospital doctor = servicio.registrarTrabajador(
                "Carlos Mena", "Doctor", "Cardiologia", null);

        assertThat(enfermero.getIdTrabajador()).isEqualTo("ENF-0001");
        assertThat(doctor.getIdTrabajador()).isEqualTo("DOC-0001");
    }

    @Test
    void elNombreSeRecortaAlRegistrar() {
        TrabajadorHospital trabajador = servicio.registrarTrabajador(
                "  Carlos Mena  ", "Doctor", "Cardiologia", null);

        assertThat(trabajador.getNombreCompleto()).isEqualTo("Carlos Mena");
    }

    @Test
    void unDoctorNoPuedeRegistrarseSinEspecialidad() {
        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.registrarTrabajador("Carlos Mena", "Doctor", null, null))
                .withMessage("La especialidad es obligatoria.");

        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.registrarTrabajador("Carlos Mena", "Doctor", "   ", null))
                .withMessage("La especialidad es obligatoria.");
    }

    @Test
    void laEspecialidadDebeSerUnTextoDescriptivo() {
        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.registrarTrabajador("Carlos Mena", "Doctor", "12", null))
                .withMessage("La especialidad debe ser un texto descriptivo (mínimo 3 caracteres).");

        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.registrarTrabajador(
                        "Carlos Mena", "Doctor", "12345678", null))
                .withMessage("La especialidad debe ser un texto descriptivo (mínimo 3 caracteres).");
    }

    @Test
    void unEnfermeroNoPuedeRegistrarseSinNivelDeExperiencia() {
        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.registrarTrabajador("Maria Lopez", "Enfermero", null, null))
                .withMessage("Debe seleccionar un nivel de experiencia.");
    }

    @Test
    void soloSeAdmitenLosRolesDoctorYEnfermero() {
        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.registrarTrabajador("Pedro Gomez", "Medico", null, null))
                .withMessage("Debe seleccionar un rol (Doctor o Enfermero).");
    }

    @Test
    void editarTrabajadorNoPermiteCambiarElRol() {
        TrabajadorHospital doctor = servicio.registrarTrabajador(
                "Carlos Mena", "Doctor", "Cardiologia", null);

        assertThatExceptionOfType(SolicitudInvalidaException.class)
                .isThrownBy(() -> servicio.editarTrabajador(doctor.getIdTrabajador(),
                        "Carlos Mena", "Enfermero", null, NivelExperiencia.NOVATO))
                .withMessage("El rol de un trabajador no puede cambiar (actual: Doctor).");
    }

    @Test
    void editarDoctorActualizaNombreYEspecialidad() {
        TrabajadorHospital doctor = servicio.registrarTrabajador(
                "Carlos Mena", "Doctor", "Cardiologia", null);

        TrabajadorHospital editado = servicio.editarTrabajador(doctor.getIdTrabajador(),
                "Carlos Alberto Mena", "Doctor", "Neurologia", null);

        assertThat(editado.getIdTrabajador()).isEqualTo(doctor.getIdTrabajador());
        assertThat(editado.getNombreCompleto()).isEqualTo("Carlos Alberto Mena");
        assertThat(((Doctor) editado).getEspecialidad()).isEqualTo("Neurologia");
        assertThat(repositorio.buscarPorId(doctor.getIdTrabajador())).isSameAs(editado);
    }

    @Test
    void editarEnfermeroActualizaNombreYNivelSinCambiarElRol() {
        TrabajadorHospital enfermero = servicio.registrarTrabajador(
                "Maria Lopez", "Enfermero", null, NivelExperiencia.NOVATO);

        TrabajadorHospital editado = servicio.editarTrabajador(enfermero.getIdTrabajador(),
                "Maria A. Lopez", "Enfermero", null, NivelExperiencia.AVANZADO);

        assertThat(editado.getNombreCompleto()).isEqualTo("Maria A. Lopez");
        assertThat(((Enfermero) editado).getNivelExperiencia()).isEqualTo(NivelExperiencia.AVANZADO);
        assertThat(repositorio.buscarPorId(enfermero.getIdTrabajador())).isSameAs(editado);
    }

    @Test
    void obtenerTrabajadorInexistenteLanzaNotFound() {
        assertThatExceptionOfType(NotFoundException.class)
                .isThrownBy(() -> servicio.obtenerTrabajador("DOC-9999"))
                .withMessage("No se encontró el trabajador DOC-9999.");
    }

    @Test
    void eliminarTrabajadorInexistenteLanzaNotFound() {
        assertThatExceptionOfType(NotFoundException.class)
                .isThrownBy(() -> servicio.eliminarTrabajador("ENF-9999"))
                .withMessage("No se encontró el trabajador ENF-9999.");
    }

    @Test
    void eliminarTrabajadorExistenteLoQuitaDelRepositorio() {
        TrabajadorHospital enfermero = servicio.registrarTrabajador(
                "Maria Lopez", "Enfermero", null, NivelExperiencia.AVANZADO);

        servicio.eliminarTrabajador(enfermero.getIdTrabajador());

        assertThat(repositorio.buscarPorId(enfermero.getIdTrabajador())).isNull();
        assertThat(servicio.listarTrabajadores(null)).isEmpty();
    }

    @Test
    void listarTrabajadoresFiltraPorNombreOIdSinImportarMayusculas() {
        servicio.registrarTrabajador("Carlos Mena", "Doctor", "Cardiologia", null);
        servicio.registrarTrabajador("Maria Lopez", "Enfermero", null, NivelExperiencia.AVANZADO);

        assertThat(servicio.listarTrabajadores(null)).hasSize(2);
        assertThat(servicio.listarTrabajadores("CARLOS")).hasSize(1);
        assertThat(servicio.listarTrabajadores("  carlos ")).first()
                .extracting(TrabajadorHospital::getNombreCompleto).isEqualTo("Carlos Mena");
        assertThat(servicio.listarTrabajadores("enf-")).hasSize(1);
        assertThat(servicio.listarTrabajadores("zzz")).isEmpty();
    }

    @Test
    void eliminarTrabajadorNoBorraLosRegistrosYaEscritos() {
        Paciente paciente = servicioPacientes.registrarPaciente("Ana Torres", 30, 101);
        Enfermero enfermero = (Enfermero) servicio.registrarTrabajador(
                "Maria Lopez", "Enfermero", null, NivelExperiencia.AVANZADO);
        HistorialClinicoService historial = new HistorialClinicoService(
                repositorioPacientes, repositorio);

        RegistroResponse registro = historial.agregarRegistroPaciente(
                paciente.getIdPaciente(), enfermero.getIdTrabajador(),
                TipoRegistro.EVOLUCION, "Evolucion de Ana", null);

        servicio.eliminarTrabajador(enfermero.getIdTrabajador());

        List<RegistroResponse> registros = historial.obtenerRegistrosPorPaciente(
                paciente.getIdPaciente(), null);
        assertThat(registros).hasSize(1);
        assertThat(registros.get(0).idRegistro()).isEqualTo(registro.idRegistro());
        assertThat(registros.get(0).autor().idTrabajador()).isEqualTo(enfermero.getIdTrabajador());
        assertThat(registros.get(0).autor().nombreCompleto()).isEqualTo("Maria Lopez");
    }
}
