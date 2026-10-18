package com.handgelus.application.port.in.modelo;

import com.handgelus.domain.model.Pago;
import com.handgelus.domain.model.TipoServicio;

public record PagoVista(Pago pago, String clienteNombre, String tituloSolicitud, TipoServicio tipoServicio) {
}
