package com.handgelus.domain.model;

import java.math.BigDecimal;

/**
 * Parámetros de las reglas de negocio. Se leen de application.properties en el
 * adaptador de configuración y se inyectan en los servicios del dominio.
 */
public record ParametrosNegocio(
        int porcentajeAbonoTatuaje,        // RN-02
        int porcentajeAnticipoComision,    // RN-10
        int horasExpiracionPago,           // RF-09
        int horasAlertaCotizacion,         // CU-02 4.a
        int maxCitasPorDia,                // RF-10
        long imagenMaxBytes,               // RF-02
        String ciudadLocal,                // RN-09
        BigDecimal costoEnvioLocal,        // RN-09
        BigDecimal costoEnvioNacional,     // RN-09
        int horasPorSesion                 // RN-04
) {
    public static ParametrosNegocio porDefecto() {
        return new ParametrosNegocio(30, 50, 48, 72, 2, 5L * 1024 * 1024, "Armenia",
                new BigDecimal("12000"), new BigDecimal("28000"), 4);
    }
}
