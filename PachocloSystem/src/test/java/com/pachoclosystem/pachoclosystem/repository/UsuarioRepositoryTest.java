package com.pachoclosystem.pachoclosystem.repository;

import com.pachoclosystem.pachoclosystem.model.Rol;
import com.pachoclosystem.pachoclosystem.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** Comportamiento del repositorio de usuarios en memoria, sin contexto Spring. */
class UsuarioRepositoryTest {

    /** Hash de mentira: el repositorio no mira la contraseña. */
    private static final String HASH_DE_MENTIRA = "$2a$10$de.mentira.para.el.repositorio";

    private UsuarioRepositoryEnMemoria repositorio;

    @BeforeEach
    void preparar() {
        repositorio = new UsuarioRepositoryEnMemoria();
    }

    @Test
    void generarNuevoIdSigueElFormatoUsrCuatroDigitos() {
        assertThat(repositorio.generarNuevoId()).isEqualTo("USR-0001");
        assertThat(repositorio.generarNuevoId()).isEqualTo("USR-0002");
        assertThat(repositorio.generarNuevoId()).isEqualTo("USR-0003");
    }

    @Test
    void guardarYBuscarPorId() {
        Usuario usuario = usuario("USR-0001", "ana.torres", null);

        assertThat(repositorio.guardar(usuario)).isTrue();
        assertThat(repositorio.buscarPorId("USR-0001")).isSameAs(usuario);
        assertThat(repositorio.buscarPorId("USR-9999")).isNull();
        assertThat(repositorio.buscarPorId(null)).isNull();
        assertThat(repositorio.guardar(null)).isFalse();
    }

    @Test
    void buscarPorUsernameNoDistingueMayusculas() {
        Usuario usuario = usuario("USR-0001", "ana.torres", null);
        repositorio.guardar(usuario);

        assertThat(repositorio.buscarPorUsername("ana.torres")).isSameAs(usuario);
        assertThat(repositorio.buscarPorUsername("ANA.TORRES")).isSameAs(usuario);
        assertThat(repositorio.buscarPorUsername("  Ana.Torres ")).isSameAs(usuario);
        assertThat(repositorio.buscarPorUsername("otro")).isNull();
        assertThat(repositorio.buscarPorUsername(null)).isNull();
        assertThat(repositorio.existePorUsername("AnA.ToRrEs")).isTrue();
        assertThat(repositorio.existePorUsername("otro")).isFalse();
    }

    @Test
    void usernameRepetidoConDistintaMayusculaNoSeGuarda() {
        repositorio.guardar(usuario("USR-0001", "ana.torres", null));

        assertThat(repositorio.guardar(usuario("USR-0002", "ANA.TORRES", null))).isFalse();
        assertThat(repositorio.listarTodos()).hasSize(1);
        assertThat(repositorio.buscarPorId("USR-0002")).isNull();
        assertThat(repositorio.buscarPorUsername("ana.torres").getIdUsuario()).isEqualTo("USR-0001");
    }

    @Test
    void buscarPorIdTrabajadorYUnicidadDelVinculo() {
        Usuario usuario = usuario("USR-0001", "carlos.mena", "DOC-0001");
        repositorio.guardar(usuario);

        assertThat(repositorio.buscarPorIdTrabajador("DOC-0001")).isSameAs(usuario);
        assertThat(repositorio.buscarPorIdTrabajador("DOC-9999")).isNull();
        assertThat(repositorio.buscarPorIdTrabajador(null)).isNull();

        // Un mismo trabajador no admite dos usuarios, aunque el username sea distinto.
        assertThat(repositorio.guardar(usuario("USR-0002", "otro.username", "DOC-0001"))).isFalse();
        assertThat(repositorio.listarTodos()).hasSize(1);

        // Un trabajador distinto con el mismo username tampoco pasa.
        assertThat(repositorio.guardar(usuario("USR-0002", "CARLOS.MENA", "DOC-0002"))).isFalse();
        assertThat(repositorio.buscarPorId("USR-0002")).isNull();
    }

    @Test
    void listarTodosDevuelveTodosOrdenadosPorId() {
        Usuario tercero = usuario("USR-0003", "tercero.user", null);
        Usuario primero = usuario("USR-0001", "primero.user", null);
        Usuario segundo = usuario("USR-0002", "segundo.user", null);
        repositorio.guardar(tercero);
        repositorio.guardar(primero);
        repositorio.guardar(segundo);

        List<Usuario> listados = repositorio.listarTodos();

        assertThat(listados).containsExactly(primero, segundo, tercero);
    }

    @Test
    void reservarElTrabajadorFallidoNoDejaIndiceDeUsernameSucio() throws Exception {
        repositorio.guardar(usuario("USR-0001", "titular.doc", "DOC-0001"));

        // Este intento reserva el username y luego falla por el trabajador.
        assertThat(repositorio.guardar(usuario("USR-0002", "suplente.doc", "DOC-0001"))).isFalse();

        // El índice de username de USR-0002 quedó libre: se puede reutilizar.
        assertThat(repositorio.guardar(usuario("USR-0002", "suplente.doc", "DOC-0002"))).isTrue();
        assertThat(repositorio.buscarPorUsername("suplente.doc").getIdUsuario()).isEqualTo("USR-0002");
        assertThat(repositorio.buscarPorIdTrabajador("DOC-0001").getIdUsuario()).isEqualTo("USR-0001");
    }

    @Test
    void concurrenciaExactamenteUnHiloCreaElMismoUsername() throws Exception {
        int numeroDeHilos = 16;
        ExecutorService pool = Executors.newFixedThreadPool(numeroDeHilos);
        CountDownLatch esperarEnLaPuerta = new CountDownLatch(1);
        List<Future<Boolean>> resultados = new java.util.ArrayList<>();

        try {
            for (int i = 0; i < numeroDeHilos; i++) {
                resultados.add(pool.submit(() -> {
                    esperarEnLaPuerta.await();
                    return repositorio.guardar(
                            usuario(repositorio.generarNuevoId(), "usuario.contendio", null));
                }));
            }
            esperarEnLaPuerta.countDown();

            int exitos = 0;
            for (Future<Boolean> resultado : resultados) {
                if (resultado.get(30, TimeUnit.SECONDS)) {
                    exitos++;
                }
            }

            assertThat(exitos)
                    .as("solo un hilo puede ganar la reserva del username")
                    .isEqualTo(1);
            assertThat(repositorio.listarTodos()).hasSize(1);
            assertThat(repositorio.buscarPorUsername("USUARIO.CONTENDIO")).isNotNull();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void concurrenciaIdentificadoresUnicosEnParalelo() throws Exception {
        int numeroDeHilos = 8;
        int intentosPorHilo = 50;
        ExecutorService pool = Executors.newFixedThreadPool(numeroDeHilos);
        CountDownLatch esperarEnLaPuerta = new CountDownLatch(1);
        List<Future<List<String>>> resultados = new java.util.ArrayList<>();

        try {
            for (int i = 0; i < numeroDeHilos; i++) {
                resultados.add(pool.submit(() -> {
                    esperarEnLaPuerta.await();
                    List<String> ids = new java.util.ArrayList<>();
                    for (int j = 0; j < intentosPorHilo; j++) {
                        ids.add(repositorio.generarNuevoId());
                    }
                    return ids;
                }));
            }
            esperarEnLaPuerta.countDown();

            List<String> todosLosIds = new java.util.ArrayList<>();
            for (Future<List<String>> resultado : resultados) {
                todosLosIds.addAll(resultado.get(30, TimeUnit.SECONDS));
            }

            assertThat(todosLosIds).hasSize(numeroDeHilos * intentosPorHilo);
            assertThat(todosLosIds.stream().distinct())
                    .as("ningún ID debe repetirse entre hilos")
                    .hasSize(numeroDeHilos * intentosPorHilo);
        } finally {
            pool.shutdownNow();
        }
    }

    private Usuario usuario(String id, String username, String idTrabajador) {
        return new Usuario(id, username, HASH_DE_MENTIRA, Rol.DOCTOR, idTrabajador);
    }
}
