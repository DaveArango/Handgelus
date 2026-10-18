package com.handgelus.domain.model;

import com.handgelus.domain.exception.ReglaNegocioException;

import java.time.LocalDateTime;

/** Obra artística por comisión y su ciclo de producción (F-07 · RF-12). */
public class Obra {

    private final Long id;
    private final Long solicitudId;
    private EstadoObra estado;
    private int progreso;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaTerminada;
    private LocalDateTime fechaEnvio;
    private String guiaEnvio;

    public Obra(Long id, Long solicitudId, EstadoObra estado, int progreso, LocalDateTime fechaInicio,
                LocalDateTime fechaTerminada, LocalDateTime fechaEnvio, String guiaEnvio) {
        this.id = id;
        this.solicitudId = solicitudId;
        this.estado = estado;
        this.progreso = progreso;
        this.fechaInicio = fechaInicio;
        this.fechaTerminada = fechaTerminada;
        this.fechaEnvio = fechaEnvio;
        this.guiaEnvio = guiaEnvio;
    }

    public static Obra enEspera(Long solicitudId) {
        return new Obra(null, solicitudId, EstadoObra.EN_ESPERA, 0, null, null, null, null);
    }

    /**
     * Cambia el estado respetando el orden: en espera → en producción → terminada → enviada.
     * La regla del saldo (SWR-20) la valida el caso de uso porque requiere consultar los pagos.
     */
    public void cambiarEstado(EstadoObra destino, LocalDateTime ahora) {
        if (destino.ordinal() != estado.ordinal() + 1) {
            throw new ReglaNegocioException("La obra no puede pasar de '" + estado.getEtiqueta()
                    + "' a '" + destino.getEtiqueta() + "'. Los estados avanzan en orden.");
        }
        this.estado = destino;
        switch (destino) {
            case EN_PRODUCCION -> fechaInicio = ahora;
            case TERMINADA -> {
                fechaTerminada = ahora;
                progreso = 100;
            }
            case ENVIADA -> fechaEnvio = ahora;
            default -> { }
        }
    }

    public void actualizarProgreso(int nuevoProgreso) {
        if (nuevoProgreso < 0 || nuevoProgreso > 100) {
            throw new ReglaNegocioException("El progreso debe estar entre 0 y 100.");
        }
        if (estado == EstadoObra.EN_ESPERA && nuevoProgreso > 0) {
            throw new ReglaNegocioException("RN-10", "Inicie la producción antes de registrar avance.");
        }
        this.progreso = nuevoProgreso;
    }

    public void registrarGuia(String guia) {
        this.guiaEnvio = guia;
    }

    public Long getId() { return id; }
    public Long getSolicitudId() { return solicitudId; }
    public EstadoObra getEstado() { return estado; }
    public int getProgreso() { return progreso; }
    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public LocalDateTime getFechaTerminada() { return fechaTerminada; }
    public LocalDateTime getFechaEnvio() { return fechaEnvio; }
    public String getGuiaEnvio() { return guiaEnvio; }
}
