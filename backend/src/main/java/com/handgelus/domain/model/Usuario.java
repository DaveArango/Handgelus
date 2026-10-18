package com.handgelus.domain.model;

import com.handgelus.domain.exception.ReglaNegocioException;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Usuario del sistema: cliente o miembro del equipo interno (F-01, F-03 · SWR-06).
 */
public class Usuario {

    private static final Pattern CORREO = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private final Long id;
    private String nombre;
    private final String correo;
    private String telefono;
    private String passwordHash;
    private Rol rol;
    private boolean activo;
    private final LocalDateTime fechaRegistro;

    public Usuario(Long id, String nombre, String correo, String telefono, String passwordHash,
                   Rol rol, boolean activo, LocalDateTime fechaRegistro) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.telefono = telefono;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.activo = activo;
        this.fechaRegistro = fechaRegistro;
    }

    public static Usuario nuevo(String nombre, String correo, String telefono, String passwordHash,
                                Rol rol, LocalDateTime ahora) {
        validarNombre(nombre);
        String correoNormalizado = normalizarCorreo(correo);
        validarTelefono(telefono);
        Objects.requireNonNull(rol, "El rol es obligatorio.");
        return new Usuario(null, nombre.trim(), correoNormalizado, telefono.trim(), passwordHash, rol, true, ahora);
    }

    public static String normalizarCorreo(String correo) {
        if (correo == null || !CORREO.matcher(correo.trim()).matches()) {
            throw new ReglaNegocioException("El correo electrónico no tiene un formato válido.");
        }
        return correo.trim().toLowerCase();
    }

    private static void validarNombre(String nombre) {
        if (nombre == null || nombre.trim().length() < 3) {
            throw new ReglaNegocioException("El nombre debe tener al menos 3 caracteres.");
        }
    }

    private static void validarTelefono(String telefono) {
        if (telefono == null || !telefono.replaceAll("[\\s+()-]", "").matches("\\d{7,15}")) {
            throw new ReglaNegocioException("El teléfono debe contener entre 7 y 15 dígitos.");
        }
    }

    public void actualizarDatos(String nombre, String telefono) {
        validarNombre(nombre);
        validarTelefono(telefono);
        this.nombre = nombre.trim();
        this.telefono = telefono.trim();
    }

    public void cambiarRol(Rol nuevoRol) {
        this.rol = Objects.requireNonNull(nuevoRol);
    }

    public void cambiarPassword(String nuevoHash) {
        this.passwordHash = Objects.requireNonNull(nuevoHash);
    }

    public void activar() {
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getTelefono() { return telefono; }
    public String getPasswordHash() { return passwordHash; }
    public Rol getRol() { return rol; }
    public boolean isActivo() { return activo; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
}
