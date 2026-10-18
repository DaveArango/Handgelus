package com.handgelus.domain.model;

import com.handgelus.domain.exception.AccesoDenegadoException;

import java.util.Arrays;

/**
 * Usuario autenticado que ejecuta un caso de uso. Lo construye el adaptador
 * de entrada a partir del token y lo usan los servicios para validar permisos.
 */
public record Actor(Long id, String nombre, Rol rol) {

    public boolean es(Rol... roles) {
        return Arrays.asList(roles).contains(rol);
    }

    public boolean esCliente() {
        return rol == Rol.CLIENTE;
    }

    public void exigir(Rol... roles) {
        if (!es(roles)) {
            throw new AccesoDenegadoException("Su rol no tiene permiso para realizar esta acción.");
        }
    }

    /** Un cliente solo puede operar sobre sus propios recursos (RF-13 · SWR-19). */
    public void exigirPropietarioOPersonal(Long clienteId) {
        if (esCliente() && !id.equals(clienteId)) {
            throw new AccesoDenegadoException("Solo puede consultar sus propios servicios.");
        }
    }
}
