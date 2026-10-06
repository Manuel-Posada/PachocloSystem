package com.pachoclosystem.pachoclosystem.exception;

/** La petición choca con el estado actual de otro recurso (p. ej. un duplicado). */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
