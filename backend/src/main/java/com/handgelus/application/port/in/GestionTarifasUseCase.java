package com.handgelus.application.port.in;

import com.handgelus.domain.model.Actor;
import com.handgelus.domain.model.CategoriaTamano;
import com.handgelus.domain.model.Tarifa;
import com.handgelus.domain.model.TipoServicio;

import java.math.BigDecimal;
import java.util.List;

/** F-02 · Configurar tabla de tarifas (SWR-04 / SWR-05). */
public interface GestionTarifasUseCase {

    List<Tarifa> listar();

    /** Registra o actualiza la tarifa de un tipo de servicio y tamaño. */
    Tarifa guardar(TarifaComando comando, Actor actor);

    record TarifaComando(TipoServicio tipoServicio, CategoriaTamano categoria, BigDecimal valorBase,
                         BigDecimal valorHora, int horasEstimadas, boolean tarifaFija, String descripcion) {
    }
}
