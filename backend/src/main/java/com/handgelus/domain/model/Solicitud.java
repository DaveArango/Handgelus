package com.handgelus.domain.model;

import com.handgelus.domain.exception.ReglaNegocioException;

import java.time.LocalDateTime;

/**
 * Solicitud de servicio de tatuaje u obra por comisión (F-03 · RF-02/RF-03).
 * Concentra la máquina de estados DE-01.
 */
public class Solicitud {

    private final Long id;
    private final Long clienteId;
    private final TipoServicio tipoServicio;
    private String titulo;
    private String descripcion;
    private CategoriaTamano categoria;
    private Estilo estilo;
    // Tatuaje
    private String zonaCuerpo;
    // Comisión
    private String direccionDestino;
    private String ciudadDestino;
    // Común
    private String imagenReferencia;
    private EstadoSolicitud estado;
    private final LocalDateTime fechaCreacion;
    private LocalDateTime fechaCambioEstado;
    private String motivoCancelacion;

    public Solicitud(Long id, Long clienteId, TipoServicio tipoServicio, String titulo, String descripcion,
                     CategoriaTamano categoria, Estilo estilo, String zonaCuerpo, String direccionDestino,
                     String ciudadDestino, String imagenReferencia, EstadoSolicitud estado,
                     LocalDateTime fechaCreacion, LocalDateTime fechaCambioEstado, String motivoCancelacion) {
        this.id = id;
        this.clienteId = clienteId;
        this.tipoServicio = tipoServicio;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.estilo = estilo;
        this.zonaCuerpo = zonaCuerpo;
        this.direccionDestino = direccionDestino;
        this.ciudadDestino = ciudadDestino;
        this.imagenReferencia = imagenReferencia;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
        this.fechaCambioEstado = fechaCambioEstado;
        this.motivoCancelacion = motivoCancelacion;
    }

    /** Crea una solicitud de tatuaje validando los campos de RF-02 / SWR-07. */
    public static Solicitud nuevaTatuaje(Long clienteId, String titulo, String descripcion, CategoriaTamano categoria,
                                         Estilo estilo, String zonaCuerpo, String imagen, LocalDateTime ahora) {
        validarComunes(clienteId, titulo, descripcion, categoria);
        if (esVacio(zonaCuerpo)) {
            throw new ReglaNegocioException("SWR-07", "Debe indicar la zona del cuerpo donde irá el tatuaje.");
        }
        return new Solicitud(null, clienteId, TipoServicio.TATUAJE, titulo.trim(), descripcion.trim(), categoria,
                estilo, zonaCuerpo.trim(), null, null, imagen, EstadoSolicitud.RECIBIDA, ahora, ahora, null);
    }

    /** Crea una solicitud de obra por comisión validando RF-03 / SWR-08 (dirección completa). */
    public static Solicitud nuevaComision(Long clienteId, String titulo, String idea, CategoriaTamano formato,
                                          Estilo estilo, String direccion, String ciudad, String imagen,
                                          LocalDateTime ahora) {
        validarComunes(clienteId, titulo, idea, formato);
        if (esVacio(direccion) || direccion.trim().length() < 8 || esVacio(ciudad)) {
            throw new ReglaNegocioException("SWR-08",
                    "Debe indicar la dirección completa de destino (dirección y ciudad) para calcular el envío.");
        }
        return new Solicitud(null, clienteId, TipoServicio.COMISION, titulo.trim(), idea.trim(), formato, estilo,
                null, direccion.trim(), ciudad.trim(), imagen, EstadoSolicitud.RECIBIDA, ahora, ahora, null);
    }

    private static void validarComunes(Long clienteId, String titulo, String descripcion, CategoriaTamano categoria) {
        if (clienteId == null) {
            throw new ReglaNegocioException("SWR-06", "La solicitud debe pertenecer a un cliente registrado.");
        }
        if (esVacio(titulo)) {
            throw new ReglaNegocioException("Asigne un nombre a su proyecto.");
        }
        if (esVacio(descripcion) || descripcion.trim().length() < 10) {
            throw new ReglaNegocioException("Describa su idea con al menos 10 caracteres.");
        }
        if (categoria == null) {
            throw new ReglaNegocioException("RN-08", "Seleccione un tamaño de la lista establecida.");
        }
    }

    private static boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    /** Aplica una transición validando la máquina de estados DE-01. */
    public void cambiarEstado(EstadoSolicitud destino, LocalDateTime ahora) {
        if (!estado.puedeCambiarA(destino)) {
            throw new ReglaNegocioException("La solicitud no puede pasar de '" + estado.getEtiqueta()
                    + "' a '" + destino.getEtiqueta() + "'.");
        }
        this.estado = destino;
        this.fechaCambioEstado = ahora;
    }

    public void cancelar(String motivo, LocalDateTime ahora) {
        cambiarEstado(EstadoSolicitud.CANCELADA, ahora);
        this.motivoCancelacion = motivo;
    }

    public void exigirEstado(EstadoSolicitud... permitidos) {
        for (EstadoSolicitud e : permitidos) {
            if (estado == e) {
                return;
            }
        }
        throw new ReglaNegocioException("La acción no está disponible en el estado '" + estado.getEtiqueta() + "'.");
    }

    public boolean esTatuaje() {
        return tipoServicio == TipoServicio.TATUAJE;
    }

    public boolean esComision() {
        return tipoServicio == TipoServicio.COMISION;
    }

    public Long getId() { return id; }
    public Long getClienteId() { return clienteId; }
    public TipoServicio getTipoServicio() { return tipoServicio; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public CategoriaTamano getCategoria() { return categoria; }
    public Estilo getEstilo() { return estilo; }
    public String getZonaCuerpo() { return zonaCuerpo; }
    public String getDireccionDestino() { return direccionDestino; }
    public String getCiudadDestino() { return ciudadDestino; }
    public String getImagenReferencia() { return imagenReferencia; }
    public EstadoSolicitud getEstado() { return estado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaCambioEstado() { return fechaCambioEstado; }
    public String getMotivoCancelacion() { return motivoCancelacion; }
}
