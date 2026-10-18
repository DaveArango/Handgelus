package com.handgelus.domain.model;

public enum TipoPago {
    ABONO("Abono"),
    ANTICIPO("Anticipo"),
    SALDO("Saldo restante");

    private final String etiqueta;

    TipoPago(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public boolean esPagoInicial() {
        return this == ABONO || this == ANTICIPO;
    }
}
