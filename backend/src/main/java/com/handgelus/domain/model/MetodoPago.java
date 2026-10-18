package com.handgelus.domain.model;

public enum MetodoPago {
    TARJETA("Tarjeta"),
    PSE("PSE"),
    NEQUI("Nequi"),
    TRANSFERENCIA("Transferencia"),
    EFECTIVO("Efectivo");

    private final String etiqueta;

    MetodoPago(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Métodos que se procesan a través de la pasarela externa. */
    public boolean usaPasarela() {
        return this == TARJETA || this == PSE || this == NEQUI;
    }
}
