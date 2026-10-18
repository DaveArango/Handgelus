package com.handgelus.application.service.soporte;

import com.handgelus.application.port.in.modelo.CitaAgenda;
import com.handgelus.application.port.in.modelo.ResumenPago;
import com.handgelus.application.port.in.modelo.SolicitudDetalle;
import com.handgelus.application.port.in.modelo.SolicitudResumen;
import com.handgelus.application.port.in.modelo.UsuarioVista;
import com.handgelus.application.port.out.AvanceRepositoryPort;
import com.handgelus.application.port.out.CitaRepositoryPort;
import com.handgelus.application.port.out.ComprobanteRepositoryPort;
import com.handgelus.application.port.out.CotizacionRepositoryPort;
import com.handgelus.application.port.out.ObraRepositoryPort;
import com.handgelus.application.port.out.PagoRepositoryPort;
import com.handgelus.application.port.out.SolicitudRepositoryPort;
import com.handgelus.application.port.out.UsuarioRepositoryPort;
import com.handgelus.domain.exception.RecursoNoEncontradoException;
import com.handgelus.domain.model.Cita;
import com.handgelus.domain.model.Cotizacion;
import com.handgelus.domain.model.EstadoCotizacion;
import com.handgelus.domain.model.Pago;
import com.handgelus.domain.model.Solicitud;
import com.handgelus.domain.model.TipoPago;
import com.handgelus.domain.model.Usuario;
import com.handgelus.domain.service.ReglasPago;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Construye las vistas de lectura de una solicitud a partir de los repositorios. */
public class EnsambladorDetalle {

    private final SolicitudRepositoryPort solicitudes;
    private final UsuarioRepositoryPort usuarios;
    private final CotizacionRepositoryPort cotizaciones;
    private final PagoRepositoryPort pagos;
    private final CitaRepositoryPort citas;
    private final ObraRepositoryPort obras;
    private final AvanceRepositoryPort avances;
    private final ComprobanteRepositoryPort comprobantes;
    private final ReglasPago reglasPago;

    public EnsambladorDetalle(SolicitudRepositoryPort solicitudes, UsuarioRepositoryPort usuarios,
                              CotizacionRepositoryPort cotizaciones, PagoRepositoryPort pagos,
                              CitaRepositoryPort citas, ObraRepositoryPort obras, AvanceRepositoryPort avances,
                              ComprobanteRepositoryPort comprobantes, ReglasPago reglasPago) {
        this.solicitudes = solicitudes;
        this.usuarios = usuarios;
        this.cotizaciones = cotizaciones;
        this.pagos = pagos;
        this.citas = citas;
        this.obras = obras;
        this.avances = avances;
        this.comprobantes = comprobantes;
        this.reglasPago = reglasPago;
    }

    public Solicitud solicitud(Long id) {
        return solicitudes.buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("La solicitud", id));
    }

    public Usuario usuario(Long id) {
        return usuarios.buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("El usuario", id));
    }

    public Cotizacion cotizacion(Long solicitudId) {
        return cotizaciones.buscarPorSolicitud(solicitudId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La solicitud aún no tiene cotización."));
    }

    /** Valor del servicio: definitivo si fue confirmado; si no, el preliminar. */
    public BigDecimal valorServicio(Cotizacion cotizacion) {
        return cotizacion == null ? BigDecimal.ZERO : cotizacion.valorVigente();
    }

    public ResumenPago resumenPago(Solicitud solicitud, Cotizacion cotizacion, List<Pago> listaPagos) {
        BigDecimal total = valorServicio(cotizacion);
        TipoPago tipoInicial = reglasPago.tipoPagoInicial(solicitud.getTipoServicio());
        boolean inicialConfirmado = listaPagos.stream()
                .anyMatch(p -> p.estaConfirmado() && p.getTipo().esPagoInicial());
        boolean pendiente = listaPagos.stream().anyMatch(Pago::estaPendiente);
        return new ResumenPago(total, reglasPago.totalConfirmado(listaPagos), reglasPago.saldo(total, listaPagos),
                reglasPago.estadoPago(total, listaPagos), tipoInicial,
                reglasPago.porcentajeMinimo(solicitud.getTipoServicio()),
                reglasPago.montoMinimoInicial(total, solicitud.getTipoServicio()), inicialConfirmado, pendiente);
    }

    public ResumenPago resumenPago(Solicitud solicitud) {
        return resumenPago(solicitud, cotizaciones.buscarPorSolicitud(solicitud.getId()).orElse(null),
                pagos.listarPorSolicitud(solicitud.getId()));
    }

    public SolicitudDetalle detalle(Solicitud solicitud) {
        Long id = solicitud.getId();
        Usuario cliente = usuarios.buscarPorId(solicitud.getClienteId()).orElse(null);
        Cotizacion cotizacion = cotizaciones.buscarPorSolicitud(id).orElse(null);
        List<Pago> listaPagos = pagos.listarPorSolicitud(id).stream()
                .sorted(Comparator.comparing(Pago::getFechaCreacion).reversed()).toList();
        List<CitaAgenda> listaCitas = citasAgenda(citas.listarPorSolicitud(id));
        return new SolicitudDetalle(solicitud, UsuarioVista.de(cliente), cotizacion,
                resumenPago(solicitud, cotizacion, listaPagos), listaPagos, listaCitas,
                obras.buscarPorSolicitud(id).orElse(null),
                avances.listarPorSolicitud(id),
                comprobantes.listarPorSolicitud(id));
    }

    public SolicitudDetalle detalle(Long solicitudId) {
        return detalle(solicitud(solicitudId));
    }

    /** Enriquece citas con nombres de cliente, proyecto y tatuador. */
    public List<CitaAgenda> citasAgenda(List<Cita> lista) {
        if (lista.isEmpty()) {
            return List.of();
        }
        Map<Long, Solicitud> porSolicitud = solicitudes.buscarPorIds(
                        lista.stream().map(Cita::getSolicitudId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Solicitud::getId, Function.identity()));
        java.util.Set<Long> idsUsuarios = new java.util.HashSet<>();
        lista.forEach(c -> idsUsuarios.add(c.getTatuadorId()));
        porSolicitud.values().forEach(s -> idsUsuarios.add(s.getClienteId()));
        Map<Long, Usuario> porUsuario = usuarios.buscarPorIds(idsUsuarios).stream()
                .collect(Collectors.toMap(Usuario::getId, Function.identity()));
        return lista.stream()
                .sorted(Comparator.comparing(Cita::inicio))
                .map(c -> {
                    Solicitud s = porSolicitud.get(c.getSolicitudId());
                    String cliente = Optional.ofNullable(s).map(x -> porUsuario.get(x.getClienteId()))
                            .map(Usuario::getNombre).orElse("—");
                    String tatuador = Optional.ofNullable(porUsuario.get(c.getTatuadorId()))
                            .map(Usuario::getNombre).orElse("—");
                    return new CitaAgenda(c, cliente, s == null ? "—" : s.getTitulo(), tatuador,
                            s == null ? null : s.getEstado());
                }).toList();
    }

    /** Filas de la bandeja con cliente, valor y estado de pago, consultando en lote. */
    public List<SolicitudResumen> resumir(List<Solicitud> lista) {
        if (lista.isEmpty()) {
            return List.of();
        }
        Set<Long> ids = lista.stream().map(Solicitud::getId).collect(Collectors.toSet());
        Map<Long, Usuario> clientes = usuarios.buscarPorIds(
                        lista.stream().map(Solicitud::getClienteId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Usuario::getId, Function.identity()));
        Map<Long, Cotizacion> porSolicitud = cotizaciones.buscarPorSolicitudes(ids).stream()
                .collect(Collectors.toMap(Cotizacion::getSolicitudId, Function.identity(), (a, b) -> a));
        Map<Long, List<Pago>> pagosPorSolicitud = pagos.listarPorSolicitudes(ids).stream()
                .collect(Collectors.groupingBy(Pago::getSolicitudId));
        return lista.stream().map(s -> {
            Cotizacion cot = porSolicitud.get(s.getId());
            BigDecimal valor = valorServicio(cot);
            Usuario cliente = clientes.get(s.getClienteId());
            return new SolicitudResumen(s, cliente == null ? "—" : cliente.getNombre(), valor,
                    cotizacionConfirmada(cot),
                    reglasPago.estadoPago(valor, pagosPorSolicitud.getOrDefault(s.getId(), List.of())));
        }).toList();
    }

    public boolean cotizacionConfirmada(Cotizacion c) {
        return c != null && c.getEstado() != EstadoCotizacion.PRELIMINAR;
    }
}
