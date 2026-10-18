package com.handgelus.application.port.in;

import com.handgelus.application.port.in.modelo.CitaAgenda;
import com.handgelus.application.port.in.modelo.DisponibilidadDia;
import com.handgelus.application.port.in.modelo.SolicitudDetalle;
import com.handgelus.domain.model.Actor;
import com.handgelus.domain.model.Jornada;

import java.time.LocalDate;
import java.util.List;

/** F-06 · Agendar cita de tatuaje (CU-03). */
public interface AgendaUseCase {

    List<DisponibilidadDia> disponibilidad(Long tatuadorId, LocalDate desde, LocalDate hasta);

    List<DisponibilidadDia> sugerirFechas(Long tatuadorId, LocalDate desde, int cantidad);

    List<CitaAgenda> citas(LocalDate desde, LocalDate hasta, Long tatuadorId, Actor actor);

    SolicitudDetalle agendar(AgendarCitaComando comando, Actor actor);

    CitaAgenda marcarRealizada(Long citaId, Actor actor);

    CitaAgenda cancelarCita(Long citaId, Actor actor);

    /** RN-07: cierra el servicio de tatuaje cuando el pago total está completo. */
    SolicitudDetalle completarServicio(Long solicitudId, Actor actor);

    record AgendarCitaComando(Long solicitudId, Long tatuadorId, LocalDate fecha, Jornada jornada, String notas) {
    }
}
