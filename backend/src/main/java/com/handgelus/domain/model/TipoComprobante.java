package com.handgelus.domain.model;

public enum TipoComprobante {
    COTIZACION_ACEPTADA("Cotización aceptada"),
    PAGO("Comprobante de pago"),
    CITA_AGENDADA("Cita confirmada");

    private final String etiqueta;

    TipoComprobante(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
