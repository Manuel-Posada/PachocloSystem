package com.pachoclosystem.pachoclosystem;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Solo en los tests: cada contexto de Spring arranca con el esquema recién
 * creado (Flyway {@code clean} + {@code migrate}), igual que antes arrancaba con
 * los repositorios en memoria vacíos. Antes de borrar nada comprueba que la
 * base termina en {@code _test}; si no, el contexto no arranca.
 *
 * <p>Con {@value #PROP_CONSERVAR_DATOS}{@code =true} solo migra (lo usa la
 * prueba de reinicio para arrancar sobre los datos del arranque anterior).</p>
 *
 * <p>Está en {@code src/test}, así que solo lo encuentra el escaneo de
 * componentes de los tests. Necesita {@code spring.flyway.clean-disabled=false},
 * que solo fija la configuración de tests.</p>
 */
@Configuration(proxyBeanMethods = false)
public class EsquemaLimpioEnTestsConfig {

    public static final String PROP_CONSERVAR_DATOS = "pruebas.conservar-datos";

    @Bean
    FlywayMigrationStrategy esquemaLimpio(Environment entorno) {
        return flyway -> {
            BaseDeDatosDePruebas.comprobarQueEsDePruebas(flyway.getConfiguration().getDataSource());
            if (!entorno.getProperty(PROP_CONSERVAR_DATOS, Boolean.class, false)) {
                flyway.clean();
            }
            flyway.migrate();
        };
    }
}
