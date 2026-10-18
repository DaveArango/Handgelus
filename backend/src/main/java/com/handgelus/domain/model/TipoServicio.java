package com.handgelus.domain.model;

public enum TipoServicio {
    TATUAJE("Tatuaje de autor"),
    COMISION("Obra por comisión");

    private final String etiqueta;

    TipoServicio(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
