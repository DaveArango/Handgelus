package com.handgelus.domain.model;

import java.math.BigDecimal;

/**
 * Estilo artístico solicitado. El factor representa la complejidad del trabajo
 * y se aplica a la cotización preliminar (RF-04 · "tamaño, tipo de trabajo y complejidad").
 */
public enum Estilo {
    LINEA_FINA("Línea fina", "1.00"),
    BLACKWORK("Blackwork", "1.10"),
    MICRORREALISMO("Microrrealismo", "1.35"),
    REALISMO_SOMBRAS("Realismo en sombras", "1.30"),
    ABSTRACTO("Abstracto", "1.05"),
    BOTANICO("Botánico", "1.10"),
    LETTERING("Lettering de autor", "0.95"),
    ILUSTRACION("Ilustración / óleo", "1.15");

    private final String etiqueta;
    private final BigDecimal factor;

    Estilo(String etiqueta, String factor) {
        this.etiqueta = etiqueta;
        this.factor = new BigDecimal(factor);
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public BigDecimal getFactor() {
        return factor;
    }
}
