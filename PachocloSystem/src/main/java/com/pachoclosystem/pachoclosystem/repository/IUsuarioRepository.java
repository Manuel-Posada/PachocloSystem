package com.pachoclosystem.pachoclosystem.repository;

import com.pachoclosystem.pachoclosystem.model.Usuario;

import java.util.List;

/**
 * Repositorio de usuarios en memoria. La unicidad de username se garantiza de
 * forma atómica dentro de {@link #guardar(Usuario)} (no es responsabilidad del
 * cliente leer-comprobar-escribir).
 */
public interface IUsuarioRepository {

    /** Genera el siguiente ID con formato USR-0001 (mismo estilo que PAC-/DOC-/ENF-). */
    String generarNuevoId();

    /**
     * Guarda el usuario si su username (case-insensitive) y su trabajador
     * vinculado no están ya ocupados por otro usuario.
     *
     * @return {@code false} si el username o el trabajador ya pertenecen a otro usuario
     */
    boolean guardar(Usuario usuario);

    Usuario buscarPorId(String id);

    /** Búsqueda por username sin distinguir mayúsculas. */
    Usuario buscarPorUsername(String username);

    /** Devuelve el usuario vinculado a ese trabajador, o {@code null}. */
    Usuario buscarPorIdTrabajador(String idTrabajador);

    boolean existePorUsername(String username);

    /** Todos los usuarios, ordenados por ID. */
    List<Usuario> listarTodos();
}
