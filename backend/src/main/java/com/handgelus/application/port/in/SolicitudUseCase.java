package com.handgelus.application.port.in;

import com.handgelus.application.port.in.modelo.SolicitudDetalle;
import com.handgelus.application.port.in.modelo.SolicitudResumen;
import com.handgelus.application.port.out.AlmacenamientoArchivosPort.ArchivoAlmacenado;
import com.handgelus.domain.model.Actor;
import com.handgelus.domain.model.CategoriaTamano;
import com.handgelus.domain.model.EstadoSolicitud;
import com.handgelus.domain.model.Estilo;
import com.handgelus.domain.model.TipoServicio;

import java.util.List;

/** F-03 · Registrar solicitud de servicio (CU-01). */
public interface SolicitudUseCase {

    SolicitudDetalle registrar(RegistrarSolicitudComando comando, Actor actor);

    List<SolicitudResumen> listar(EstadoSolicitud estado, TipoServicio tipo, String grupo, Actor actor);

    SolicitudDetalle detalle(Long solicitudId, Actor actor);

    SolicitudDetalle cancelar(Long solicitudId, String motivo, Actor actor);

    ArchivoAlmacenado archivo(String nombre, Actor actor);

    record RegistrarSolicitudComando(TipoServicio tipoServicio, String titulo, String descripcion,
                                     CategoriaTamano categoria, Estilo estilo, String zonaCuerpo,
                                     String direccionDestino, String ciudadDestino,
                                     ImagenAdjunta imagen) {
    }

    record ImagenAdjunta(String nombreOriginal, String contentType, byte[] contenido) {
    }
}
