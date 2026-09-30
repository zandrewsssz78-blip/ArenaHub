package com.arenahub.dominio;

/** Se lanza cuando una operación viola una regla del dominio. */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
