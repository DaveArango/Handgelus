package com.handgelus.application.port.in.modelo;

import com.handgelus.domain.model.Comprobante;
import com.handgelus.domain.model.EstadoPagoServicio;
import com.handgelus.domain.model.EstadoSolicitud;
import com.handgelus.domain.model.TipoServicio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Historial de servicios de un cliente (F-08 · SWR-21). */
public record HistorialCliente(UsuarioVista cliente, List<Servicio> servicios, BigDecimal totalPagado,
                               int totalServicios, int serviciosCompletados) {

    public record Servicio(Long solicitudId, TipoServicio tipoServicio, String titulo, LocalDateTime fecha,
                           BigDecimal valor, EstadoSolicitud estado, EstadoPagoServicio estadoPago,
                           BigDecimal pagado, List<Comprobante> comprobantes) {
    }
}
