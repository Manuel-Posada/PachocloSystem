package com.pachoclosystem.pachoclosystem.exception;

/**
 * Una salida de stock con clave de idempotencia no respondió ni al reintento:
 * puede haberse hecho o no. Como MedicamentosService recuerda la clave, repetir
 * la petición con la misma clave no descuenta dos veces. El mensaje es solo para
 * el log: al cliente le llega un 503 que le dice que puede reintentar.
 */
public class SalidaNoConfirmadaException extends ServicioNoDisponibleException {

    public SalidaNoConfirmadaException(String detalle, Throwable causa) {
        super(detalle, causa);
    }
}
