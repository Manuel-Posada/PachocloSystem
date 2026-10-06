package com.pachoclosystem.pachoclosystem.repository;

import com.pachoclosystem.pachoclosystem.model.Usuario;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Repositorio de usuarios en memoria basado en {@link ConcurrentHashMap}.
 *
 * <p>Ningún método está {@code synchronized}: la unicidad de username y de
 * trabajador vinculado se resuelve con {@code putIfAbsent} sobre índices
 * auxiliares, operación atómica en las colecciones concurrentes. No se usa el
 * patrón leer-comprobar-escribir.</p>
 */
@Repository
public class UsuarioRepositoryImpl implements IUsuarioRepository {

    private static final String PREFIJO_ID = "USR";

    /** ID de usuario -> usuario. */
    private final Map<String, Usuario> usuarios = new ConcurrentHashMap<>();
    /** Username normalizado (minúsculas) -> ID de usuario. */
    private final Map<String, String> indicePorUsername = new ConcurrentHashMap<>();
    /** ID de trabajador vinculado -> ID de usuario. */
    private final Map<String, String> indicePorIdTrabajador = new ConcurrentHashMap<>();

    private final AtomicInteger contadorId = new AtomicInteger(1);

    @Override
    public String generarNuevoId() {
        return String.format("%s-%04d", PREFIJO_ID, contadorId.getAndIncrement());
    }

    @Override
    public boolean guardar(Usuario usuario) {
        if (usuario == null) {
            return false;
        }
        String id = usuario.getIdUsuario();
        String username = usuario.getUsername();

        // Unicidad de username: putIfAbsent es atómico, gana un solo hilo.
        String idQueYaTieneElUsername = indicePorUsername.putIfAbsent(username, id);
        boolean usernameRecienReservado = idQueYaTieneElUsername == null;
        if (!usernameRecienReservado && !idQueYaTieneElUsername.equals(id)) {
            return false;
        }

        // Unicidad del trabajador vinculado (si lo hay).
        if (usuario.getIdTrabajador() != null) {
            String idQueYaVinculaAlTrabajador =
                    indicePorIdTrabajador.putIfAbsent(usuario.getIdTrabajador(), id);
            if (idQueYaVinculaAlTrabajador != null && !idQueYaVinculaAlTrabajador.equals(id)) {
                // Se deshace solo la reserva recién hecha; si el username ya estaba
                // reservado por este mismo usuario, se conserva.
                if (usernameRecienReservado) {
                    indicePorUsername.remove(username, id);
                }
                return false;
            }
        }

        // username e idTrabajador son inmutables en Usuario: si el ID ya existía,
        // los apuntadores apuntan al mismo ID, por lo que no queda ningún índice obsoleto.
        usuarios.put(id, usuario);
        return true;
    }

    @Override
    public Usuario buscarPorId(String id) {
        return id == null ? null : usuarios.get(id);
    }

    @Override
    public Usuario buscarPorUsername(String username) {
        String normalizado = Usuario.normalizarUsername(username);
        if (normalizado == null) {
            return null;
        }
        String id = indicePorUsername.get(normalizado);
        return id == null ? null : usuarios.get(id);
    }

    @Override
    public Usuario buscarPorIdTrabajador(String idTrabajador) {
        if (idTrabajador == null) {
            return null;
        }
        String id = indicePorIdTrabajador.get(idTrabajador);
        return id == null ? null : usuarios.get(id);
    }

    @Override
    public boolean existePorUsername(String username) {
        String normalizado = Usuario.normalizarUsername(username);
        return normalizado != null && indicePorUsername.containsKey(normalizado);
    }

    @Override
    public List<Usuario> listarTodos() {
        List<Usuario> listados = new ArrayList<>(usuarios.values());
        listados.sort((a, b) -> a.getIdUsuario().compareTo(b.getIdUsuario()));
        return listados;
    }
}
