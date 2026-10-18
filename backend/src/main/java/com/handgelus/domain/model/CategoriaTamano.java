package com.handgelus.domain.model;

/**
 * Lista preestablecida de tamaños (RF-02) y formatos estandarizados de obra (RN-08).
 */
public enum CategoriaTamano {
    PEQUENO("Pequeño · hasta 7 cm", "Formato A5 · 15 × 21 cm"),
    MEDIANO("Mediano · 8 a 15 cm", "Formato A4 · 21 × 30 cm"),
    GRANDE("Grande · 16 a 25 cm", "Formato A3 · 30 × 42 cm"),
    EXTRA_GRANDE("Extra grande · más de 25 cm", "Formato A2 · 42 × 59 cm");

    private final String etiquetaTatuaje;
    private final String etiquetaComision;

    CategoriaTamano(String etiquetaTatuaje, String etiquetaComision) {
        this.etiquetaTatuaje = etiquetaTatuaje;
        this.etiquetaComision = etiquetaComision;
    }

    public String getEtiquetaTatuaje() {
        return etiquetaTatuaje;
    }

    public String getEtiquetaComision() {
        return etiquetaComision;
    }

    public String etiquetaPara(TipoServicio tipo) {
        return tipo == TipoServicio.TATUAJE ? etiquetaTatuaje : etiquetaComision;
    }
}
