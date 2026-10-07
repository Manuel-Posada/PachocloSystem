package com.pachoclosystem.pachoclosystem.repository;

import com.pachoclosystem.pachoclosystem.model.Rol;
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

    /**
     * Cambia el rol y el trabajador vinculado de un usuario de forma atómica,
     * actualizando a la vez el índice por trabajador: no hay ventana en la que
     * dos usuarios compartan un mismo trabajador ni en la que el índice siga
     * apuntando al trabajador anterior.
     *
     * <p>No verifica que el trabajador exista ni su tipo: eso es responsabilidad
     * del servicio. Sí garantiza que un trabajador no quede vinculado a dos
     * usuarios.</p>
     *
     * @return {@code false} si el usuario no existe o el trabajador destino ya
     *         está vinculado a otro usuario; {@code true} si el cambio se aplicó
     */
    boolean cambiarRol(String idUsuario, Rol nuevoRol, String idTrabajador);

    Usuario buscarPorId(String id);

    /** Búsqueda por username sin distinguir mayúsculas. */
    Usuario buscarPorUsername(String username);

    /** Devuelve el usuario vinculado a ese trabajador, o {@code null}. */
    Usuario buscarPorIdTrabajador(String idTrabajador);

    boolean existePorUsername(String username);

    /** Todos los usuarios, ordenados por ID. */
    List<Usuario> listarTodos();
}
