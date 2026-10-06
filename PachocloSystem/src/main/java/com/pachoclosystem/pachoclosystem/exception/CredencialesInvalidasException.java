package com.pachoclosystem.pachoclosystem.exception;

/**
 * Fallo de autenticación en el login: usuario inexistente, contraseña
 * incorrecta o usuario desactivado. La API responde 401 con el mensaje genérico
 * "Credenciales inválidas." y nunca revela cuál de las tres causas ocurrió.
 */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Credenciales inválidas.");
    }
}