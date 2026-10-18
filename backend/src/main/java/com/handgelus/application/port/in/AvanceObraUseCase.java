package com.handgelus.application.port.in;

import com.handgelus.application.port.in.SolicitudUseCase.ImagenAdjunta;
import com.handgelus.application.port.in.modelo.ObraVista;
import com.handgelus.application.port.in.modelo.SolicitudDetalle;
import com.handgelus.domain.model.Actor;
import com.handgelus.domain.model.EstadoObra;

import java.util.List;

/** F-07 · Actualizar avance de obra (SWR-18, SWR-19, SWR-20). */
public interface AvanceObraUseCase {

    SolicitudDetalle cambiarEstadoObra(Long solicitudId, EstadoObra estado, String guiaEnvio, Actor actor);

    SolicitudDetalle registrarAvance(RegistrarAvanceComando comando, Actor actor);

    /** Proyectos en curso: comisiones activas y tatuajes con cita. */
    List<ObraVista> proyectosEnCurso(Actor actor);

    record RegistrarAvanceComando(Long solicitudId, String titulo, String descripcion, Integer progreso,
                                  ImagenAdjunta imagen) {
    }
}
