package com.handgelus.application.port.in.modelo;

import com.handgelus.domain.model.Jornada;

import java.time.LocalDate;
import java.util.List;

/** Jornadas libres de un día para un tatuador. */
public record DisponibilidadDia(LocalDate fecha, List<Jornada> jornadasLibres, int citasAgendadas, boolean disponible) {
}
