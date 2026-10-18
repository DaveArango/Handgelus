package com.handgelus.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Máquina de estados del objeto Solicitud (DE-01 / Tabla 6 EST-01..EST-08).
 * PAGO_EN_PROCESO representa el intervalo entre el inicio del pago y la confirmación
 * de la pasarela (RF-09: si no se confirma en 48 h vuelve a COTIZACION_ACEPTADA).
 */
public enum EstadoSolicitud {
    RECIBIDA("Recibida"),
    COTIZACION_GENERADA("Cotización generada"),
    COTIZACION_ACEPTADA("Cotización aceptada"),
    PAGO_EN_PROCESO("Pago en proceso"),
    PAGO_PARCIAL_CONFIRMADO("Pago parcial confirmado"),
    CITA_AGENDADA("Cita agendada"),
    EN_PRODUCCION("Obra en producción"),
    TERMINADA("Obra terminada"),
    ENVIADA("Obra enviada"),
    SERVICIO_COMPLETADO("Servicio completado"),
    CANCELADA("Cancelada");

    private final String etiqueta;

    EstadoSolicitud(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public Set<EstadoSolicitud> siguientesPermitidos() {
        return switch (this) {
            case RECIBIDA -> EnumSet.of(COTIZACION_GENERADA, CANCELADA);
            case COTIZACION_GENERADA -> EnumSet.of(COTIZACION_ACEPTADA, CANCELADA);
            case COTIZACION_ACEPTADA -> EnumSet.of(PAGO_EN_PROCESO, PAGO_PARCIAL_CONFIRMADO, CANCELADA);
            case PAGO_EN_PROCESO -> EnumSet.of(PAGO_PARCIAL_CONFIRMADO, COTIZACION_ACEPTADA, CANCELADA);
            case PAGO_PARCIAL_CONFIRMADO -> EnumSet.of(CITA_AGENDADA, EN_PRODUCCION, CANCELADA);
            // Volver a PAGO_PARCIAL_CONFIRMADO permite reprogramar cuando se cancelan todas las citas
            case CITA_AGENDADA -> EnumSet.of(SERVICIO_COMPLETADO, PAGO_PARCIAL_CONFIRMADO, CANCELADA);
            case EN_PRODUCCION -> EnumSet.of(TERMINADA);
            case TERMINADA -> EnumSet.of(ENVIADA);
            case ENVIADA, SERVICIO_COMPLETADO, CANCELADA -> EnumSet.noneOf(EstadoSolicitud.class);
        };
    }

    public boolean puedeCambiarA(EstadoSolicitud destino) {
        return siguientesPermitidos().contains(destino);
    }

    public boolean esFinal() {
        return this == ENVIADA || this == SERVICIO_COMPLETADO || this == CANCELADA;
    }
}
