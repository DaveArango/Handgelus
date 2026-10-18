package com.handgelus.domain.model;

import com.handgelus.domain.exception.ReglaNegocioException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Pago de abono, anticipo o saldo de un servicio (F-05 · RF-08/RF-09/RF-14). */
public class Pago {

    private final Long id;
    private final Long solicitudId;
    private final TipoPago tipo;
    private final BigDecimal monto;
    private EstadoPago estado;
    private final MetodoPago metodo;
    private String referencia;
    private final LocalDateTime fechaCreacion;
    private LocalDateTime fechaConfirmacion;
    private final Long registradoPorId;

    public Pago(Long id, Long solicitudId, TipoPago tipo, BigDecimal monto, EstadoPago estado, MetodoPago metodo,
                String referencia, LocalDateTime fechaCreacion, LocalDateTime fechaConfirmacion, Long registradoPorId) {
        this.id = id;
        this.solicitudId = solicitudId;
        this.tipo = tipo;
        this.monto = monto;
        this.estado = estado;
        this.metodo = metodo;
        this.referencia = referencia;
        this.fechaCreacion = fechaCreacion;
        this.fechaConfirmacion = fechaConfirmacion;
        this.registradoPorId = registradoPorId;
    }

    public static Pago pendiente(Long solicitudId, TipoPago tipo, BigDecimal monto, MetodoPago metodo,
                                 String referencia, Long registradoPorId, LocalDateTime ahora) {
        validarMonto(monto);
        return new Pago(null, solicitudId, tipo, monto, EstadoPago.PENDIENTE, metodo, referencia, ahora, null,
                registradoPorId);
    }

    public static Pago confirmadoManual(Long solicitudId, TipoPago tipo, BigDecimal monto, MetodoPago metodo,
                                        String referencia, Long registradoPorId, LocalDateTime ahora) {
        validarMonto(monto);
        return new Pago(null, solicitudId, tipo, monto, EstadoPago.CONFIRMADO, metodo, referencia, ahora, ahora,
                registradoPorId);
    }

    private static void validarMonto(BigDecimal monto) {
        if (monto == null || monto.signum() <= 0) {
            throw new ReglaNegocioException("El monto del pago debe ser mayor a cero.");
        }
    }

    public void confirmar(LocalDateTime ahora) {
        exigirPendiente();
        this.estado = EstadoPago.CONFIRMADO;
        this.fechaConfirmacion = ahora;
    }

    public void rechazar(LocalDateTime ahora) {
        exigirPendiente();
        this.estado = EstadoPago.RECHAZADO;
        this.fechaConfirmacion = ahora;
    }

    public void expirar(LocalDateTime ahora) {
        exigirPendiente();
        this.estado = EstadoPago.EXPIRADO;
        this.fechaConfirmacion = ahora;
    }

    private void exigirPendiente() {
        if (estado != EstadoPago.PENDIENTE) {
            throw new ReglaNegocioException("El pago ya fue procesado anteriormente.");
        }
    }

    public boolean estaConfirmado() {
        return estado == EstadoPago.CONFIRMADO;
    }

    public boolean estaPendiente() {
        return estado == EstadoPago.PENDIENTE;
    }

    public Long getId() { return id; }
    public Long getSolicitudId() { return solicitudId; }
    public TipoPago getTipo() { return tipo; }
    public BigDecimal getMonto() { return monto; }
    public EstadoPago getEstado() { return estado; }
    public MetodoPago getMetodo() { return metodo; }
    public String getReferencia() { return referencia; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaConfirmacion() { return fechaConfirmacion; }
    public Long getRegistradoPorId() { return registradoPorId; }
}
