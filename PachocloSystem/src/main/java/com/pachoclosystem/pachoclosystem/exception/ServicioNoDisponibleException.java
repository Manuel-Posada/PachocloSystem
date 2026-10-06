package com.pachoclosystem.pachoclosystem.exception;

/**
 * Un servicio externo no respondió (conexión rechazada o timeout). El mensaje es
 * solo para el log: al cliente le llega un 503 con un texto genérico.
 */
public class ServicioNoDisponibleException extends RuntimeException {

    public ServicioNoDisponibleException(String detalle, Throwable causa) {
        super(detalle, causa);
    }
}
