package com.pachoclosystem.pachoclosystem.repository;

import com.pachoclosystem.pachoclosystem.model.Rol;
import com.pachoclosystem.pachoclosystem.model.Usuario;

import java.util.List;

/**
 * Usuarios de acceso. La implementación de la aplicación es
 * {@link UsuarioRepositoryJdbc} (PostgreSQL).
 *
 * <p>Cada lectura devuelve un objeto nuevo con el estado guardado: los cambios
 * (estado, contraseña, rol) se guardan con su operación propia, nunca
 * modificando el objeto leído.</p>
 */
public interface IUsuarioRepository {

    /** Genera el siguiente ID con formato USR-0001 (mismo estilo que PAC-/DOC-/ENF-). */
    String generarNuevoId();

    /**
     * Inserta el usuario si su username (case-insensitive) y su trabajador
     * vinculado no están ya ocupados por otro usuario.
     *
     * @return {@code false} si el username o el trabajador ya pertenecen a otro usuario
     */
    boolean guardar(Usuario usuario);

    /**
     * Cambia el rol y el trabajador vinculado de un usuario de forma atómica.
     * No verifica que el trabajador exista ni su tipo: eso es responsabilidad
     * del servicio. Sí garantiza que un trabajador no quede vinculado a dos
     * usuarios.
     *
     * @return {@code false} si el usuario no existe o el trabajador destino ya
     *         está vinculado a otro usuario; {@code true} si el cambio se aplicó
     */
    boolean cambiarRol(String idUsuario, Rol nuevoRol, String idTrabajador);

    /**
     * Desactiva el usuario y sube su versión de token (revoca sus tokens).
     * Idempotente: si ya estaba inactivo no cambia nada.
     *
     * @return {@code true} si estaba activo y se desactivó
     */
    boolean desactivar(String idUsuario);

    /** Reactiva el usuario sin tocar su versión de token (los tokens anteriores siguen revocados). */
    void reactivar(String idUsuario);

    /** Sustituye el hash, sube la versión de token y fija si debe cambiar la contraseña. */
    void cambiarPassword(String idUsuario, String nuevoHash, boolean exigirCambio);

    Usuario buscarPorId(String id);

    /**
     * Como {@link #buscarPorId(String)}, pero bloquea al usuario hasta el final de
     * la transacción en curso: otra petición que quiera modificarlo espera.
     */
    Usuario buscarPorIdParaActualizar(String id);

    /** Búsqueda por username sin distinguir mayúsculas. */
    Usuario buscarPorUsername(String username);

    /** Devuelve el usuario vinculado a ese trabajador, o {@code null}. */
    Usuario buscarPorIdTrabajador(String idTrabajador);

    boolean existePorUsername(String username);

    /** Número de usuarios ADMIN activos. */
    long contarAdministradoresActivos();

    /**
     * Serializa, hasta el final de la transacción en curso, las operaciones que
     * pueden dejar el sistema sin administradores activos (desactivar, activar
     * y cambiar de rol), aunque haya varias instancias del servicio.
     */
    void bloquearAdministracion();

    /** Todos los usuarios, ordenados por ID. */
    List<Usuario> listarTodos();
}
