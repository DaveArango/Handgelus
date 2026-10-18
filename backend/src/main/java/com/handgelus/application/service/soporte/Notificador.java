package com.handgelus.application.service.soporte;

import com.handgelus.application.port.out.EnvioCorreoPort;
import com.handgelus.application.port.out.NotificacionRepositoryPort;
import com.handgelus.domain.model.Notificacion;
import com.handgelus.domain.model.TipoNotificacion;
import com.handgelus.domain.model.Usuario;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Envía correos al cliente y alertas internas, dejando trazabilidad en la base de datos.
 * El envío es inmediato al confirmar la acción, lo que cumple el máximo de 5 minutos de RF-06.
 */
public class Notificador {

    public static final String DESTINO_EQUIPO = "equipo@handgelus.co";

    private final NotificacionRepositoryPort notificaciones;
    private final EnvioCorreoPort correo;
    private final Clock reloj;

    public Notificador(NotificacionRepositoryPort notificaciones, EnvioCorreoPort correo, Clock reloj) {
        this.notificaciones = notificaciones;
        this.correo = correo;
        this.reloj = reloj;
    }

    public void correoCliente(Usuario cliente, String asunto, String cuerpo, Long solicitudId) {
        String cuerpoCompleto = "Hola, " + cliente.getNombre() + ":\n\n" + cuerpo
                + "\n\nCon gratitud,\nAtelier Handgelus · Arte por comisión y tatuaje de autor";
        notificaciones.guardar(Notificacion.nueva(TipoNotificacion.CORREO_CLIENTE, cliente.getCorreo(), asunto,
                cuerpoCompleto, solicitudId, LocalDateTime.now(reloj)));
        correo.enviar(cliente.getCorreo(), asunto, cuerpoCompleto);
    }

    public void alertaInterna(String asunto, String cuerpo, Long solicitudId) {
        notificaciones.guardar(Notificacion.nueva(TipoNotificacion.ALERTA_INTERNA, DESTINO_EQUIPO, asunto, cuerpo,
                solicitudId, LocalDateTime.now(reloj)));
    }
}
