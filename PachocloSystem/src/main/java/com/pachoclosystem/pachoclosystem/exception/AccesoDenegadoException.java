package com.pachoclosystem.pachoclosystem.exception;

/**
 * 403 decidido por una regla de negocio que no se puede expresar por URL (por
 * ejemplo, según el tipo de registro del cuerpo). El mensaje se muestra al
 * cliente, así que debe explicar el motivo sin datos internos.
 */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
