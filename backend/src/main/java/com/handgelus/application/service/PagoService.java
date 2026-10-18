package com.handgelus.application.service;

import com.handgelus.application.port.in.PagoUseCase;
import com.handgelus.application.port.in.modelo.CheckoutVista;
import com.handgelus.application.port.in.modelo.PagoVista;
import com.handgelus.application.port.in.modelo.SolicitudDetalle;
import com.handgelus.application.port.out.ObraRepositoryPort;
import com.handgelus.application.port.out.PagoRepositoryPort;
import com.handgelus.application.port.out.PasarelaPagoPort;
import com.handgelus.application.port.out.PasarelaPagoPort.SesionPago;
import com.handgelus.application.port.out.SolicitudRepositoryPort;
import com.handgelus.application.port.out.UsuarioRepositoryPort;
import com.handgelus.application.service.soporte.EmisorComprobantes;
import com.handgelus.application.service.soporte.EnsambladorDetalle;
import com.handgelus.application.service.soporte.Notificador;
import com.handgelus.domain.exception.AccesoDenegadoException;
import com.handgelus.domain.exception.RecursoNoEncontradoException;
import com.handgelus.domain.exception.ReglaNegocioException;
import com.handgelus.domain.model.Actor;
import com.handgelus.domain.model.Cotizacion;
import com.handgelus.domain.model.EstadoCotizacion;
import com.handgelus.domain.model.EstadoPago;
import com.handgelus.domain.model.EstadoSolicitud;
import com.handgelus.domain.model.MetodoPago;
import com.handgelus.domain.model.Obra;
import com.handgelus.domain.model.Pago;
import com.handgelus.domain.model.ParametrosNegocio;
import com.handgelus.domain.model.Rol;
import com.handgelus.domain.model.Solicitud;
import com.handgelus.domain.model.TipoComprobante;
import com.handgelus.domain.model.TipoPago;
import com.handgelus.domain.model.Usuario;
import com.handgelus.domain.service.ReglasPago;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** F-05 · Registrar pago de abono o anticipo (RF-08, RF-09, RF-14 · SWR-12/13/14). */
public class PagoService implements PagoUseCase {

    /** Estados en los que se puede abonar el saldo restante. */
    private static final Set<EstadoSolicitud> ESTADOS_SALDO = EnumSet.of(EstadoSolicitud.PAGO_PARCIAL_CONFIRMADO,
            EstadoSolicitud.CITA_AGENDADA, EstadoSolicitud.EN_PRODUCCION, EstadoSolicitud.TERMINADA);

    private final SolicitudRepositoryPort solicitudes;
    private final PagoRepositoryPort pagos;
    private final ObraRepositoryPort obras;
    private final UsuarioRepositoryPort usuarios;
    private final PasarelaPagoPort pasarela;
    private final ReglasPago reglasPago;
    private final EnsambladorDetalle ensamblador;
    private final EmisorComprobantes emisor;
    private final Notificador notificador;
    private final ParametrosNegocio parametros;
    private final Clock reloj;

    public PagoService(SolicitudRepositoryPort solicitudes, PagoRepositoryPort pagos, ObraRepositoryPort obras,
                       UsuarioRepositoryPort usuarios, PasarelaPagoPort pasarela, ReglasPago reglasPago,
                       EnsambladorDetalle ensamblador, EmisorComprobantes emisor, Notificador notificador,
                       ParametrosNegocio parametros, Clock reloj) {
        this.solicitudes = solicitudes;
        this.pagos = pagos;
        this.obras = obras;
        this.usuarios = usuarios;
        this.pasarela = pasarela;
        this.reglasPago = reglasPago;
        this.ensamblador = ensamblador;
        this.emisor = emisor;
        this.notificador = notificador;
        this.parametros = parametros;
        this.reloj = reloj;
    }

    @Override
    public CheckoutVista iniciarPago(Long solicitudId, BigDecimal monto, MetodoPago metodo, Actor actor) {
        actor.exigir(Rol.CLIENTE);
        Solicitud solicitud = ensamblador.solicitud(solicitudId);
        actor.exigirPropietarioOPersonal(solicitud.getClienteId());
        MetodoPago metodoFinal = metodo == null ? MetodoPago.TARJETA : metodo;
        if (!metodoFinal.usaPasarela()) {
            throw new ReglaNegocioException("Los pagos en línea solo admiten tarjeta, PSE o Nequi.");
        }
        PagoPlaneado plan = planificar(solicitud, monto);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Usuario cliente = ensamblador.usuario(solicitud.getClienteId());

        String referencia = pasarela.generarReferencia();
        String concepto = plan.tipo().getEtiqueta() + " · " + solicitud.getTitulo();
        SesionPago sesion = pasarela.crearSesion(referencia, plan.monto(), concepto, cliente.getCorreo());
        Pago pago = pagos.guardar(Pago.pendiente(solicitudId, plan.tipo(), plan.monto(), metodoFinal,
                sesion.referencia(), actor.id(), ahora));

        if (plan.tipo().esPagoInicial()) {
            solicitud.cambiarEstado(EstadoSolicitud.PAGO_EN_PROCESO, ahora);
            solicitudes.guardar(solicitud);
        }
        return new CheckoutVista(pago, sesion.urlCheckout(), sesion.proveedor(), concepto, cliente.getNombre(),
                solicitudId);
    }

    /** Determina el tipo de pago y valida el monto según el estado del servicio. */
    private PagoPlaneado planificar(Solicitud solicitud, BigDecimal monto) {
        Cotizacion cotizacion = ensamblador.cotizacion(solicitud.getId());
        if (cotizacion.getEstado() != EstadoCotizacion.ACEPTADA) {
            throw new ReglaNegocioException("El pago se habilita cuando la cotización ha sido aceptada.");
        }
        List<Pago> existentes = pagos.listarPorSolicitud(solicitud.getId());
        if (existentes.stream().anyMatch(Pago::estaPendiente)) {
            throw new ReglaNegocioException("Ya existe un pago en proceso para este servicio. "
                    + "Espere su confirmación antes de registrar otro.");
        }
        BigDecimal total = cotizacion.valorVigente();
        if (solicitud.getEstado() == EstadoSolicitud.COTIZACION_ACEPTADA) {
            BigDecimal valor = monto == null
                    ? reglasPago.montoMinimoInicial(total, solicitud.getTipoServicio()) : monto;
            reglasPago.validarPagoInicial(valor, total, solicitud.getTipoServicio());
            return new PagoPlaneado(reglasPago.tipoPagoInicial(solicitud.getTipoServicio()), valor);
        }
        if (ESTADOS_SALDO.contains(solicitud.getEstado())) {
            BigDecimal saldo = reglasPago.saldo(total, existentes);
            BigDecimal valor = monto == null ? saldo : monto;
            reglasPago.validarPagoSaldo(valor, saldo);
            return new PagoPlaneado(TipoPago.SALDO, valor);
        }
        throw new ReglaNegocioException("No es posible registrar pagos en el estado '"
                + solicitud.getEstado().getEtiqueta() + "'.");
    }

    @Override
    public CheckoutVista consultarCheckout(String referencia, Actor actor) {
        Pago pago = buscarPorReferencia(referencia);
        Solicitud solicitud = ensamblador.solicitud(pago.getSolicitudId());
        actor.exigirPropietarioOPersonal(solicitud.getClienteId());
        Usuario cliente = ensamblador.usuario(solicitud.getClienteId());
        return new CheckoutVista(pago, null, "Pasarela Handgelus", pago.getTipo().getEtiqueta() + " · "
                + solicitud.getTitulo(), cliente.getNombre(), solicitud.getId());
    }

    @Override
    public Pago procesarRespuestaPasarela(String referencia, boolean aprobado, String firma) {
        if (!pasarela.firmaValida(firma)) {
            throw new AccesoDenegadoException("Firma de la pasarela inválida.");
        }
        Pago pago = buscarPorReferencia(referencia);
        if (!pago.estaPendiente()) {
            return pago;   // idempotencia: la pasarela puede reenviar el webhook
        }
        Solicitud solicitud = ensamblador.solicitud(pago.getSolicitudId());
        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (aprobado) {
            pago.confirmar(ahora);
            pago = pagos.guardar(pago);
            alConfirmarPago(pago, solicitud, ahora);
        } else {
            pago.rechazar(ahora);
            pago = pagos.guardar(pago);
            if (pago.getTipo().esPagoInicial() && solicitud.getEstado() == EstadoSolicitud.PAGO_EN_PROCESO) {
                solicitud.cambiarEstado(EstadoSolicitud.COTIZACION_ACEPTADA, ahora);
                solicitudes.guardar(solicitud);
            }
            notificador.correoCliente(ensamblador.usuario(solicitud.getClienteId()),
                    "Handgelus · Tu pago no fue aprobado",
                    "La pasarela no aprobó el pago " + pago.getReferencia() + " por $"
                            + ReglasPago.formato(pago.getMonto()) + ". Puedes intentarlo nuevamente desde tu perfil.",
                    solicitud.getId());
        }
        return pago;
    }

    @Override
    public SolicitudDetalle registrarPagoManual(Long solicitudId, BigDecimal monto, MetodoPago metodo, Actor actor) {
        actor.exigir(Rol.ASISTENTE, Rol.TATUADOR, Rol.ADMINISTRADOR);
        if (monto == null) {
            throw new ReglaNegocioException("Indique el monto recibido.");
        }
        Solicitud solicitud = ensamblador.solicitud(solicitudId);
        if (solicitud.getEstado() == EstadoSolicitud.PAGO_EN_PROCESO) {
            throw new ReglaNegocioException("El cliente tiene un pago en proceso con la pasarela. Espere su resultado.");
        }
        PagoPlaneado plan = planificar(solicitud, monto);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        MetodoPago metodoFinal = metodo == null ? MetodoPago.EFECTIVO : metodo;
        String referencia = "HG-MAN-" + System.currentTimeMillis();
        Pago pago = pagos.guardar(Pago.confirmadoManual(solicitudId, plan.tipo(), plan.monto(), metodoFinal,
                referencia, actor.id(), ahora));
        alConfirmarPago(pago, solicitud, ahora);
        return ensamblador.detalle(solicitudId);
    }

    /** SWR-13: actualiza el estado del servicio y emite el comprobante del pago. */
    private void alConfirmarPago(Pago pago, Solicitud solicitud, LocalDateTime ahora) {
        if (pago.getTipo().esPagoInicial()) {
            solicitud.cambiarEstado(EstadoSolicitud.PAGO_PARCIAL_CONFIRMADO, ahora);
            solicitudes.guardar(solicitud);
            if (solicitud.esComision() && obras.buscarPorSolicitud(solicitud.getId()).isEmpty()) {
                obras.guardar(Obra.enEspera(solicitud.getId()));    // RN-10: la obra puede iniciar producción
            }
        }
        Usuario cliente = ensamblador.usuario(solicitud.getClienteId());
        BigDecimal saldo = ensamblador.resumenPago(solicitud).saldo();
        emisor.emitir(solicitud, cliente, TipoComprobante.PAGO,
                pago.getTipo().getEtiqueta() + " recibido (" + pago.getMetodo().getEtiqueta() + ", ref. "
                        + pago.getReferencia() + "). Saldo pendiente: $" + ReglasPago.formato(saldo) + ".",
                pago.getMonto());
        String siguiente = !pago.getTipo().esPagoInicial() ? "Se registró un pago de saldo."
                : solicitud.esTatuaje() ? "Ya puede agendarse la cita de tatuaje."
                : "La obra puede iniciar producción.";
        notificador.alertaInterna("Pago confirmado · " + solicitud.getTitulo(),
                cliente.getNombre() + " pagó $" + ReglasPago.formato(pago.getMonto()) + " ("
                        + pago.getTipo().getEtiqueta().toLowerCase() + "). " + siguiente, solicitud.getId());
    }

    @Override
    public List<PagoVista> listar(EstadoPago estado, Actor actor) {
        actor.exigir(Rol.ASISTENTE, Rol.TATUADOR, Rol.ADMINISTRADOR);
        List<Pago> lista = pagos.listar(estado);
        if (lista.isEmpty()) {
            return List.of();
        }
        Map<Long, Solicitud> porSolicitud = solicitudes.buscarPorIds(
                        lista.stream().map(Pago::getSolicitudId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Solicitud::getId, Function.identity()));
        Map<Long, Usuario> clientes = usuarios.buscarPorIds(
                        porSolicitud.values().stream().map(Solicitud::getClienteId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Usuario::getId, Function.identity()));
        return lista.stream()
                .sorted(Comparator.comparing(Pago::getFechaCreacion).reversed())
                .map(p -> {
                    Solicitud s = porSolicitud.get(p.getSolicitudId());
                    Usuario c = s == null ? null : clientes.get(s.getClienteId());
                    return new PagoVista(p, c == null ? "—" : c.getNombre(), s == null ? "—" : s.getTitulo(),
                            s == null ? null : s.getTipoServicio());
                }).toList();
    }

    @Override
    public int expirarPagosVencidos() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        int expirados = 0;
        for (Pago pago : pagos.listarPendientesCreadosAntesDe(ahora.minusHours(parametros.horasExpiracionPago()))) {
            pago.expirar(ahora);
            pagos.guardar(pago);
            Solicitud solicitud = solicitudes.buscarPorId(pago.getSolicitudId()).orElse(null);
            if (solicitud != null && pago.getTipo().esPagoInicial()
                    && solicitud.getEstado() == EstadoSolicitud.PAGO_EN_PROCESO) {
                solicitud.cambiarEstado(EstadoSolicitud.COTIZACION_ACEPTADA, ahora);   // SWR-14
                solicitudes.guardar(solicitud);
                notificador.correoCliente(ensamblador.usuario(solicitud.getClienteId()),
                        "Handgelus · Tu pago expiró",
                        "No recibimos la confirmación del pago " + pago.getReferencia() + " en "
                                + parametros.horasExpiracionPago() + " horas. Tu cotización sigue vigente y "
                                + "puedes realizar el pago nuevamente.", solicitud.getId());
            }
            expirados++;
        }
        return expirados;
    }

    private Pago buscarPorReferencia(String referencia) {
        return pagos.buscarPorReferencia(referencia)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un pago con referencia " + referencia + "."));
    }

    private record PagoPlaneado(TipoPago tipo, BigDecimal monto) {
    }
}
