package com.handgelus.domain.model;

import com.handgelus.domain.exception.ReglaNegocioException;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Cita de tatuaje en una jornada de la agenda del tatuador (F-06 · RF-10). */
public class Cita {

    private final Long id;
    private final Long solicitudId;
    private final Long tatuadorId;
    private final LocalDate fecha;
    private final Jornada jornada;
    private EstadoCita estado;
    private final int numeroSesion;
    private String notas;
    private final LocalDateTime fechaCreacion;

    public Cita(Long id, Long solicitudId, Long tatuadorId, LocalDate fecha, Jornada jornada, EstadoCita estado,
                int numeroSesion, String notas, LocalDateTime fechaCreacion) {
        this.id = id;
        this.solicitudId = solicitudId;
        this.tatuadorId = tatuadorId;
        this.fecha = fecha;
        this.jornada = jornada;
        this.estado = estado;
        this.numeroSesion = numeroSesion;
        this.notas = notas;
        this.fechaCreacion = fechaCreacion;
    }

    public static Cita programar(Long solicitudId, Long tatuadorId, LocalDate fecha, Jornada jornada,
                                 int numeroSesion, String notas, LocalDateTime ahora) {
        if (fecha == null || jornada == null) {
            throw new ReglaNegocioException("Seleccione la fecha y la jornada de la cita.");
        }
        if (fecha.isBefore(ahora.toLocalDate())) {
            throw new ReglaNegocioException("No es posible agendar citas en fechas pasadas.");
        }
        return new Cita(null, solicitudId, tatuadorId, fecha, jornada, EstadoCita.PROGRAMADA, numeroSesion, notas, ahora);
    }

    public void marcarRealizada() {
        if (estado != EstadoCita.PROGRAMADA) {
            throw new ReglaNegocioException("Solo las citas programadas pueden marcarse como realizadas.");
        }
        this.estado = EstadoCita.REALIZADA;
    }

    public void cancelar() {
        if (estado != EstadoCita.PROGRAMADA) {
            throw new ReglaNegocioException("Solo las citas programadas pueden cancelarse.");
        }
        this.estado = EstadoCita.CANCELADA;
    }

    public boolean ocupaAgenda() {
        return estado != EstadoCita.CANCELADA;
    }

    public LocalDateTime inicio() {
        return fecha.atTime(jornada.getHoraInicio());
    }

    public Long getId() { return id; }
    public Long getSolicitudId() { return solicitudId; }
    public Long getTatuadorId() { return tatuadorId; }
    public LocalDate getFecha() { return fecha; }
    public Jornada getJornada() { return jornada; }
    public EstadoCita getEstado() { return estado; }
    public int getNumeroSesion() { return numeroSesion; }
    public String getNotas() { return notas; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}
