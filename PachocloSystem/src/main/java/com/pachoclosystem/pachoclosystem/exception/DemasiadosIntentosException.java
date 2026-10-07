package com.pachoclosystem.pachoclosystem.exception;

/**
 * El login está temporalmente bloqueado por acumular demasiados intentos
 * fallidos (por usuario o por IP). La API responde {@code 429} con el cuerpo de
 * error uniforme y la cabecera {@code Retry-After} en segundos. El mensaje no
 * revela si el usuario existe.
 */
public class DemasiadosIntentosException extends RuntimeException {

    private final long segundosRestantes;

    public DemasiadosIntentosException(long segundosRestantes) {
        super("Demasiados intentos fallidos. Inténtelo de nuevo más tarde.");
        this.segundosRestantes = segundosRestantes;
    }

    public long getSegundosRestantes() {
        return segundosRestantes;
    }
}