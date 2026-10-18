package com.handgelus.domain.model;

/** Estados de avance de una obra por comisión (RF-12 · SWR-18). */
public enum EstadoObra {
    EN_ESPERA("En espera"),
    EN_PRODUCCION("En producción"),
    TERMINADA("Terminada"),
    ENVIADA("Enviada");

    private final String etiqueta;

    EstadoObra(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
