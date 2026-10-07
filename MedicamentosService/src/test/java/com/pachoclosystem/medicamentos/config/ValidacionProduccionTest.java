package com.pachoclosystem.medicamentos.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/** Reglas del perfil prod: qué hace falta para arrancar en producción. */
class ValidacionProduccionTest {

    private static final String CLAVE_SERVICIO = "clave-servicio-de-prueba-de-32-caracteres";
    private static final String PASSWORD_BD = "password-bd-de-prueba";

    private static MockEnvironment completa() {
        return new MockEnvironment()
                .withProperty("medicamentos.api-key", CLAVE_SERVICIO)
                .withProperty("spring.datasource.url", "jdbc:postgresql://postgres.railway.internal:5432/medicamentos")
                .withProperty("spring.datasource.password", PASSWORD_BD);
    }

    @Test
    void conTodaLaConfiguracionArranca() {
        assertThatNoException().isThrownBy(() -> ValidacionProduccion.validar(completa()));
    }

    @Test
    void sinNadaEnumeraTodosLosProblemasALaVez() {
        assertThatIllegalStateException()
                .isThrownBy(() -> ValidacionProduccion.validar(new MockEnvironment()))
                .withMessageContaining("perfil prod")
                .withMessageContaining("MEDICAMENTOS_API_KEY")
                .withMessageContaining("MEDICAMENTOS_DB_URL")
                .withMessageContaining("MEDICAMENTOS_DB_PASSWORD");
    }

    @Test
    void sinClaveDeServicioNuncaArrancaYElMensajeNoIncluyeValores() {
        MockEnvironment entorno = completa().withProperty("medicamentos.api-key", "clave-corta-secreta");

        assertThatIllegalStateException()
                .isThrownBy(() -> ValidacionProduccion.validar(entorno))
                .withMessageContaining("MEDICAMENTOS_API_KEY")
                .satisfies(error -> assertThat(error.getMessage()).doesNotContain("clave-corta-secreta", PASSWORD_BD));
        assertThatNoException().isThrownBy(() -> ValidacionProduccion.validar(
                completa().withProperty("medicamentos.api-key", "a".repeat(32))));
    }

    @Test
    void laBaseNoPuedeSerLocal() {
        assertThatIllegalStateException().isThrownBy(() -> ValidacionProduccion.validar(
                completa().withProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/medicamentos")))
                .withMessageContaining("MEDICAMENTOS_DB_URL");
        assertThat(ValidacionProduccion.apuntaALocalhost("jdbc:postgresql://127.0.0.1:5432/x")).isTrue();
        assertThat(ValidacionProduccion.apuntaALocalhost("jdbc:postgresql://[::1]:5432/x")).isTrue();
        assertThat(ValidacionProduccion.apuntaALocalhost("jdbc:postgresql://postgres.railway.internal:5432/x"))
                .isFalse();
    }
}
