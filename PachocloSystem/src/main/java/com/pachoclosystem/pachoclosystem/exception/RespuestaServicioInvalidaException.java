package com.pachoclosystem.pachoclosystem.exception;

/**
 * Un servicio externo respondió algo que no se puede trasladar al cliente (5xx,
 * 401/403 por mala configuración o un cuerpo ilegible). El mensaje es solo para
 * el log: al cliente le llega un 502 con un texto genérico.
 */
public class RespuestaServicioInvalidaException extends RuntimeException {

    public RespuestaServicioInvalidaException(String detalle) {
        super(detalle);
    }

    public RespuestaServicioInvalidaException(String detalle, Throwable causa) {
        super(detalle, causa);
    }
}
