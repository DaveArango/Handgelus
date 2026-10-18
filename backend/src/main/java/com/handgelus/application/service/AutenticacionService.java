package com.handgelus.application.service;

import com.handgelus.application.port.in.AutenticacionUseCase;
import com.handgelus.application.port.in.modelo.SesionVista;
import com.handgelus.application.port.in.modelo.UsuarioVista;
import com.handgelus.application.port.out.CifradoPasswordPort;
import com.handgelus.application.port.out.TokenPort;
import com.handgelus.application.port.out.UsuarioRepositoryPort;
import com.handgelus.domain.exception.CredencialesInvalidasException;
import com.handgelus.domain.exception.DominioException;
import com.handgelus.domain.exception.RecursoNoEncontradoException;
import com.handgelus.domain.exception.ReglaNegocioException;
import com.handgelus.domain.model.Actor;
import com.handgelus.domain.model.Rol;
import com.handgelus.domain.model.Usuario;

import java.time.Clock;
import java.time.LocalDateTime;

public class AutenticacionService implements AutenticacionUseCase {

    private final UsuarioRepositoryPort usuarios;
    private final CifradoPasswordPort cifrado;
    private final TokenPort tokens;
    private final Clock reloj;

    public AutenticacionService(UsuarioRepositoryPort usuarios, CifradoPasswordPort cifrado, TokenPort tokens,
                                Clock reloj) {
        this.usuarios = usuarios;
        this.cifrado = cifrado;
        this.tokens = tokens;
        this.reloj = reloj;
    }

    @Override
    public SesionVista iniciarSesion(String correo, String password) {
        String correoNormalizado;
        try {
            correoNormalizado = Usuario.normalizarCorreo(correo);
        } catch (DominioException e) {
            throw new CredencialesInvalidasException();
        }
        Usuario usuario = usuarios.buscarPorCorreo(correoNormalizado)
                .orElseThrow(CredencialesInvalidasException::new);
        if (password == null || !cifrado.coincide(password, usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }
        if (!usuario.isActivo()) {
            throw new CredencialesInvalidasException("Su cuenta está inactiva. Comuníquese con el estudio.");
        }
        return new SesionVista(tokens.generar(usuario), UsuarioVista.de(usuario));
    }

    @Override
    public SesionVista registrarCliente(RegistroClienteComando c) {
        if (!c.aceptaTratamientoDatos()) {
            throw new ReglaNegocioException("RNF-09",
                    "Debe autorizar el tratamiento de sus datos personales (Ley 1581 de 2012).");
        }
        String correo = Usuario.normalizarCorreo(c.correo());
        if (usuarios.existeCorreo(correo)) {
            throw new ReglaNegocioException("SWR-06", "Ya existe una cuenta registrada con este correo.");
        }
        PoliticaPassword.validar(c.password());
        Usuario cliente = usuarios.guardar(Usuario.nuevo(c.nombre(), correo, c.telefono(),
                cifrado.cifrar(c.password()), Rol.CLIENTE, LocalDateTime.now(reloj)));
        return new SesionVista(tokens.generar(cliente), UsuarioVista.de(cliente));
    }

    @Override
    public UsuarioVista perfil(Actor actor) {
        return UsuarioVista.de(buscar(actor.id()));
    }

    @Override
    public UsuarioVista actualizarPerfil(String nombre, String telefono, Actor actor) {
        Usuario usuario = buscar(actor.id());
        usuario.actualizarDatos(nombre, telefono);
        return UsuarioVista.de(usuarios.guardar(usuario));
    }

    private Usuario buscar(Long id) {
        return usuarios.buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("El usuario", id));
    }
}
