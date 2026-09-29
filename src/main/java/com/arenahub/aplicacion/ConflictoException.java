package com.arenahub.aplicacion;

/** El recurso ya existe o está ocupado (HTTP 409). */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
