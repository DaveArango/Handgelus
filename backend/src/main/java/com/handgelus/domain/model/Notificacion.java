package com.handgelus.domain.model;

import java.time.LocalDateTime;

/** Registro de una notificación enviada (correo al cliente o alerta interna). */
public class Notificacion {

    private final Long id;
    private final TipoNotificacion tipo;
    private final String destinatario;
    private final String asunto;
    private final String cuerpo;
    private final Long solicitudId;
    private final LocalDateTime fechaEnvio;
    private boolean leida;

    public Notificacion(Long id, TipoNotificacion tipo, String destinatario, String asunto, String cuerpo,
                        Long solicitudId, LocalDateTime fechaEnvio, boolean leida) {
        this.id = id;
        this.tipo = tipo;
        this.destinatario = destinatario;
        this.asunto = asunto;
        this.cuerpo = cuerpo;
        this.solicitudId = solicitudId;
        this.fechaEnvio = fechaEnvio;
        this.leida = leida;
    }

    public static Notificacion nueva(TipoNotificacion tipo, String destinatario, String asunto, String cuerpo,
                                     Long solicitudId, LocalDateTime ahora) {
        return new Notificacion(null, tipo, destinatario, asunto, cuerpo, solicitudId, ahora, false);
    }

    public void marcarLeida() {
        this.leida = true;
    }

    public Long getId() { return id; }
    public TipoNotificacion getTipo() { return tipo; }
    public String getDestinatario() { return destinatario; }
    public String getAsunto() { return asunto; }
    public String getCuerpo() { return cuerpo; }
    public Long getSolicitudId() { return solicitudId; }
    public LocalDateTime getFechaEnvio() { return fechaEnvio; }
    public boolean isLeida() { return leida; }
}
