package com.handgelus.application.port.in;

import com.handgelus.application.port.in.modelo.SolicitudDetalle;
import com.handgelus.domain.model.Actor;
import com.handgelus.domain.model.CategoriaTamano;
import com.handgelus.domain.model.Estilo;
import com.handgelus.domain.model.ResultadoCotizacion;
import com.handgelus.domain.model.TipoServicio;

import java.math.BigDecimal;

/** F-04 · Generar cotización (CU-02). */
public interface CotizacionUseCase {

    /** Estimación referencial previa al envío de la solicitud (RF-04). */
    ResultadoCotizacion estimar(TipoServicio tipo, CategoriaTamano categoria, Estilo estilo, String ciudadDestino);

    /** SWR-10: el tatuador/artista ajusta y confirma la cotización; se notifica al cliente (SWR-11). */
    SolicitudDetalle confirmar(Long solicitudId, BigDecimal valorFinal, String observaciones, Actor actor);

    /** RF-07: el cliente acepta o rechaza la cotización. */
    SolicitudDetalle decidir(Long solicitudId, boolean acepta, Actor actor);

    /** CU-02 4.a: alerta interna por cotizaciones sin confirmar en 72 h. */
    int alertarCotizacionesSinConfirmar();
}
