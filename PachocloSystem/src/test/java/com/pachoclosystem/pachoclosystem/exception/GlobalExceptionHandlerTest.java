package com.pachoclosystem.pachoclosystem.exception;

import com.pachoclosystem.pachoclosystem.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias directas del {@link GlobalExceptionHandler}: cubren los
 * caminos no alcanzables desde MockMvc (los controladores solo reciben
 * parámetros {@code String}, así que nunca se produce una discrepancia de tipo,
 * y los contenidos no soportados se prueban aquí además de en la API).
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler manejador = new GlobalExceptionHandler();

    @Test
    void argumentoDeTipoIncorrectoResponde400ConMensajeGenerico() {
        var excepcion = new MethodArgumentTypeMismatchException(
                "no-es-un-numero", Integer.class, "edad", null, null);

        ResponseEntity<ErrorResponse> respuesta = manejador.argumentoDeTipoIncorrecto(excepcion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().status()).isEqualTo(400);
        assertThat(respuesta.getBody().error()).isEqualTo("Bad Request");
        assertThat(respuesta.getBody().mensajes())
                .containsExactly("El valor de un parámetro de la petición no es válido.");
        assertThat(respuesta.getBody().mensajes().get(0)).doesNotContain("Exception");
    }

    @Test
    void recursoInexistenteResponde404ConMensajeGenerico() {
        var excepcion = new NoResourceFoundException(
                HttpMethod.GET, "/api/noexiste", "No static resource api/noexiste.");

        ResponseEntity<ErrorResponse> respuesta = manejador.recursoNoEncontrado(excepcion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().status()).isEqualTo(404);
        assertThat(respuesta.getBody().error()).isEqualTo("Not Found");
        assertThat(respuesta.getBody().mensajes())
                .containsExactly("No se encontró el recurso solicitado.");
    }

    @Test
    void metodoNoSoportadoResponde405YConservaCabeceraAllow() {
        var excepcion = new HttpRequestMethodNotSupportedException(
                "DELETE", List.of("GET", "POST"));

        ResponseEntity<ErrorResponse> respuesta = manejador.metodoNoSoportado(excepcion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(respuesta.getHeaders().getAllow())
                .containsExactlyInAnyOrder(HttpMethod.GET, HttpMethod.POST);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().status()).isEqualTo(405);
        assertThat(respuesta.getBody().mensajes())
                .containsExactly("El método de la petición no está permitido en este recurso.");
    }

    @Test
    void tipoDeContenidoNoSoportadoResponde415() {
        var excepcion = new HttpMediaTypeNotSupportedException(
                "text/plain", List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<ErrorResponse> respuesta = manejador.tipoNoSoportado(excepcion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().status()).isEqualTo(415);
        assertThat(respuesta.getBody().mensajes())
                .containsExactly("El tipo de contenido de la petición no está soportado por este recurso.");
    }

    @Test
    void tipoAceptadoNoDisponibleResponde406() {
        var excepcion = new HttpMediaTypeNotAcceptableException("text/plain");

        ResponseEntity<ErrorResponse> respuesta = manejador.tipoAceptadoNoDisponible(excepcion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().status()).isEqualTo(406);
        assertThat(respuesta.getBody().mensajes())
                .containsExactly("El tipo de contenido solicitado en la respuesta no está disponible.");
    }

    @Test
    void accesoDenegadoDeNegocioResponde403UniformeConSuMotivo() {
        var excepcion = new AccesoDenegadoException("Los enfermeros no pueden crear diagnósticos.");

        ResponseEntity<ErrorResponse> respuesta = manejador.accesoDenegado(excepcion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().status()).isEqualTo(403);
        assertThat(respuesta.getBody().error()).isEqualTo("Forbidden");
        assertThat(respuesta.getBody().mensajes())
                .containsExactly("Los enfermeros no pueden crear diagnósticos.");
    }

    @Test
    void errorInesperadoResponde500GenericoSinDetallarElFalloNiLaClase() {
        var excepcion = new IllegalStateException("clave secreta interna");

        ResponseEntity<ErrorResponse> respuesta = manejador.errorInterno(excepcion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().status()).isEqualTo(500);
        assertThat(respuesta.getBody().error()).isEqualTo("Internal Server Error");
        assertThat(respuesta.getBody().mensajes())
                .containsExactly("Se produjo un error interno. Vuelva a intentarlo más tarde.");
        assertThat(respuesta.getBody().mensajes().get(0))
                .doesNotContain("clave secreta interna")
                .doesNotContain("Exception");
    }

    @Test
    void accesoDenegadoResponde403UniformeSinDetallesInternos() {
        var excepcion = new AccessDeniedException("detalle interno secreto");

        ResponseEntity<ErrorResponse> respuesta = manejador.accesoDenegadoSeguridad(excepcion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().status()).isEqualTo(403);
        assertThat(respuesta.getBody().error()).isEqualTo("Forbidden");
        assertThat(respuesta.getBody().mensajes())
                .containsExactly("No tiene permisos para realizar esta operación.");
        assertThat(respuesta.getBody().mensajes().get(0))
                .doesNotContain("detalle interno secreto")
                .doesNotContain("Exception");
    }

    @Test
    void autorizacionDenegadaResponde403Uniforme() {
        var excepcion = new AuthorizationDeniedException("denegado por autorización");

        ResponseEntity<ErrorResponse> respuesta = manejador.accesoDenegadoSeguridad(excepcion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().status()).isEqualTo(403);
        assertThat(respuesta.getBody().mensajes())
                .containsExactly("No tiene permisos para realizar esta operación.");
    }

    @Test
    void laRedDeSeguridadNoPisaAlManejadorDeCuerpoInvalido() {
        var excepcion = new org.springframework.http.converter.HttpMessageNotReadableException(
                "JSON inválido", new org.springframework.http.HttpInputMessage() {
                    @Override
                    public org.springframework.http.HttpHeaders getHeaders() {
                        return new org.springframework.http.HttpHeaders();
                    }

                    @Override
                    public java.io.InputStream getBody() {
                        return java.io.InputStream.nullInputStream();
                    }
                });

        ResponseEntity<ErrorResponse> respuesta = manejador.cuerpoIlegible(excepcion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().mensajes())
                .containsExactly("El cuerpo de la petición es inválido o tiene valores no reconocidos.");
    }
}
