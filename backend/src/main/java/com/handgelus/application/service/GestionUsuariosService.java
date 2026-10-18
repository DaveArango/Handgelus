package com.handgelus.application.service;

import com.handgelus.application.port.in.GestionUsuariosUseCase;
import com.handgelus.application.port.in.modelo.UsuarioVista;
import com.handgelus.application.port.out.CifradoPasswordPort;
import com.handgelus.application.port.out.UsuarioRepositoryPort;
import com.handgelus.domain.exception.RecursoNoEncontradoException;
import com.handgelus.domain.exception.ReglaNegocioException;
import com.handgelus.domain.model.Actor;
import com.handgelus.domain.model.Rol;
import com.handgelus.domain.model.Usuario;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** F-01 · Gestionar usuarios del sistema. Solo el administrador crea cuentas internas (RF-16). */
public class GestionUsuariosService implements GestionUsuariosUseCase {

    private final UsuarioRepositoryPort usuarios;
    private final CifradoPasswordPort cifrado;
    private final Clock reloj;

    public GestionUsuariosService(UsuarioRepositoryPort usuarios, CifradoPasswordPort cifrado, Clock reloj) {
        this.usuarios = usuarios;
        this.cifrado = cifrado;
        this.reloj = reloj;
    }

    @Override
    public UsuarioVista crear(CrearUsuarioComando c, Actor actor) {
        actor.exigir(Rol.ADMINISTRADOR);
        if (c.rol() == null) {
            throw new ReglaNegocioException("SWR-01", "Asigne un rol a la cuenta.");
        }
        String correo = Usuario.normalizarCorreo(c.correo());
        if (usuarios.existeCorreo(correo)) {
            throw new ReglaNegocioException("SWR-02", "Ya existe una cuenta con este correo.");
        }
        PoliticaPassword.validar(c.password());
        Usuario nuevo = Usuario.nuevo(c.nombre(), correo, c.telefono(), cifrado.cifrar(c.password()), c.rol(),
                LocalDateTime.now(reloj));
        return UsuarioVista.de(usuarios.guardar(nuevo));
    }

    @Override
    public List<UsuarioVista> listar(Rol rol, Actor actor) {
        actor.exigir(Rol.ADMINISTRADOR);
        List<Usuario> lista = rol == null ? usuarios.listar() : usuarios.listarPorRol(rol);
        return lista.stream()
                .sorted(Comparator.comparing(Usuario::getRol).thenComparing(Usuario::getNombre))
                .map(UsuarioVista::de).toList();
    }

    @Override
    public UsuarioVista actualizar(Long id, ActualizarUsuarioComando c, Actor actor) {
        actor.exigir(Rol.ADMINISTRADOR);
        Usuario usuario = buscar(id);
        usuario.actualizarDatos(c.nombre(), c.telefono());
        if (c.rol() != null && c.rol() != usuario.getRol()) {
            if (usuario.getId().equals(actor.id())) {
                throw new ReglaNegocioException("No puede cambiar el rol de su propia cuenta.");
            }
            usuario.cambiarRol(c.rol());
        }
        if (c.nuevaPassword() != null && !c.nuevaPassword().isBlank()) {
            PoliticaPassword.validar(c.nuevaPassword());
            usuario.cambiarPassword(cifrado.cifrar(c.nuevaPassword()));
        }
        return UsuarioVista.de(usuarios.guardar(usuario));
    }

    @Override
    public UsuarioVista cambiarEstado(Long id, boolean activo, Actor actor) {
        actor.exigir(Rol.ADMINISTRADOR);
        Usuario usuario = buscar(id);
        if (!activo && usuario.getId().equals(actor.id())) {
            throw new ReglaNegocioException("No puede desactivar su propia cuenta.");
        }
        if (activo) {
            usuario.activar();
        } else {
            usuario.desactivar();
        }
        return UsuarioVista.de(usuarios.guardar(usuario));
    }

    @Override
    public List<UsuarioVista> listarTatuadores() {
        return usuarios.listarPorRol(Rol.TATUADOR).stream().filter(Usuario::isActivo)
                .sorted(Comparator.comparing(Usuario::getNombre)).map(UsuarioVista::de).toList();
    }

    private Usuario buscar(Long id) {
        return usuarios.buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("El usuario", id));
    }
}
