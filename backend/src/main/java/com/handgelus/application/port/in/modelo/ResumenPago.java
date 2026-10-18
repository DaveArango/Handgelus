package com.handgelus.application.port.in.modelo;

import com.handgelus.domain.model.EstadoPagoServicio;
import com.handgelus.domain.model.TipoPago;

import java.math.BigDecimal;

/** Estado financiero de un servicio: total, pagado, saldo y mínimo exigido. */
public record ResumenPago(BigDecimal valorTotal, BigDecimal pagado, BigDecimal saldo, EstadoPagoServicio estado,
                          TipoPago tipoPagoInicial, int porcentajeMinimo, BigDecimal montoMinimoInicial,
                          boolean pagoInicialConfirmado, boolean pagoPendiente) {
}
