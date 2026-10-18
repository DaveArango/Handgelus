package com.handgelus.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Comprobante digital de cotización aceptada, pago o cita (RF-11 · Tabla 5). */
public class Comprobante {

    private final Long id;
    private final Long solicitudId;
    private final Long clienteId;
    private final TipoComprobante tipo;
    private final String codigo;
    private final String detalle;
    private final BigDecimal valor;
    private final LocalDateTime fechaEmision;

    public Comprobante(Long id, Long solicitudId, Long clienteId, TipoComprobante tipo, String codigo,
                       String detalle, BigDecimal valor, LocalDateTime fechaEmision) {
        this.id = id;
        this.solicitudId = solicitudId;
        this.clienteId = clienteId;
        this.tipo = tipo;
        this.codigo = codigo;
        this.detalle = detalle;
        this.valor = valor;
        this.fechaEmision = fechaEmision;
    }

    public static Comprobante emitir(Long solicitudId, Long clienteId, TipoComprobante tipo, String codigo,
                                     String detalle, BigDecimal valor, LocalDateTime ahora) {
        return new Comprobante(null, solicitudId, clienteId, tipo, codigo, detalle, valor, ahora);
    }

    public Long getId() { return id; }
    public Long getSolicitudId() { return solicitudId; }
    public Long getClienteId() { return clienteId; }
    public TipoComprobante getTipo() { return tipo; }
    public String getCodigo() { return codigo; }
    public String getDetalle() { return detalle; }
    public BigDecimal getValor() { return valor; }
    public LocalDateTime getFechaEmision() { return fechaEmision; }
}
