package com.handgelus.infrastructure.adapter.in.web;

import com.handgelus.application.port.in.GestionUsuariosUseCase;
import com.handgelus.application.port.in.GestionUsuariosUseCase.ActualizarUsuarioComando;
import com.handgelus.application.port.in.GestionUsuariosUseCase.CrearUsuarioComando;
import com.handgelus.application.port.in.modelo.UsuarioVista;
import com.handgelus.domain.model.Actor;
import com.handgelus.domain.model.Rol;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** F-01 · Gestionar usuarios del sistema. */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final GestionUsuariosUseCase usuarios;

    public UsuarioController(GestionUsuariosUseCase usuarios) {
        this.usuarios = usuarios;
    }

    public record CrearUsuarioRequest(@NotBlank(message = "El nombre es obligatorio") String nombre,
                                      @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo no válido") String correo,
                                      @NotBlank(message = "El teléfono es obligatorio") String telefono,
                                      @NotBlank(message = "La contraseña es obligatoria") String password,
                                      @NotNull(message = "Seleccione un rol") Rol rol) {
    }

    public record ActualizarUsuarioRequest(@NotBlank(message = "El nombre es obligatorio") String nombre,
                                           @NotBlank(message = "El teléfono es obligatorio") String telefono,
                                           Rol rol, String nuevaPassword) {
    }

    public record EstadoRequest(boolean activo) {
    }

    @GetMapping
    public List<UsuarioVista> listar(@RequestParam(required = false) Rol rol, Actor actor) {
        return usuarios.listar(rol, actor);
    }

    @GetMapping("/tatuadores")
    public List<UsuarioVista> tatuadores() {
        return usuarios.listarTatuadores();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioVista crear(@Valid @RequestBody CrearUsuarioRequest r, Actor actor) {
        return usuarios.crear(new CrearUsuarioComando(r.nombre(), r.correo(), r.telefono(), r.password(), r.rol()), actor);
    }

    @PutMapping("/{id}")
    public UsuarioVista actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarUsuarioRequest r, Actor actor) {
        return usuarios.actualizar(id, new ActualizarUsuarioComando(r.nombre(), r.telefono(), r.rol(), r.nuevaPassword()),
                actor);
    }

    @PatchMapping("/{id}/estado")
    public UsuarioVista cambiarEstado(@PathVariable Long id, @RequestBody EstadoRequest r, Actor actor) {
        return usuarios.cambiarEstado(id, r.activo(), actor);
    }
}
