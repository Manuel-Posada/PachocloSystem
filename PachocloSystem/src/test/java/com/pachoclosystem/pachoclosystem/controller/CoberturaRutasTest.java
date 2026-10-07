package com.pachoclosystem.pachoclosystem.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cinturón estructural de la autorización: enumera los endpoints que Spring
 * registra de verdad en {@link RequestMappingHandlerMapping} y falla si alguno
 * no figura en {@link MatrizAutorizacion}. Así, un controlador nuevo sin regla
 * declarada (y por tanto potencialmente sin autorización) rompe la suite en vez
 * de pasar inadvertido.
 */
class CoberturaRutasTest extends MockMvcBaseTest {

    /**
     * Endpoints internos de Spring que no son API propia y quedan fuera de la
     * matriz; la lista es explícita y no debe crecer sin justificarlo:
     * <ul>
     *   <li>{@code /error}: {@code BasicErrorController}, que renderiza el error
     *       uniforme de Spring Boot.</li>
     *   <li>{@code /actuator/**}: Spring Boot Actuator; hoy no está en el
     *       classpath, se documenta por si se añadiera en el futuro.</li>
     * </ul>
     */
    private static final Set<String> PATRONES_INTERNOS_SPRING = Set.of("/error", "/actuator/**");

    @Autowired
    private RequestMappingHandlerMapping mapeoDeRutas;

    @Test
    void todaRutaMapeadaFiguraEnLaMatrizDeAutorizacion() {
        Set<String> rutasMapeadas = new TreeSet<>();
        mapeoDeRutas.getHandlerMethods().keySet().forEach(info -> acumularRutas(info, rutasMapeadas));

        assertThat(rutasMapeadas)
                .as("toda ruta mapeada debe figurar en %s", MatrizAutorizacion.class.getSimpleName())
                .isNotEmpty()
                .isSubsetOf(MatrizAutorizacion.claves());
    }

    /** Añade a {@code destino} las claves {@code VERBO patrón} que declara el mapeo. */
    private static void acumularRutas(RequestMappingInfo info, Set<String> destino) {
        Set<RequestMethod> metodos = info.getMethodsCondition().getMethods();
        for (String patron : info.getPatternValues()) {
            if (PATRONES_INTERNOS_SPRING.contains(patron)) {
                continue;
            }
            if (metodos.isEmpty()) {
                destino.add("* " + patron);
            } else {
                metodos.forEach(metodo -> destino.add(metodo.name() + " " + patron));
            }
        }
    }
}
