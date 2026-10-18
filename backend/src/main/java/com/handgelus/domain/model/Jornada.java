package com.handgelus.domain.model;

import java.time.LocalTime;

/** Jornadas de trabajo del tatuador. Cada jornada admite una sola cita. */
public enum Jornada {
    MANANA("Mañana", LocalTime.of(9, 0), LocalTime.of(13, 0)),
    TARDE("Tarde", LocalTime.of(14, 0), LocalTime.of(18, 0));

    private final String etiqueta;
    private final LocalTime horaInicio;
    private final LocalTime horaFin;

    Jornada(String etiqueta, LocalTime horaInicio, LocalTime horaFin) {
        this.etiqueta = etiqueta;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }
}
