package com.handgelus.application.port.in;

import com.handgelus.application.port.in.modelo.ClienteResumen;
import com.handgelus.application.port.in.modelo.HistorialCliente;
import com.handgelus.domain.model.Actor;
import com.handgelus.domain.model.Comprobante;

import java.util.List;

/** F-08 · Consultar historial de servicios (SWR-21). */
public interface HistorialUseCase {

    List<ClienteResumen> clientes(String busqueda, Actor actor);

    HistorialCliente historial(Long clienteId, Actor actor);

    Comprobante comprobante(Long comprobanteId, Actor actor);
}
