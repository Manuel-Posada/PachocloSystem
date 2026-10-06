package com.pachoclosystem.pachoclosystem.client;

import com.pachoclosystem.pachoclosystem.exception.ServicioNoDisponibleException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Cliente real (el bean de la aplicación, con sus timeouts) contra un puerto
 * cerrado: confirma que un servicio de medicamentos caído termina en
 * {@link ServicioNoDisponibleException} sin mocks. La URL de prueba
 * ({@code http://127.0.0.1:1}) está en el application.properties de test.
 */
@SpringBootTest
class MedicamentosClientCaidoTest {

    @Autowired
    private MedicamentosClient cliente;

    @Test
    void servicioCaidoLanzaServicioNoDisponible() {
        assertThatExceptionOfType(ServicioNoDisponibleException.class)
                .isThrownBy(() -> cliente.listar(null));
    }
}
