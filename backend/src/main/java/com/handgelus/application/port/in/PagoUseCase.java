package com.handgelus.application.port.in;

import com.handgelus.application.port.in.modelo.CheckoutVista;
import com.handgelus.application.port.in.modelo.PagoVista;
import com.handgelus.application.port.in.modelo.SolicitudDetalle;
import com.handgelus.domain.model.Actor;
import com.handgelus.domain.model.EstadoPago;
import com.handgelus.domain.model.MetodoPago;
import com.handgelus.domain.model.Pago;

import java.math.BigDecimal;
import java.util.List;

/** F-05 · Registrar pago de abono o anticipo (RF-08, RF-09, RF-14). */
public interface PagoUseCase {

    /** El cliente inicia el pago (abono/anticipo o saldo) a través de la pasarela. */
    CheckoutVista iniciarPago(Long solicitudId, BigDecimal monto, MetodoPago metodo, Actor actor);

    CheckoutVista consultarCheckout(String referencia, Actor actor);

    /** Webhook de la pasarela: confirma o rechaza la transacción (SWR-13). */
    Pago procesarRespuestaPasarela(String referencia, boolean aprobado, String firma);

    /** La asistente registra un pago recibido fuera de la pasarela (efectivo, transferencia). */
    SolicitudDetalle registrarPagoManual(Long solicitudId, BigDecimal monto, MetodoPago metodo, Actor actor);

    List<PagoVista> listar(EstadoPago estado, Actor actor);

    /** SWR-14: revierte a 'Cotización aceptada' los pagos no confirmados en 48 h. */
    int expirarPagosVencidos();
}
