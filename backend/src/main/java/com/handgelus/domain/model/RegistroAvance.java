package com.handgelus.domain.model;

import com.handgelus.domain.exception.ReglaNegocioException;

import java.time.LocalDateTime;

/** Entrada de la bitácora de avance de un servicio (sesión de tatuaje o avance de obra). */
public class RegistroAvance {

    private final Long id;
    private final Long solicitudId;
    private final Long autorId;
    private final String titulo;
    private final String descripcion;
    private final Integer progreso;
    private final String imagen;
    private final LocalDateTime fecha;

    public RegistroAvance(Long id, Long solicitudId, Long autorId, String titulo, String descripcion,
                          Integer progreso, String imagen, LocalDateTime fecha) {
        this.id = id;
        this.solicitudId = solicitudId;
        this.autorId = autorId;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.progreso = progreso;
        this.imagen = imagen;
        this.fecha = fecha;
    }

    public static RegistroAvance nuevo(Long solicitudId, Long autorId, String titulo, String descripcion,
                                       Integer progreso, String imagen, LocalDateTime ahora) {
        if (titulo == null || titulo.isBlank()) {
            throw new ReglaNegocioException("El avance debe tener un título.");
        }
        return new RegistroAvance(null, solicitudId, autorId, titulo.trim(),
                descripcion == null ? null : descripcion.trim(), progreso, imagen, ahora);
    }

    public Long getId() { return id; }
    public Long getSolicitudId() { return solicitudId; }
    public Long getAutorId() { return autorId; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public Integer getProgreso() { return progreso; }
    public String getImagen() { return imagen; }
    public LocalDateTime getFecha() { return fecha; }
}
