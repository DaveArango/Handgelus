package com.handgelus.infrastructure.adapter.in.web;

import com.handgelus.application.port.in.AutenticacionUseCase;
import com.handgelus.application.port.in.AutenticacionUseCase.RegistroClienteComando;
import com.handgelus.application.port.in.modelo.SesionVista;
import com.handgelus.application.port.in.modelo.UsuarioVista;
import com.handgelus.domain.model.Actor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** F-01 autenticación · F-03 registro del cliente. */
@RestController
@RequestMapping("/api/auth")
public class AutenticacionController {

    private final AutenticacionUseCase autenticacion;

    public AutenticacionController(AutenticacionUseCase autenticacion) {
        this.autenticacion = autenticacion;
    }

    public record LoginRequest(@NotBlank(message = "Ingrese su correo") String correo,
                               @NotBlank(message = "Ingrese su contraseña") String password) {
    }

    public record RegistroRequest(
            @NotBlank(message = "El nombre es obligatorio") @Size(min = 3, max = 120, message = "Entre 3 y 120 caracteres") String nombre,
            @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo no válido") String correo,
            @NotBlank(message = "El teléfono es obligatorio") String telefono,
            @NotBlank(message = "La contraseña es obligatoria") @Size(min = 8, message = "Mínimo 8 caracteres") String password,
            boolean aceptaTratamientoDatos) {
    }

    public record PerfilRequest(@NotBlank(message = "El nombre es obligatorio") String nombre,
                                @NotBlank(message = "El teléfono es obligatorio") String telefono) {
    }

    @PostMapping("/login")
    public SesionVista login(@Valid @RequestBody LoginRequest r) {
        return autenticacion.iniciarSesion(r.correo(), r.password());
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public SesionVista registro(@Valid @RequestBody RegistroRequest r) {
        return autenticacion.registrarCliente(new RegistroClienteComando(r.nombre(), r.correo(), r.telefono(),
                r.password(), r.aceptaTratamientoDatos()));
    }

    @GetMapping("/perfil")
    public UsuarioVista perfil(Actor actor) {
        return autenticacion.perfil(actor);
    }

    @PutMapping("/perfil")
    public UsuarioVista actualizarPerfil(@Valid @RequestBody PerfilRequest r, Actor actor) {
        return autenticacion.actualizarPerfil(r.nombre(), r.telefono(), actor);
    }
}
