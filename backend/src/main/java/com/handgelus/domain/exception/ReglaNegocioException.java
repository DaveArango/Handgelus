package com.handgelus.domain.exception;

/**
 * Se lanza cuando una operación viola una regla de negocio (RN-xx / SWR-xx).
 */
public class ReglaNegocioException extends DominioException {

    private final String regla;

    public ReglaNegocioException(String mensaje) {
        this(null, mensaje);
    }

    public ReglaNegocioException(String regla, String mensaje) {
        super(mensaje);
        this.regla = regla;
    }

    public String getRegla() {
        return regla;
    }
}
