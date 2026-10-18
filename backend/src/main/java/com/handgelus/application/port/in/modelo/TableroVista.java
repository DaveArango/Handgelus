package com.handgelus.application.port.in.modelo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Indicadores del panel de gestión del estudio. */
public record TableroVista(BigDecimal ingresosMes,
                           BigDecimal saldosPendientes,
                           long serviciosConSaldo,
                           long solicitudesNuevas,
                           long cotizacionesPorConfirmar,
                           long pagosPendientes,
                           long obrasEnProduccion,
                           long alertasSinLeer,
                           Map<String, Long> solicitudesPorEstado,
                           List<CitaAgenda> proximasCitas,
                           List<SolicitudResumen> ultimasSolicitudes) {
}
