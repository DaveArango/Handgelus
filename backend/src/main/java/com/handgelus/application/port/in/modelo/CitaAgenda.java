package com.handgelus.application.port.in.modelo;

import com.handgelus.domain.model.Cita;
import com.handgelus.domain.model.EstadoSolicitud;

/** Cita con los datos necesarios para pintarla en la agenda. */
public record CitaAgenda(Cita cita, String clienteNombre, String tituloSolicitud, String tatuadorNombre,
                         EstadoSolicitud estadoSolicitud) {
}
