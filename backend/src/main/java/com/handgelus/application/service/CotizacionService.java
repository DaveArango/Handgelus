package com.handgelus.application.service;

import com.handgelus.application.port.in.CotizacionUseCase;
import com.handgelus.application.port.in.modelo.SolicitudDetalle;
import com.handgelus.application.port.out.CotizacionRepositoryPort;
import com.handgelus.application.port.out.SolicitudRepositoryPort;
import com.handgelus.application.port.out.TarifaRepositoryPort;
import com.handgelus.application.service.soporte.EmisorComprobantes;
import com.handgelus.application.service.soporte.EnsambladorDetalle;
import com.handgelus.application.service.soporte.Notificador;
import com.handgelus.domain.exception.ReglaNegocioException;
import com.handgelus.domain.model.Actor;
import com.handgelus.domain.model.CategoriaTamano;
import com.handgelus.domain.model.Cotizacion;
import com.handgelus.domain.model.EstadoSolicitud;
import com.handgelus.domain.model.Estilo;
import com.handgelus.domain.model.ParametrosNegocio;
import com.handgelus.domain.model.ResultadoCotizacion;
import com.handgelus.domain.model.Rol;
import com.handgelus.domain.model.Solicitud;
import com.handgelus.domain.model.TipoComprobante;
import com.handgelus.domain.model.TipoServicio;
import com.handgelus.domain.model.Usuario;
import com.handgelus.domain.service.CalculadoraCotizacion;
import com.handgelus.domain.service.ReglasPago;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

/** F-04 · Generar cotización (CU-02 · RF-04..RF-07 · SWR-09/10/11). */
public class CotizacionService implements CotizacionUseCase {

    private final SolicitudRepositoryPort solicitudes;
    private final CotizacionRepositoryPort cotizaciones;
    private final TarifaRepositoryPort tarifas;
    private final CalculadoraCotizacion calculadora;
    private final ReglasPago reglasPago;
    private final EnsambladorDetalle ensamblador;
    private final EmisorComprobantes emisor;
    private final Notificador notificador;
    private final ParametrosNegocio parametros;
    private final Clock reloj;

    public CotizacionService(SolicitudRepositoryPort solicitudes, CotizacionRepositoryPort cotizaciones,
                             TarifaRepositoryPort tarifas, CalculadoraCotizacion calculadora, ReglasPago reglasPago,
                             EnsambladorDetalle ensamblador, EmisorComprobantes emisor, Notificador notificador,
                             ParametrosNegocio parametros, Clock reloj) {
        this.solicitudes = solicitudes;
        this.cotizaciones = cotizaciones;
        this.tarifas = tarifas;
        this.calculadora = calculadora;
        this.reglasPago = reglasPago;
        this.ensamblador = ensamblador;
        this.emisor = emisor;
        this.notificador = notificador;
        this.parametros = parametros;
        this.reloj = reloj;
    }

    @Override
    public ResultadoCotizacion estimar(TipoServicio tipo, CategoriaTamano categoria, Estilo estilo, String ciudad) {
        if (tipo == null || categoria == null) {
            throw new ReglaNegocioException("Seleccione el tipo de servicio y el tamaño para estimar el valor.");
        }
        return calculadora.calcular(tarifas.buscarPorTipoYCategoria(tipo, categoria).orElse(null), estilo, ciudad);
    }

    @Override
    public SolicitudDetalle confirmar(Long solicitudId, BigDecimal valorFinal, String observaciones, Actor actor) {
        // RF-05: solo el tatuador/artista puede modificar y confirmar el valor final
        actor.exigir(Rol.TATUADOR);
        Solicitud solicitud = ensamblador.solicitud(solicitudId);
        solicitud.exigirEstado(EstadoSolicitud.RECIBIDA);
        LocalDateTime ahora = LocalDateTime.now(reloj);

        Cotizacion cotizacion = ensamblador.cotizacion(solicitudId);
        cotizacion.confirmar(valorFinal, observaciones, actor.id(), ahora);
        cotizaciones.guardar(cotizacion);
        solicitud.cambiarEstado(EstadoSolicitud.COTIZACION_GENERADA, ahora);
        solicitudes.guardar(solicitud);

        // RF-06 / SWR-11: notificación inmediata al cliente
        Usuario cliente = ensamblador.usuario(solicitud.getClienteId());
        BigDecimal minimo = reglasPago.montoMinimoInicial(cotizacion.getValorFinal(), solicitud.getTipoServicio());
        StringBuilder cuerpo = new StringBuilder()
                .append("Tu cotización para \"").append(solicitud.getTitulo()).append("\" está lista para revisión.\n\n")
                .append("Valor del servicio: $").append(ReglasPago.formato(cotizacion.getValorFinal())).append(" COP\n");
        if (solicitud.esTatuaje() && cotizacion.getSesionesEstimadas() > 1) {
            cuerpo.append("Sesiones estimadas: ").append(cotizacion.getSesionesEstimadas()).append("\n");
        }
        cuerpo.append(solicitud.esTatuaje() ? "Abono" : "Anticipo").append(" requerido (")
                .append(reglasPago.porcentajeMinimo(solicitud.getTipoServicio())).append("%): $")
                .append(ReglasPago.formato(minimo)).append(" COP\n");
        if (observaciones != null && !observaciones.isBlank()) {
            cuerpo.append("\nNotas del artista: ").append(observaciones.trim()).append("\n");
        }
        cuerpo.append("\nIngresa a HandgelusApp para aceptarla o rechazarla.");
        notificador.correoCliente(cliente, "Handgelus · Tu cotización está lista", cuerpo.toString(), solicitudId);
        return ensamblador.detalle(solicitud);
    }

    @Override
    public SolicitudDetalle decidir(Long solicitudId, boolean acepta, Actor actor) {
        actor.exigir(Rol.CLIENTE);
        Solicitud solicitud = ensamblador.solicitud(solicitudId);
        actor.exigirPropietarioOPersonal(solicitud.getClienteId());
        solicitud.exigirEstado(EstadoSolicitud.COTIZACION_GENERADA);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Cotizacion cotizacion = ensamblador.cotizacion(solicitudId);
        Usuario cliente = ensamblador.usuario(solicitud.getClienteId());

        if (acepta) {
            cotizacion.aceptar(ahora);
            solicitud.cambiarEstado(EstadoSolicitud.COTIZACION_ACEPTADA, ahora);
            cotizaciones.guardar(cotizacion);
            solicitudes.guardar(solicitud);
            emisor.emitir(solicitud, cliente, TipoComprobante.COTIZACION_ACEPTADA,
                    "Cotización aceptada el " + ahora.toLocalDate() + ". Siguiente paso: pago del "
                            + (solicitud.esTatuaje() ? "abono" : "anticipo") + ".",
                    cotizacion.getValorFinal());
            notificador.alertaInterna("Cotización aceptada · " + solicitud.getTitulo(),
                    cliente.getNombre() + " aceptó la cotización. Queda pendiente el pago inicial.", solicitudId);
        } else {
            // CU-02 7.a: si el cliente rechaza, la solicitud pasa a 'Cancelada' y se notifica a la asistente
            cotizacion.rechazar(ahora);
            solicitud.cancelar("Cotización rechazada por el cliente", ahora);
            cotizaciones.guardar(cotizacion);
            solicitudes.guardar(solicitud);
            notificador.alertaInterna("Cotización rechazada · " + solicitud.getTitulo(),
                    cliente.getNombre() + " rechazó la cotización. La solicitud quedó cancelada.", solicitudId);
        }
        return ensamblador.detalle(solicitud);
    }

    @Override
    public int alertarCotizacionesSinConfirmar() {
        LocalDateTime limite = LocalDateTime.now(reloj).minusHours(parametros.horasAlertaCotizacion());
        int alertas = 0;
        for (Cotizacion cotizacion : cotizaciones.listarPreliminaresSinAlertaAntesDe(limite)) {
            Solicitud solicitud = solicitudes.buscarPorId(cotizacion.getSolicitudId()).orElse(null);
            if (solicitud == null || solicitud.getEstado() != EstadoSolicitud.RECIBIDA) {
                continue;
            }
            notificador.alertaInterna("Cotización sin confirmar · " + solicitud.getTitulo(),
                    "La solicitud lleva más de " + parametros.horasAlertaCotizacion()
                            + " horas sin cotización confirmada por el artista.", solicitud.getId());
            cotizacion.marcarAlertaEmitida();
            cotizaciones.guardar(cotizacion);
            alertas++;
        }
        return alertas;
    }
}
