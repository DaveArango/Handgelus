package com.handgelus.domain.exception;

/**
 * Excepción base del dominio de Handgelus.
 * No depende de ningún framework: el adaptador web la traduce a HTTP.
 */
public class DominioException extends RuntimeException {

    public DominioException(String mensaje) {
        super(mensaje);
    }
}
