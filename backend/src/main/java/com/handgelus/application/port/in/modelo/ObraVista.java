package com.handgelus.application.port.in.modelo;

import com.handgelus.domain.model.Obra;
import com.handgelus.domain.model.RegistroAvance;
import com.handgelus.domain.model.Solicitud;

/** Proyecto en curso (obra por comisión o tatuaje con sesiones) con su último avance. */
public record ObraVista(Solicitud solicitud, Obra obra, String clienteNombre, RegistroAvance ultimoAvance,
                        int totalAvances) {
}
