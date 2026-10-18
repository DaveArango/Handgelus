package com.handgelus.application.service;

import com.handgelus.application.port.in.HistorialUseCase;
import com.handgelus.application.port.in.modelo.ClienteResumen;
import com.handgelus.application.port.in.modelo.HistorialCliente;
import com.handgelus.application.port.in.modelo.UsuarioVista;
import com.handgelus.application.port.out.ComprobanteRepositoryPort;
import com.handgelus.application.port.out.CotizacionRepositoryPort;
import com.handgelus.application.port.out.PagoRepositoryPort;
import com.handgelus.application.port.out.SolicitudRepositoryPort;
import com.handgelus.application.port.out.UsuarioRepositoryPort;
import com.handgelus.application.service.soporte.EnsambladorDetalle;
import com.handgelus.domain.exception.RecursoNoEncontradoException;
import com.handgelus.domain.exception.ReglaNegocioException;
import com.handgelus.domain.model.Actor;
import com.handgelus.domain.model.Comprobante;
import com.handgelus.domain.model.Cotizacion;
import com.handgelus.domain.model.EstadoSolicitud;
import com.handgelus.domain.model.Pago;
import com.handgelus.domain.model.Rol;
import com.handgelus.domain.model.Solicitud;
import com.handgelus.domain.model.Usuario;
import com.handgelus.domain.service.ReglasPago;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** F-08 · Consultar historial de servicios (RF-15 · SWR-21). */
public class HistorialService implements HistorialUseCase {

    private final UsuarioRepositoryPort usuarios;
    private final SolicitudRepositoryPort solicitudes;
    private final CotizacionRepositoryPort cotizaciones;
    private final PagoRepositoryPort pagos;
    private final ComprobanteRepositoryPort comprobantes;
    private final EnsambladorDetalle ensamblador;
    private final ReglasPago reglasPago;

    public HistorialService(UsuarioRepositoryPort usuarios, SolicitudRepositoryPort solicitudes,
                            CotizacionRepositoryPort cotizaciones, PagoRepositoryPort pagos,
                            ComprobanteRepositoryPort comprobantes, EnsambladorDetalle ensamblador,
                            ReglasPago reglasPago) {
        this.usuarios = usuarios;
        this.solicitudes = solicitudes;
        this.cotizaciones = cotizaciones;
        this.pagos = pagos;
        this.comprobantes = comprobantes;
        this.ensamblador = ensamblador;
        this.reglasPago = reglasPago;
    }

    @Override
    public List<ClienteResumen> clientes(String busqueda, Actor actor) {
        actor.exigir(Rol.ASISTENTE, Rol.TATUADOR, Rol.ADMINISTRADOR);
        String filtro = busqueda == null ? "" : normalizar(busqueda);
        List<Usuario> clientes = usuarios.listarPorRol(Rol.CLIENTE).stream()
                .filter(u -> filtro.isEmpty() || normalizar(u.getNombre()).contains(filtro)
                        || u.getCorreo().contains(filtro) || u.getTelefono().replaceAll("\\D", "").contains(filtro))
                .toList();
        Map<Long, List<Solicitud>> porCliente = solicitudes.buscar(null, null, null).stream()
                .collect(Collectors.groupingBy(Solicitud::getClienteId));
        Set<Long> idsSolicitudes = porCliente.values().stream().flatMap(List::stream).map(Solicitud::getId)
                .collect(Collectors.toSet());
        Map<Long, List<Pago>> pagosPorSolicitud = idsSolicitudes.isEmpty() ? Map.of()
                : pagos.listarPorSolicitudes(idsSolicitudes).stream().collect(Collectors.groupingBy(Pago::getSolicitudId));

        return clientes.stream().map(c -> {
                    List<Solicitud> propias = porCliente.getOrDefault(c.getId(), List.of());
                    BigDecimal pagado = propias.stream()
                            .map(s -> reglasPago.totalConfirmado(pagosPorSolicitud.getOrDefault(s.getId(), List.of())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    LocalDateTime ultimo = propias.stream().map(Solicitud::getFechaCreacion)
                            .max(Comparator.naturalOrder()).orElse(null);
                    int activos = (int) propias.stream().filter(s -> !s.getEstado().esFinal()).count();
                    return new ClienteResumen(UsuarioVista.de(c), propias.size(), activos, ultimo, pagado);
                })
                .sorted(Comparator.comparing(ClienteResumen::ultimoServicio,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public HistorialCliente historial(Long clienteId, Actor actor) {
        actor.exigirPropietarioOPersonal(clienteId);
        Usuario cliente = ensamblador.usuario(clienteId);
        if (cliente.getRol() != Rol.CLIENTE) {
            throw new ReglaNegocioException("El usuario indicado no es un cliente.");
        }
        List<Solicitud> propias = solicitudes.buscar(null, null, clienteId);
        Set<Long> ids = propias.stream().map(Solicitud::getId).collect(Collectors.toSet());
        Map<Long, Cotizacion> cots = ids.isEmpty() ? Map.of() : cotizaciones.buscarPorSolicitudes(ids).stream()
                .collect(Collectors.toMap(Cotizacion::getSolicitudId, Function.identity(), (a, b) -> a));
        Map<Long, List<Pago>> pagosPorSolicitud = ids.isEmpty() ? Map.of() : pagos.listarPorSolicitudes(ids).stream()
                .collect(Collectors.groupingBy(Pago::getSolicitudId));
        Map<Long, List<Comprobante>> comprobantesPorSolicitud = comprobantes.listarPorCliente(clienteId).stream()
                .collect(Collectors.groupingBy(Comprobante::getSolicitudId));

        List<HistorialCliente.Servicio> servicios = propias.stream().map(s -> {
            BigDecimal valor = ensamblador.valorServicio(cots.get(s.getId()));
            List<Pago> lista = pagosPorSolicitud.getOrDefault(s.getId(), List.of());
            return new HistorialCliente.Servicio(s.getId(), s.getTipoServicio(), s.getTitulo(), s.getFechaCreacion(),
                    valor, s.getEstado(), reglasPago.estadoPago(valor, lista), reglasPago.totalConfirmado(lista),
                    comprobantesPorSolicitud.getOrDefault(s.getId(), List.of()).stream()
                            .sorted(Comparator.comparing(Comprobante::getFechaEmision)).toList());
        }).sorted(Comparator.comparing(HistorialCliente.Servicio::fecha).reversed()).toList();

        BigDecimal totalPagado = servicios.stream().map(HistorialCliente.Servicio::pagado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int completados = (int) servicios.stream().filter(s -> s.estado() == EstadoSolicitud.SERVICIO_COMPLETADO
                || s.estado() == EstadoSolicitud.ENVIADA).count();
        return new HistorialCliente(UsuarioVista.de(cliente), servicios, totalPagado, servicios.size(), completados);
    }

    @Override
    public Comprobante comprobante(Long comprobanteId, Actor actor) {
        Comprobante c = comprobantes.buscarPorId(comprobanteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El comprobante", comprobanteId));
        actor.exigirPropietarioOPersonal(c.getClienteId());
        return c;
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(Objects.requireNonNullElse(texto, "").trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }
}
