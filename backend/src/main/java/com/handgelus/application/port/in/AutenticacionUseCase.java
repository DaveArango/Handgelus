package com.handgelus.application.port.in;

import com.handgelus.application.port.in.modelo.SesionVista;
import com.handgelus.application.port.in.modelo.UsuarioVista;
import com.handgelus.domain.model.Actor;

/** F-01 (SWR-02) autenticación · F-03 (SWR-06) registro de clientes. */
public interface AutenticacionUseCase {

    SesionVista iniciarSesion(String correo, String password);

    SesionVista registrarCliente(RegistroClienteComando comando);

    UsuarioVista perfil(Actor actor);

    UsuarioVista actualizarPerfil(String nombre, String telefono, Actor actor);

    record RegistroClienteComando(String nombre, String correo, String telefono, String password,
                                  boolean aceptaTratamientoDatos) {
    }
}
