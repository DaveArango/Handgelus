package com.handgelus.domain.model;

/** Roles del sistema (F-01 · SWR-01/02). */
public enum Rol {
    CLIENTE("Cliente"),
    ASISTENTE("Asistente"),
    TATUADOR("Tatuador / Artista"),
    ADMINISTRADOR("Administrador");

    private final String etiqueta;

    Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public boolean esPersonalInterno() {
        return this != CLIENTE;
    }
}
