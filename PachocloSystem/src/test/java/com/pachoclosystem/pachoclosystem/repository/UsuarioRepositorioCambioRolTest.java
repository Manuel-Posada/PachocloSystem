package com.pachoclosystem.pachoclosystem.repository;

import com.pachoclosystem.pachoclosystem.model.Rol;
import com.pachoclosystem.pachoclosystem.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cambio de rol atómico en el repositorio en memoria: el índice por trabajador
 * queda siempre coherente con el vínculo de cada usuario, incluso bajo
 * concurrencia (nunca dos usuarios comparten trabajador ni el índice apunta a un
 * trabajador que el usuario ya no tiene).
 */
class UsuarioRepositorioCambioRolTest {

    private static final String HASH_DE_MENTIRA = "$2a$10$de.mentira.para.el.repositorio";

    private UsuarioRepositoryEnMemoria repositorio;

    @BeforeEach
    void preparar() {
        repositorio = new UsuarioRepositoryEnMemoria();
    }

    @Test
    void cambiarRolActualizaRolVinculoEIndice() {
        repositorio.guardar(doctor("USR-0001", "carlos.mena", "DOC-0001"));

        boolean cambiado = repositorio.cambiarRol("USR-0001", Rol.ENFERMERO, "ENF-0002");

        assertThat(cambiado).isTrue();
        Usuario usuario = repositorio.buscarPorId("USR-0001");
        assertThat(usuario.getRol()).isEqualTo(Rol.ENFERMERO);
        assertThat(usuario.getIdTrabajador()).isEqualTo("ENF-0002");
        assertThat(repositorio.buscarPorIdTrabajador("DOC-0001")).isNull();
        assertThat(repositorio.buscarPorIdTrabajador("ENF-0002")).isSameAs(usuario);
    }

    @Test
    void cambiarRolAAdminLiberaElTrabajadorAnterior() {
        repositorio.guardar(doctor("USR-0001", "carlos.mena", "DOC-0001"));

        assertThat(repositorio.cambiarRol("USR-0001", Rol.ADMIN, null)).isTrue();

        Usuario usuario = repositorio.buscarPorId("USR-0001");
        assertThat(usuario.getRol()).isEqualTo(Rol.ADMIN);
        assertThat(usuario.getIdTrabajador()).isNull();
        assertThat(repositorio.buscarPorIdTrabajador("DOC-0001")).isNull();
    }

    @Test
    void cambiarRolATrabajadorOcupadoFallaYNoModificaNada() {
        repositorio.guardar(doctor("USR-0001", "carlos.mena", "DOC-0001"));
        repositorio.guardar(doctor("USR-0002", "lucia.vidal", "DOC-0002"));

        assertThat(repositorio.cambiarRol("USR-0001", Rol.DOCTOR, "DOC-0002")).isFalse();

        Usuario usuario = repositorio.buscarPorId("USR-0001");
        assertThat(usuario.getIdTrabajador()).isEqualTo("DOC-0001");
        assertThat(repositorio.buscarPorIdTrabajador("DOC-0001")).isSameAs(usuario);
        assertThat(repositorio.buscarPorIdTrabajador("DOC-0002").getIdUsuario()).isEqualTo("USR-0002");
    }

    @Test
    void cambiarRolDeUsuarioInexistenteDevuelveFalse() {
        assertThat(repositorio.cambiarRol("USR-9999", Rol.ADMIN, null)).isFalse();
        assertThat(repositorio.cambiarRol(null, Rol.ADMIN, null)).isFalse();
    }

    @Test
    void concurrenciaElIndicePorTrabajadorNuncaQuedaIncoherente() throws Exception {
        int usuarios = 12;
        int trabajadores = 12;
        List<String> idsUsuarios = new ArrayList<>();
        for (int i = 1; i <= usuarios; i++) {
            String id = String.format("USR-%04d", i);
            idsUsuarios.add(id);
            // La mitad arrancan como ADMIN (sin trabajador), la otra como DOCTOR.
            if (i % 2 == 0) {
                repositorio.guardar(doctor(id, "usuario." + i, String.format("DOC-%04d", i)));
            } else {
                repositorio.guardar(new Usuario(id, "usuario." + i, HASH_DE_MENTIRA, Rol.ADMIN, null));
            }
        }

        int hilos = 8;
        int operacionesPorHilo = 300;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch esperarEnLaPuerta = new CountDownLatch(1);
        List<Future<?>> resultados = new ArrayList<>();
        try {
            for (int h = 0; h < hilos; h++) {
                resultados.add(pool.submit(() -> {
                    esperarEnLaPuerta.await();
                    ThreadLocalRandom aleatorio = ThreadLocalRandom.current();
                    for (int j = 0; j < operacionesPorHilo; j++) {
                        String idUsuario = idsUsuarios.get(aleatorio.nextInt(idsUsuarios.size()));
                        int destino = aleatorio.nextInt(trabajadores + 1); // 0 => ADMIN sin trabajador
                        if (destino == 0) {
                            repositorio.cambiarRol(idUsuario, Rol.ADMIN, null);
                        } else {
                            String trabajador = String.format("DOC-%04d", destino);
                            repositorio.cambiarRol(idUsuario, Rol.DOCTOR, trabajador);
                        }
                    }
                    return null;
                }));
            }
            esperarEnLaPuerta.countDown();
            for (Future<?> resultado : resultados) {
                resultado.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        // Invariante 1: el índice de cada usuario apunta a su trabajador actual.
        for (String idUsuario : idsUsuarios) {
            Usuario usuario = repositorio.buscarPorId(idUsuario);
            if (usuario.getIdTrabajador() != null) {
                assertThat(repositorio.buscarPorIdTrabajador(usuario.getIdTrabajador()))
                        .as("el índice del trabajador %s debe apuntar al usuario %s",
                                usuario.getIdTrabajador(), idUsuario)
                        .isSameAs(usuario);
            }
        }

        // Invariante 2: ningún trabajador queda vinculado a dos usuarios.
        for (int i = 1; i <= trabajadores; i++) {
            String trabajador = String.format("DOC-%04d", i);
            Usuario dueno = repositorio.buscarPorIdTrabajador(trabajador);
            if (dueno != null) {
                assertThat(dueno.getIdTrabajador())
                        .as("el índice de %s no puede apuntar a un usuario que ya no lo tiene", trabajador)
                        .isEqualTo(trabajador);
            }
        }
        assertThat(repositorio.listarTodos()).hasSize(usuarios);
    }

    private static Usuario doctor(String id, String username, String idTrabajador) {
        return new Usuario(id, username, HASH_DE_MENTIRA, Rol.DOCTOR, idTrabajador);
    }
}
