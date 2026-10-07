package com.pachoclosystem.pachoclosystem;

import org.springframework.jdbc.core.simple.JdbcClient;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Vacía la base de datos de los tests. Antes comprueba que es una base de
 * pruebas (su nombre termina en {@code _test}): si por error apuntara a la de
 * desarrollo o a otra con datos reales, se detiene sin tocar nada.
 */
public final class BaseDeDatosDePruebas {

    private BaseDeDatosDePruebas() {
    }

    /** Falla si la base de {@code dataSource} no es una base de pruebas. */
    public static void comprobarQueEsDePruebas(DataSource dataSource) {
        try (Connection conexion = dataSource.getConnection();
             Statement sentencia = conexion.createStatement();
             ResultSet fila = sentencia.executeQuery("SELECT current_database()")) {
            fila.next();
            comprobarNombre(fila.getString(1));
        } catch (SQLException fallo) {
            throw new IllegalStateException("No se pudo comprobar la base de datos de los tests.", fallo);
        }
    }

    /**
     * Borra pacientes, trabajadores, registros, claves de idempotencia y todos los
     * usuarios salvo {@code usernameAdmin} (que vuelve a estar activo y con rol
     * ADMIN; su contraseña no se toca), y reinicia los ids de pacientes y
     * trabajadores (PAC-0001, DOC-0001, ENF-0001). Los ids de usuario no se
     * reinician: el administrador conserva el suyo.
     */
    public static void vaciar(JdbcClient jdbc, String usernameAdmin) {
        comprobarNombre(jdbc.sql("SELECT current_database()").query(String.class).single());
        jdbc.sql("TRUNCATE registros_clinicos, historial_idempotencia, pacientes, trabajadores").update();
        jdbc.sql("DELETE FROM usuarios WHERE username <> :admin").param("admin", usernameAdmin).update();
        // Si una prueba lo desactivó o le cambió el rol, el administrador vuelve a su estado.
        jdbc.sql("UPDATE usuarios SET activo = TRUE, rol = 'ADMIN', id_trabajador = NULL WHERE username = :admin")
                .param("admin", usernameAdmin).update();
        for (String secuencia : new String[]{"pacientes_id_seq", "doctores_id_seq", "enfermeros_id_seq"}) {
            jdbc.sql("ALTER SEQUENCE " + secuencia + " RESTART WITH 1").update();
        }
    }

    private static void comprobarNombre(String base) {
        if (base == null || !base.endsWith("_test")) {
            throw new IllegalStateException("Los tests solo vacían bases cuyo nombre termina en \"_test\"; "
                    + "esta es \"" + base + "\". Revise PACHOCLOSYSTEM_TEST_DB_URL.");
        }
    }
}
