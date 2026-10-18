package com.handgelus.domain.exception;

public class RecursoNoEncontradoException extends DominioException {

    public RecursoNoEncontradoException(String recurso, Object id) {
        super(recurso + " con identificador " + id + " no existe.");
    }

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
