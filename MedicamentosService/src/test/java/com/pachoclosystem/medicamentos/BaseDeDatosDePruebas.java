package com.pachoclosystem.medicamentos;

import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Vacía la base de datos de los tests. Antes comprueba que es una base de
 * pruebas (su nombre termina en {@code _test}): si por error apuntara a la de
 * desarrollo o a otra con datos reales, se detiene sin tocar nada.
 */
public final class BaseDeDatosDePruebas {

    private BaseDeDatosDePruebas() {
    }

    /** Borra todos los medicamentos y claves de idempotencia y reinicia los ids en MED-0001. */
    public static void vaciar(JdbcClient jdbc) {
        String base = jdbc.sql("SELECT current_database()").query(String.class).single();
        if (!base.endsWith("_test")) {
            throw new IllegalStateException("Los tests solo vacían bases cuyo nombre termina en \"_test\"; "
                    + "esta es \"" + base + "\". Revise MEDICAMENTOS_TEST_DB_URL.");
        }
        jdbc.sql("TRUNCATE medicamentos, idempotencia_salidas").update();
        jdbc.sql("ALTER SEQUENCE medicamentos_id_seq RESTART WITH 1").update();
    }
}
