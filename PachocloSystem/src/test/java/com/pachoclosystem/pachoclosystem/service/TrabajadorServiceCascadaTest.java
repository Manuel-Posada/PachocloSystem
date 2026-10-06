package com.pachoclosystem.pachoclosystem.service;

import com.pachoclosystem.pachoclosystem.exception.NotFoundException;
import com.pachoclosystem.pachoclosystem.model.Rol;
import com.pachoclosystem.pachoclosystem.model.TrabajadorHospital;
import com.pachoclosystem.pachoclosystem.model.Usuario;
import com.pachoclosystem.pachoclosystem.repository.TrabajadorRepositoryImpl;
import com.pachoclosystem.pachoclosystem.repository.UsuarioRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Cascada de desactivación: eliminar un trabajador desactiva el usuario
 * vinculado (activo=false); si no tiene usuario, la eliminación no cambia de
 * comportamiento ni de mensajes de error.
 */
class TrabajadorServiceCascadaTest {

    private static final String PASSWORD_DE_PRUEBA = "password-de-prueba-12345";

    private TrabajadorRepositoryImpl repositorioTrabajadores;
    private UsuarioRepositoryImpl repositorioUsuarios;
    private TrabajadorService servicioTrabajadores;
    private UsuarioService servicioUsuarios;

    @BeforeEach
    void preparar() {
        repositorioTrabajadores = new TrabajadorRepositoryImpl();
        repositorioUsuarios = new UsuarioRepositoryImpl();
        servicioTrabajadores = new TrabajadorService(repositorioTrabajadores);
        servicioTrabajadores.setUsuarioRepository(repositorioUsuarios);
        servicioUsuarios = new UsuarioService(repositorioUsuarios, servicioTrabajadores,
                new BCryptPasswordEncoder());
    }

    @Test
    void alEliminarUnTrabajadorSuUsuarioVinculadoQuedaInactivo() {
        TrabajadorHospital doctor = registroDeTrabajador();

        String idUsuario = servicioUsuarios.crearUsuario("cascada.dr", PASSWORD_DE_PRUEBA,
                Rol.DOCTOR, doctor.getIdTrabajador()).getIdUsuario();

        servicioTrabajadores.eliminarTrabajador(doctor.getIdTrabajador());

        Usuario usuario = repositorioUsuarios.buscarPorId(idUsuario);
        assertThat(usuario).isNotNull();
        assertThat(usuario.isActivo()).isFalse();
        assertThat(repositorioTrabajadores.buscarPorId(doctor.getIdTrabajador())).isNull();
    }

    @Test
    void alEliminarUnTrabajadorSinUsuarioNoPasaNadaYNoFalla() {
        TrabajadorHospital doctor = registroDeTrabajador();

        servicioTrabajadores.eliminarTrabajador(doctor.getIdTrabajador());

        assertThat(repositorioUsuarios.listarTodos()).isEmpty();
        assertThat(repositorioTrabajadores.buscarPorId(doctor.getIdTrabajador())).isNull();
    }

    @Test
    void eliminarDosVecesElSegundoIntentoSigueDevolviendo404() {
        TrabajadorHospital doctor = registroDeTrabajador();
        servicioUsuarios.crearUsuario("cascada.dr2", PASSWORD_DE_PRUEBA, Rol.DOCTOR,
                doctor.getIdTrabajador());

        servicioTrabajadores.eliminarTrabajador(doctor.getIdTrabajador());

        assertThatExceptionOfType(NotFoundException.class)
                .isThrownBy(() -> servicioTrabajadores.eliminarTrabajador(doctor.getIdTrabajador()))
                .withMessage("No se encontró el trabajador " + doctor.getIdTrabajador() + ".");
    }

    @Test
    void laCascadaSoloDesactivaAlUsuarioVinculadoAlTrabajadorEliminado() {
        TrabajadorHospital doctorEliminado = registroDeTrabajador();
        TrabajadorHospital doctorConservado = registroDeTrabajador();
        servicioUsuarios.crearUsuario("cascada.eliminado", PASSWORD_DE_PRUEBA, Rol.DOCTOR,
                doctorEliminado.getIdTrabajador());
        Usuario conservado = servicioUsuarios.crearUsuario("cascada.conservado", PASSWORD_DE_PRUEBA,
                Rol.DOCTOR, doctorConservado.getIdTrabajador());

        servicioTrabajadores.eliminarTrabajador(doctorEliminado.getIdTrabajador());

        assertThat(repositorioUsuarios.buscarPorUsername("cascada.eliminado").isActivo()).isFalse();
        assertThat(repositorioUsuarios.buscarPorUsername("cascada.conservado")).isSameAs(conservado);
        assertThat(conservado.isActivo()).isTrue();
    }

    @Test
    void eliminarUnTrabajadorConUsuarioYaInactivoSigueSiendoIdempotente() {
        TrabajadorHospital doctor = registroDeTrabajador();
        Usuario usuario = servicioUsuarios.crearUsuario("cascada.ya.inactivo", PASSWORD_DE_PRUEBA,
                Rol.DOCTOR, doctor.getIdTrabajador());
        usuario.desactivar();

        servicioTrabajadores.eliminarTrabajador(doctor.getIdTrabajador());

        assertThat(usuario.isActivo()).isFalse();
    }

    private TrabajadorHospital registroDeTrabajador() {
        return servicioTrabajadores.registrarTrabajador("Carlos Mena", "Doctor", "Cardiologia", null);
    }
}