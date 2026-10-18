package com.handgelus.application.service;

import com.handgelus.application.port.in.TableroUseCase;
import com.handgelus.application.port.in.modelo.CitaAgenda;
import com.handgelus.application.port.in.modelo.SolicitudResumen;
import com.handgelus.application.port.in.modelo.TableroVista;
import com.handgelus.application.port.out.CitaRepositoryPort;
import com.handgelus.application.port.out.CotizacionRepositoryPort;
import com.handgelus.application.port.out.NotificacionRepositoryPort;
import com.handgelus.application.port.out.PagoRepositoryPort;
import com.handgelus.application.port.out.SolicitudRepositoryPort;
import com.handgelus.application.service.soporte.EnsambladorDetalle;
import com.handgelus.domain.exception.RecursoNoEncontradoException;
import com.handgelus.domain.model.Actor;
import com.handgelus.domain.model.Cita;
import com.handgelus.domain.model.Cotizacion;
import com.handgelus.domain.model.EstadoPago;
import com.handgelus.domain.model.EstadoSolicitud;
import com.handgelus.domain.model.Notificacion;
import com.handgelus.domain.model.Pago;
import com.handgelus.domain.model.Rol;
import com.handgelus.domain.model.Solicitud;
import com.handgelus.domain.model.TipoNotificacion;
import com.handgelus.domain.service.ReglasPago;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Panel de gestión del estudio (Imagen 6): indicadores, próximas citas y alertas. */
public class TableroService implements TableroUseCase {

    private static final Set<EstadoSolicitud> CON_SALDO = EnumSet.of(EstadoSolicitud.PAGO_PARCIAL_CONFIRMADO,
            EstadoSolicitud.CITA_AGENDADA, EstadoSolicitud.EN_PRODUCCION, EstadoSolicitud.TERMINADA);

    private final SolicitudRepositoryPort solicitudes;
    private final CotizacionRepositoryPort cotizaciones;
    private final PagoRepositoryPort pagos;
    private final CitaRepositoryPort citas;
    private final NotificacionRepositoryPort notificaciones;
    private final EnsambladorDetalle ensamblador;
    private final ReglasPago reglasPago;
    private final Clock reloj;

    public TableroService(SolicitudRepositoryPort solicitudes, CotizacionRepositoryPort cotizaciones,
                          PagoRepositoryPort pagos, CitaRepositoryPort citas,
                          NotificacionRepositoryPort notificaciones, EnsambladorDetalle ensamblador,
                          ReglasPago reglasPago, Clock reloj) {
        this.solicitudes = solicitudes;
        this.cotizaciones = cotizaciones;
        this.pagos = pagos;
        this.citas = citas;
        this.notificaciones = notificaciones;
        this.ensamblador = ensamblador;
        this.reglasPago = reglasPago;
        this.reloj = reloj;
    }

    @Override
    public TableroVista tablero(Actor actor) {
        actor.exigir(Rol.ASISTENTE, Rol.TATUADOR, Rol.ADMINISTRADOR);
        List<Solicitud> todas = solicitudes.buscar(null, null, null);
        Map<EstadoSolicitud, Long> conteo = todas.stream()
                .collect(Collectors.groupingBy(Solicitud::getEstado, Collectors.counting()));
        Map<String, Long> porEstado = new LinkedHashMap<>();
        for (EstadoSolicitud e : EstadoSolicitud.values()) {
            porEstado.put(e.name(), conteo.getOrDefault(e, 0L));
        }

        YearMonth mes = YearMonth.now(reloj);
        BigDecimal ingresosMes = pagos.listar(EstadoPago.CONFIRMADO).stream()
                .filter(p -> p.getFechaConfirmacion() != null && YearMonth.from(p.getFechaConfirmacion()).equals(mes))
                .map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Solicitud> conSaldo = todas.stream().filter(s -> CON_SALDO.contains(s.getEstado())).toList();
        BigDecimal saldos = BigDecimal.ZERO;
        long serviciosConSaldo = 0;
        if (!conSaldo.isEmpty()) {
            Set<Long> ids = conSaldo.stream().map(Solicitud::getId).collect(Collectors.toSet());
            Map<Long, Cotizacion> cots = cotizaciones.buscarPorSolicitudes(ids).stream()
                    .collect(Collectors.toMap(Cotizacion::getSolicitudId, Function.identity(), (a, b) -> a));
            Map<Long, List<Pago>> pagosPorSolicitud = pagos.listarPorSolicitudes(ids).stream()
                    .collect(Collectors.groupingBy(Pago::getSolicitudId));
            for (Solicitud s : conSaldo) {
                BigDecimal saldo = reglasPago.saldo(ensamblador.valorServicio(cots.get(s.getId())),
                        pagosPorSolicitud.getOrDefault(s.getId(), List.of()));
                if (saldo.signum() > 0) {
                    saldos = saldos.add(saldo);
                    serviciosConSaldo++;
                }
            }
        }

        LocalDate hoy = LocalDate.now(reloj);
        List<CitaAgenda> proximas = ensamblador.citasAgenda(citas.listarEntre(hoy, hoy.plusDays(21)).stream()
                .filter(Cita::ocupaAgenda).filter(c -> c.getEstado() == com.handgelus.domain.model.EstadoCita.PROGRAMADA)
                .toList()).stream().limit(6).toList();
        List<SolicitudResumen> ultimas = ensamblador.resumir(todas.stream().limit(6).toList());

        return new TableroVista(ingresosMes, saldos, serviciosConSaldo,
                conteo.getOrDefault(EstadoSolicitud.RECIBIDA, 0L),
                conteo.getOrDefault(EstadoSolicitud.COTIZACION_GENERADA, 0L),
                pagos.listar(EstadoPago.PENDIENTE).size(),
                conteo.getOrDefault(EstadoSolicitud.EN_PRODUCCION, 0L),
                notificaciones.contarNoLeidas(TipoNotificacion.ALERTA_INTERNA),
                porEstado, proximas, ultimas);
    }

    @Override
    public List<Notificacion> notificaciones(TipoNotificacion tipo, Actor actor) {
        actor.exigir(Rol.ASISTENTE, Rol.TATUADOR, Rol.ADMINISTRADOR);
        return notificaciones.listar(tipo, 150);
    }

    @Override
    public void marcarNotificacionLeida(Long id, Actor actor) {
        actor.exigir(Rol.ASISTENTE, Rol.TATUADOR, Rol.ADMINISTRADOR);
        Notificacion n = notificaciones.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La notificación", id));
        n.marcarLeida();
        notificaciones.guardar(n);
    }
}
