package com.handgelus.domain.exception;

public class CredencialesInvalidasException extends DominioException {

    public CredencialesInvalidasException() {
        super("Correo o contraseña incorrectos.");
    }

    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}
