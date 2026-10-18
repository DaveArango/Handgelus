package com.handgelus.domain.model;

import com.handgelus.domain.exception.ReglaNegocioException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cotización de una solicitud (F-04).
 * Conserva una "fotografía" de la tarifa usada al calcularla, de modo que los cambios
 * posteriores en la tabla de tarifas no la alteren (SWR-05 · RF-17).
 */
public class Cotizacion {

    private final Long id;
    private final Long solicitudId;
    private final BigDecimal valorCalculado;
    private BigDecimal valorFinal;
    private String observaciones;
    private EstadoCotizacion estado;
    // Fotografía de la tarifa aplicada
    private final BigDecimal tarifaValorBase;
    private final BigDecimal tarifaValorHora;
    private final int horasEstimadas;
    private final boolean tarifaFija;
    private final BigDecimal factorEstilo;
    private final BigDecimal costoEnvio;
    private final int sesionesEstimadas;
    // Trazabilidad
    private final LocalDateTime fechaEmision;
    private LocalDateTime fechaConfirmacion;
    private Long confirmadaPorId;
    private LocalDateTime fechaDecision;
    private boolean alertaEmitida;

    public Cotizacion(Long id, Long solicitudId, BigDecimal valorCalculado, BigDecimal valorFinal, String observaciones,
                      EstadoCotizacion estado, BigDecimal tarifaValorBase, BigDecimal tarifaValorHora,
                      int horasEstimadas, boolean tarifaFija, BigDecimal factorEstilo, BigDecimal costoEnvio,
                      int sesionesEstimadas, LocalDateTime fechaEmision, LocalDateTime fechaConfirmacion,
                      Long confirmadaPorId, LocalDateTime fechaDecision, boolean alertaEmitida) {
        this.id = id;
        this.solicitudId = solicitudId;
        this.valorCalculado = valorCalculado;
        this.valorFinal = valorFinal;
        this.observaciones = observaciones;
        this.estado = estado;
        this.tarifaValorBase = tarifaValorBase;
        this.tarifaValorHora = tarifaValorHora;
        this.horasEstimadas = horasEstimadas;
        this.tarifaFija = tarifaFija;
        this.factorEstilo = factorEstilo;
        this.costoEnvio = costoEnvio;
        this.sesionesEstimadas = sesionesEstimadas;
        this.fechaEmision = fechaEmision;
        this.fechaConfirmacion = fechaConfirmacion;
        this.confirmadaPorId = confirmadaPorId;
        this.fechaDecision = fechaDecision;
        this.alertaEmitida = alertaEmitida;
    }

    public static Cotizacion preliminar(Long solicitudId, ResultadoCotizacion r, LocalDateTime ahora) {
        return new Cotizacion(null, solicitudId, r.valorTotal(), null, null, EstadoCotizacion.PRELIMINAR,
                r.tarifaValorBase(), r.tarifaValorHora(), r.horasEstimadas(), r.tarifaFija(), r.factorEstilo(),
                r.costoEnvio(), r.sesionesEstimadas(), ahora, null, null, null, false);
    }

    /** RF-05 / SWR-10: el tatuador ajusta (o no) el valor y confirma la cotización definitiva. */
    public void confirmar(BigDecimal valorDefinitivo, String observaciones, Long tatuadorId, LocalDateTime ahora) {
        if (estado != EstadoCotizacion.PRELIMINAR) {
            throw new ReglaNegocioException("La cotización ya fue confirmada.");
        }
        BigDecimal valor = valorDefinitivo == null ? valorCalculado : valorDefinitivo;
        if (valor.signum() <= 0) {
            throw new ReglaNegocioException("El valor final de la cotización debe ser mayor a cero.");
        }
        this.valorFinal = valor;
        this.observaciones = observaciones;
        this.confirmadaPorId = tatuadorId;
        this.fechaConfirmacion = ahora;
        this.estado = EstadoCotizacion.CONFIRMADA;
    }

    public void aceptar(LocalDateTime ahora) {
        exigirConfirmada();
        this.estado = EstadoCotizacion.ACEPTADA;
        this.fechaDecision = ahora;
    }

    public void rechazar(LocalDateTime ahora) {
        exigirConfirmada();
        this.estado = EstadoCotizacion.RECHAZADA;
        this.fechaDecision = ahora;
    }

    private void exigirConfirmada() {
        if (estado != EstadoCotizacion.CONFIRMADA) {
            throw new ReglaNegocioException("La cotización aún no está lista para su decisión.");
        }
    }

    public void marcarAlertaEmitida() {
        this.alertaEmitida = true;
    }

    /** Valor vigente: el definitivo si ya fue confirmado, si no el calculado. */
    public BigDecimal valorVigente() {
        return valorFinal != null ? valorFinal : valorCalculado;
    }

    public Long getId() { return id; }
    public Long getSolicitudId() { return solicitudId; }
    public BigDecimal getValorCalculado() { return valorCalculado; }
    public BigDecimal getValorFinal() { return valorFinal; }
    public String getObservaciones() { return observaciones; }
    public EstadoCotizacion getEstado() { return estado; }
    public BigDecimal getTarifaValorBase() { return tarifaValorBase; }
    public BigDecimal getTarifaValorHora() { return tarifaValorHora; }
    public int getHorasEstimadas() { return horasEstimadas; }
    public boolean isTarifaFija() { return tarifaFija; }
    public BigDecimal getFactorEstilo() { return factorEstilo; }
    public BigDecimal getCostoEnvio() { return costoEnvio; }
    public int getSesionesEstimadas() { return sesionesEstimadas; }
    public LocalDateTime getFechaEmision() { return fechaEmision; }
    public LocalDateTime getFechaConfirmacion() { return fechaConfirmacion; }
    public Long getConfirmadaPorId() { return confirmadaPorId; }
    public LocalDateTime getFechaDecision() { return fechaDecision; }
    public boolean isAlertaEmitida() { return alertaEmitida; }
}
