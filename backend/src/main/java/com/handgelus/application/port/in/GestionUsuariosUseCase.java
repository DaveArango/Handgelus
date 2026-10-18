package com.handgelus.application.port.in;

import com.handgelus.application.port.in.modelo.UsuarioVista;
import com.handgelus.domain.model.Actor;
import com.handgelus.domain.model.Rol;

import java.util.List;

/** F-01 · Gestionar usuarios del sistema (SWR-01). */
public interface GestionUsuariosUseCase {

    UsuarioVista crear(CrearUsuarioComando comando, Actor actor);

    List<UsuarioVista> listar(Rol rol, Actor actor);

    UsuarioVista actualizar(Long id, ActualizarUsuarioComando comando, Actor actor);

    UsuarioVista cambiarEstado(Long id, boolean activo, Actor actor);

    List<UsuarioVista> listarTatuadores();

    record CrearUsuarioComando(String nombre, String correo, String telefono, String password, Rol rol) {
    }

    record ActualizarUsuarioComando(String nombre, String telefono, Rol rol, String nuevaPassword) {
    }
}
