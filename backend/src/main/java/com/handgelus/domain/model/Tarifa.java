package com.handgelus.domain.model;

import com.handgelus.domain.exception.ReglaNegocioException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tarifa base por tipo de servicio y tamaño (F-02 · SWR-04).
 * Si es de tarifa fija (RN-03, tatuajes pequeños) el valor base es el precio completo;
 * en otro caso se suma el valor por hora multiplicado por las horas estimadas.
 */
public class Tarifa {

    private final Long id;
    private final TipoServicio tipoServicio;
    private final CategoriaTamano categoria;
    private BigDecimal valorBase;
    private BigDecimal valorHora;
    private int horasEstimadas;
    private boolean tarifaFija;
    private String descripcion;
    private LocalDateTime fechaActualizacion;

    public Tarifa(Long id, TipoServicio tipoServicio, CategoriaTamano categoria, BigDecimal valorBase,
                  BigDecimal valorHora, int horasEstimadas, boolean tarifaFija, String descripcion,
                  LocalDateTime fechaActualizacion) {
        this.id = id;
        this.tipoServicio = tipoServicio;
        this.categoria = categoria;
        this.valorBase = valorBase;
        this.valorHora = valorHora;
        this.horasEstimadas = horasEstimadas;
        this.tarifaFija = tarifaFija;
        this.descripcion = descripcion;
        this.fechaActualizacion = fechaActualizacion;
    }

    public static Tarifa nueva(TipoServicio tipo, CategoriaTamano categoria, BigDecimal valorBase,
                               BigDecimal valorHora, int horas, boolean fija, String descripcion,
                               LocalDateTime ahora) {
        Tarifa t = new Tarifa(null, tipo, categoria, BigDecimal.ZERO, BigDecimal.ZERO, 0, false, null, ahora);
        t.actualizar(valorBase, valorHora, horas, fija, descripcion, ahora);
        return t;
    }

    public void actualizar(BigDecimal valorBase, BigDecimal valorHora, int horas, boolean fija,
                           String descripcion, LocalDateTime ahora) {
        if (valorBase == null || valorBase.signum() <= 0) {
            throw new ReglaNegocioException("El valor base debe ser mayor a cero.");
        }
        if (valorHora == null || valorHora.signum() < 0) {
            throw new ReglaNegocioException("El valor por hora no puede ser negativo.");
        }
        if (horas < 0 || horas > 80) {
            throw new ReglaNegocioException("Las horas estimadas deben estar entre 0 y 80.");
        }
        if (fija && tipoServicio == TipoServicio.COMISION) {
            throw new ReglaNegocioException("RN-09",
                    "Las obras por comisión se cotizan por tiempo, tamaño y envío; no admiten tarifa fija.");
        }
        this.valorBase = valorBase;
        this.valorHora = valorHora;
        this.horasEstimadas = horas;
        this.tarifaFija = fija;
        this.descripcion = descripcion;
        this.fechaActualizacion = ahora;
    }

    public Long getId() { return id; }
    public TipoServicio getTipoServicio() { return tipoServicio; }
    public CategoriaTamano getCategoria() { return categoria; }
    public BigDecimal getValorBase() { return valorBase; }
    public BigDecimal getValorHora() { return valorHora; }
    public int getHorasEstimadas() { return horasEstimadas; }
    public boolean isTarifaFija() { return tarifaFija; }
    public String getDescripcion() { return descripcion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
}
