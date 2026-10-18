package com.handgelus.application.port.in.modelo;

import com.handgelus.domain.model.Rol;
import com.handgelus.domain.model.Usuario;

import java.time.LocalDateTime;

/** Datos públicos de un usuario (nunca expone el hash de la contraseña). */
public record UsuarioVista(Long id, String nombre, String correo, String telefono, Rol rol, String rolEtiqueta,
                           boolean activo, LocalDateTime fechaRegistro) {

    public static UsuarioVista de(Usuario u) {
        if (u == null) {
            return null;
        }
        return new UsuarioVista(u.getId(), u.getNombre(), u.getCorreo(), u.getTelefono(), u.getRol(),
                u.getRol().getEtiqueta(), u.isActivo(), u.getFechaRegistro());
    }
}
