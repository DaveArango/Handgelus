package com.handgelus.application.port.in;

import com.handgelus.application.port.in.modelo.TableroVista;
import com.handgelus.domain.model.Actor;
import com.handgelus.domain.model.Notificacion;
import com.handgelus.domain.model.TipoNotificacion;

import java.util.List;

/** Panel del estudio: indicadores y bandeja de notificaciones. */
public interface TableroUseCase {

    TableroVista tablero(Actor actor);

    List<Notificacion> notificaciones(TipoNotificacion tipo, Actor actor);

    void marcarNotificacionLeida(Long id, Actor actor);
}
