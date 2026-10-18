package com.handgelus.domain.model;

import java.math.BigDecimal;

/** Resultado del cálculo automático de una cotización preliminar (RF-04). */
public record ResultadoCotizacion(
        BigDecimal valorTotal,
        BigDecimal subtotalTrabajo,
        BigDecimal tarifaValorBase,
        BigDecimal tarifaValorHora,
        int horasEstimadas,
        boolean tarifaFija,
        BigDecimal factorEstilo,
        BigDecimal costoEnvio,
        int sesionesEstimadas
) {
}
