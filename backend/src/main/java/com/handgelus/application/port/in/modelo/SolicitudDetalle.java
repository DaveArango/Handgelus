package com.handgelus.application.port.in.modelo;

import com.handgelus.domain.model.Comprobante;
import com.handgelus.domain.model.Cotizacion;
import com.handgelus.domain.model.Obra;
import com.handgelus.domain.model.Pago;
import com.handgelus.domain.model.RegistroAvance;
import com.handgelus.domain.model.Solicitud;

import java.util.List;

/** Vista completa de una solicitud con todo su ciclo de servicio. */
public record SolicitudDetalle(Solicitud solicitud,
                               UsuarioVista cliente,
                               Cotizacion cotizacion,
                               ResumenPago resumenPago,
                               List<Pago> pagos,
                               List<CitaAgenda> citas,
                               Obra obra,
                               List<RegistroAvance> avances,
                               List<Comprobante> comprobantes) {
}
