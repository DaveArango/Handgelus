package com.handgelus.application.port.in.modelo;

import com.handgelus.domain.model.EstadoPagoServicio;
import com.handgelus.domain.model.Solicitud;

import java.math.BigDecimal;

/** Fila de la bandeja de solicitudes. */
public record SolicitudResumen(Solicitud solicitud, String clienteNombre, BigDecimal valor,
                               boolean cotizacionConfirmada, EstadoPagoServicio estadoPago) {
}
