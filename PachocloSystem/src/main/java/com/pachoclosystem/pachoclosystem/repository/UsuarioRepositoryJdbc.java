package com.pachoclosystem.pachoclosystem.repository;

import com.pachoclosystem.pachoclosystem.model.Rol;
import com.pachoclosystem.pachoclosystem.model.Usuario;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Usuarios en PostgreSQL (tabla {@code usuarios}, ver {@code db/migration}).
 *
 * <ul>
 *   <li>La unicidad de username y de trabajador vinculado la deciden las
 *       restricciones UNIQUE de la tabla: dos altas simultáneas nunca dejan dos
 *       usuarios con el mismo username o el mismo trabajador.</li>
 *   <li>Hash, versión de token y cambio pendiente están en la misma fila: una
 *       sola lectura los devuelve juntos (lo que antes hacía el
 *       {@link Usuario.Credenciales} inmutable).</li>
 *   <li>Los ids salen de la secuencia {@code usuarios_id_seq} (USR-0001).</li>
 * </ul>
 */
@Repository
public class UsuarioRepositoryJdbc implements IUsuarioRepository {

    /** Clave fija del advisory lock con el que se serializa la administración de usuarios. */
    private static final long CLAVE_BLOQUEO_ADMINISTRACION = 0x5041434855_53524CL;

    private static final String COLUMNAS = "id_usuario, username, password_hash, version_token, "
            + "debe_cambiar_password, rol, id_trabajador, activo";

    private static final RowMapper<Usuario> MAPEO = (fila, numero) -> Usuario.restaurar(
            fila.getString("id_usuario"),
            fila.getString("username"),
            new Usuario.Credenciales(fila.getString("password_hash"), fila.getLong("version_token"),
                    fila.getBoolean("debe_cambiar_password")),
            Rol.valueOf(fila.getString("rol")),
            fila.getString("id_trabajador"),
            fila.getBoolean("activo"));

    private final JdbcClient jdbc;

    public UsuarioRepositoryJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public String generarNuevoId() {
        long numero = jdbc.sql("SELECT nextval('usuarios_id_seq')").query(Long.class).single();
        return String.format("USR-%04d", numero);
    }

    @Override
    public boolean guardar(Usuario usuario) {
        if (usuario == null || usuario.getIdUsuario() == null || usuario.getUsername() == null) {
            return false;
        }
        Usuario.Credenciales credenciales = usuario.getCredenciales();
        try {
            jdbc.sql("INSERT INTO usuarios (" + COLUMNAS + ") VALUES (:id, :username, :hash, :version, "
                            + ":debeCambiar, :rol, :idTrabajador, :activo)")
                    .param("id", usuario.getIdUsuario())
                    .param("username", usuario.getUsername())
                    .param("hash", credenciales.hash())
                    .param("version", credenciales.version())
                    .param("debeCambiar", credenciales.debeCambiarPassword())
                    .param("rol", usuario.getRol().name())
                    .param("idTrabajador", usuario.getIdTrabajador())
                    .param("activo", usuario.isActivo())
                    .update();
            return true;
        } catch (DuplicateKeyException ocupado) {
            return false;
        }
    }

    @Override
    public boolean cambiarRol(String idUsuario, Rol nuevoRol, String idTrabajador) {
        if (idUsuario == null || nuevoRol == null) {
            return false;
        }
        try {
            return jdbc.sql("UPDATE usuarios SET rol = :rol, id_trabajador = :idTrabajador WHERE id_usuario = :id")
                    .param("rol", nuevoRol.name())
                    .param("idTrabajador", idTrabajador)
                    .param("id", idUsuario)
                    .update() > 0;
        } catch (DuplicateKeyException trabajadorOcupado) {
            return false;
        }
    }

    @Override
    public boolean desactivar(String idUsuario) {
        return jdbc.sql("UPDATE usuarios SET activo = FALSE, version_token = version_token + 1 "
                        + "WHERE id_usuario = :id AND activo")
                .param("id", idUsuario)
                .update() > 0;
    }

    @Override
    public void reactivar(String idUsuario) {
        jdbc.sql("UPDATE usuarios SET activo = TRUE WHERE id_usuario = :id")
                .param("id", idUsuario)
                .update();
    }

    @Override
    public void cambiarPassword(String idUsuario, String nuevoHash, boolean exigirCambio) {
        if (nuevoHash == null || nuevoHash.isBlank()) {
            throw new IllegalArgumentException("El hash de la contraseña es obligatorio.");
        }
        jdbc.sql("UPDATE usuarios SET password_hash = :hash, version_token = version_token + 1, "
                        + "debe_cambiar_password = :exigirCambio WHERE id_usuario = :id")
                .param("hash", nuevoHash)
                .param("exigirCambio", exigirCambio)
                .param("id", idUsuario)
                .update();
    }

    @Override
    public Usuario buscarPorId(String id) {
        if (id == null) {
            return null;
        }
        return jdbc.sql("SELECT " + COLUMNAS + " FROM usuarios WHERE id_usuario = :id")
                .param("id", id).query(MAPEO).optional().orElse(null);
    }

    /** Fuera de una transacción el bloqueo no serviría de nada: se exige una. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Usuario buscarPorIdParaActualizar(String id) {
        if (id == null) {
            return null;
        }
        return jdbc.sql("SELECT " + COLUMNAS + " FROM usuarios WHERE id_usuario = :id FOR UPDATE")
                .param("id", id).query(MAPEO).optional().orElse(null);
    }

    @Override
    public Usuario buscarPorUsername(String username) {
        String normalizado = Usuario.normalizarUsername(username);
        if (normalizado == null) {
            return null;
        }
        return jdbc.sql("SELECT " + COLUMNAS + " FROM usuarios WHERE username = :username")
                .param("username", normalizado).query(MAPEO).optional().orElse(null);
    }

    @Override
    public Usuario buscarPorIdTrabajador(String idTrabajador) {
        if (idTrabajador == null) {
            return null;
        }
        return jdbc.sql("SELECT " + COLUMNAS + " FROM usuarios WHERE id_trabajador = :idTrabajador")
                .param("idTrabajador", idTrabajador).query(MAPEO).optional().orElse(null);
    }

    @Override
    public boolean existePorUsername(String username) {
        return buscarPorUsername(username) != null;
    }

    @Override
    public long contarAdministradoresActivos() {
        return jdbc.sql("SELECT count(*) FROM usuarios WHERE rol = 'ADMIN' AND activo")
                .query(Long.class).single();
    }

    /** Advisory lock de transacción: se suelta solo al terminar la transacción en curso. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void bloquearAdministracion() {
        jdbc.sql("SELECT pg_advisory_xact_lock(:clave)")
                .param("clave", CLAVE_BLOQUEO_ADMINISTRACION)
                .query((fila, numero) -> Boolean.TRUE)
                .single();
    }

    /** Ordenados por id como {@code String.compareTo} (los ids son ASCII). */
    @Override
    public List<Usuario> listarTodos() {
        return jdbc.sql("SELECT " + COLUMNAS + " FROM usuarios ORDER BY id_usuario COLLATE \"C\"")
                .query(MAPEO).list();
    }
}
