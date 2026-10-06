package com.pachoclosystem.medicamentos.exception;

import java.util.List;

public class SolicitudInvalidaException extends RuntimeException {

    private final List<String> errores;

    public SolicitudInvalidaException(List<String> errores) {
        super(String.join("; ", errores));
        this.errores = List.copyOf(errores);
    }

    public SolicitudInvalidaException(String error) {
        this(List.of(error));
    }

    public List<String> getErrores() {
        return errores;
    }
}
