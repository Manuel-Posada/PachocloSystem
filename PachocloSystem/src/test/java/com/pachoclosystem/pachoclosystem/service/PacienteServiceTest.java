package com.pachoclosystem.pachoclosystem.service;

import com.pachoclosystem.pachoclosystem.exception.NotFoundException;
import com.pachoclosystem.pachoclosystem.model.Paciente;
import com.pachoclosystem.pachoclosystem.repository.PacienteRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/** Reglas de negocio de los pacientes, sin contexto Spring. */
class PacienteServiceTest {

    private PacienteRepositoryEnMemoria repositorio;
    private PacienteService servicio;

    @BeforeEach
    void preparar() {
        repositorio = new PacienteRepositoryEnMemoria();
        servicio = new PacienteService(repositorio);
    }

    @Test
    void registrarPacienteGeneraIdsConPrefijoPac() {
        Paciente primero = servicio.registrarPaciente("Ana Torres", 30, 101);
        Paciente segundo = servicio.registrarPaciente("Bruno Diaz", 45, 202);

        assertThat(primero.getIdPaciente()).isEqualTo("PAC-0001");
        assertThat(segundo.getIdPaciente()).isEqualTo("PAC-0002");
        assertThat(primero.getNombre()).isEqualTo("Ana Torres");
        assertThat(primero.getEdad()).isEqualTo(30);
        assertThat(primero.getHabitacion()).isEqualTo(101);
    }

    @Test
    void registrarPacienteRecortaElNombre() {
        Paciente paciente = servicio.registrarPaciente("  Ana Torres  ", 30, 101);

        assertThat(paciente.getNombre()).isEqualTo("Ana Torres");
    }

    @Test
    void editarPacienteActualizaNombreEdadYHabitacion() {
        Paciente paciente = servicio.registrarPaciente("Ana Torres", 30, 101);

        Paciente editado = servicio.editarPaciente(
                paciente.getIdPaciente(), "  Ana Maria Torres ", 31, 205);

        assertThat(editado.getIdPaciente()).isEqualTo(paciente.getIdPaciente());
        assertThat(editado.getNombre()).isEqualTo("Ana Maria Torres");
        assertThat(editado.getEdad()).isEqualTo(31);
        assertThat(editado.getHabitacion()).isEqualTo(205);
        assertThat(repositorio.buscarPorId(paciente.getIdPaciente())).isSameAs(editado);
    }

    @Test
    void editarHabitacionNoTocaElRestoDeDatos() {
        Paciente paciente = servicio.registrarPaciente("Ana Torres", 30, 101);

        Paciente editado = servicio.editarHabitacion(paciente.getIdPaciente(), 310);

        assertThat(editado.getNombre()).isEqualTo("Ana Torres");
        assertThat(editado.getEdad()).isEqualTo(30);
        assertThat(editado.getHabitacion()).isEqualTo(310);
    }

    @Test
    void obtenerPacienteInexistenteLanzaNotFound() {
        assertThatExceptionOfType(NotFoundException.class)
                .isThrownBy(() -> servicio.obtenerPaciente("PAC-9999"))
                .withMessage("No se encontró el paciente PAC-9999.");
    }

    @Test
    void editarPacienteInexistenteLanzaNotFound() {
        assertThatExceptionOfType(NotFoundException.class)
                .isThrownBy(() -> servicio.editarPaciente("PAC-9999", "Ana Torres", 30, 101))
                .withMessage("No se encontró el paciente PAC-9999.");
    }

    @Test
    void eliminarPacienteInexistenteLanzaNotFound() {
        assertThatExceptionOfType(NotFoundException.class)
                .isThrownBy(() -> servicio.eliminarPaciente("PAC-9999"))
                .withMessage("No se encontró el paciente PAC-9999.");
    }

    @Test
    void eliminarPacienteExistenteHaceSoftDeleteYLoOcultaDelListado() {
        Paciente paciente = servicio.registrarPaciente("Ana Torres", 30, 101);

        servicio.eliminarPaciente(paciente.getIdPaciente());

        // Soft delete: el paciente sigue en memoria pero inactivo; nunca se reactiva.
        assertThat(repositorio.buscarPorId(paciente.getIdPaciente())).isSameAs(paciente);
        assertThat(paciente.isActivo()).isFalse();
        assertThat(servicio.listarPacientes(null)).isEmpty();
    }

    @Test
    void unPacienteDadoDeBajaSeComportaComoInexistente() {
        Paciente paciente = servicio.registrarPaciente("Ana Torres", 30, 101);
        servicio.eliminarPaciente(paciente.getIdPaciente());

        assertThatExceptionOfType(NotFoundException.class)
                .isThrownBy(() -> servicio.obtenerPaciente(paciente.getIdPaciente()))
                .withMessage("No se encontró el paciente " + paciente.getIdPaciente() + ".");
        assertThatExceptionOfType(NotFoundException.class)
                .isThrownBy(() -> servicio.editarPaciente(paciente.getIdPaciente(), "Ana Maria", 31, 205))
                .withMessage("No se encontró el paciente " + paciente.getIdPaciente() + ".");
        assertThatExceptionOfType(NotFoundException.class)
                .isThrownBy(() -> servicio.editarHabitacion(paciente.getIdPaciente(), 205))
                .withMessage("No se encontró el paciente " + paciente.getIdPaciente() + ".");
    }

    @Test
    void unPacienteDadoDeBajaNoSePuedeVolverAEliminarNiReactivar() {
        Paciente paciente = servicio.registrarPaciente("Ana Torres", 30, 101);
        servicio.eliminarPaciente(paciente.getIdPaciente());

        assertThatExceptionOfType(NotFoundException.class)
                .isThrownBy(() -> servicio.eliminarPaciente(paciente.getIdPaciente()))
                .withMessage("No se encontró el paciente " + paciente.getIdPaciente() + ".");
        assertThat(paciente.isActivo()).isFalse();
    }

    @Test
    void laBusquedaNoDevuelvePacientesDadosDeBaja() {
        Paciente activo = servicio.registrarPaciente("Ana Torres", 30, 101);
        Paciente dadoDeBaja = servicio.registrarPaciente("Bruno Diaz", 45, 202);
        servicio.eliminarPaciente(dadoDeBaja.getIdPaciente());

        assertThat(servicio.listarPacientes(null)).extracting(Paciente::getIdPaciente)
                .containsExactly(activo.getIdPaciente());
        assertThat(servicio.listarPacientes("bruno")).isEmpty();
    }

    @Test
    void listarPacientesFiltraPorNombreOIdSinImportarMayusculas() {
        servicio.registrarPaciente("Ana Torres", 30, 101);
        servicio.registrarPaciente("Bruno Diaz", 45, 202);

        assertThat(servicio.listarPacientes(null)).hasSize(2);
        assertThat(servicio.listarPacientes("BRUNO")).hasSize(1);
        assertThat(servicio.listarPacientes("  ana ")).first()
                .extracting(Paciente::getNombre).isEqualTo("Ana Torres");
        assertThat(servicio.listarPacientes("pac-0002")).hasSize(1);
        assertThat(servicio.listarPacientes("zzz")).isEmpty();
    }
}
