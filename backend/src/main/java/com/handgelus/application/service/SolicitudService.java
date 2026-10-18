package com.handgelus.application.service;

import com.handgelus.application.port.in.SolicitudUseCase;
import com.handgelus.application.port.in.modelo.SolicitudDetalle;
import com.handgelus.application.port.in.modelo.SolicitudResumen;
import com.handgelus.application.port.out.AlmacenamientoArchivosPort;
import com.handgelus.application.port.out.AlmacenamientoArchivosPort.ArchivoAlmacenado;
import com.handgelus.application.port.out.AvanceRepositoryPort;
import com.handgelus.application.port.out.CotizacionRepositoryPort;
import com.handgelus.application.port.out.PagoRepositoryPort;
import com.handgelus.application.port.out.SolicitudRepositoryPort;
import com.handgelus.application.port.out.TarifaRepositoryPort;
import com.handgelus.application.port.out.UsuarioRepositoryPort;
import com.handgelus.application.service.soporte.EnsambladorDetalle;
import com.handgelus.application.service.soporte.Notificador;
import com.handgelus.domain.exception.AccesoDenegadoException;
import com.handgelus.domain.exception.RecursoNoEncontradoException;
import com.handgelus.domain.exception.ReglaNegocioException;
import com.handgelus.domain.model.Actor;
import com.handgelus.domain.model.Cotizacion;
import com.handgelus.domain.model.EstadoSolicitud;
import com.handgelus.domain.model.ParametrosNegocio;
import com.handgelus.domain.model.RegistroAvance;
import com.handgelus.domain.model.ResultadoCotizacion;
import com.handgelus.domain.model.Rol;
import com.handgelus.domain.model.Solicitud;
import com.handgelus.domain.model.Tarifa;
import com.handgelus.domain.model.TipoServicio;
import com.handgelus.domain.model.Usuario;
import com.handgelus.domain.service.CalculadoraCotizacion;
import com.handgelus.domain.service.ReglasPago;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** F-03 · Registrar solicitud de servicio (CU-01 · RF-02/RF-03 · SWR-06/07/08). */
public class SolicitudService implements SolicitudUseCase {

    private static final Set<String> TIPOS_IMAGEN = Set.of("image/png", "image/jpeg", "image/webp", "application/pdf");

    private final SolicitudRepositoryPort solicitudes;
    private final UsuarioRepositoryPort usuarios;
    private final TarifaRepositoryPort tarifas;
    private final CotizacionRepositoryPort cotizaciones;
    private final PagoRepositoryPort pagos;
    private final AvanceRepositoryPort avances;
    private final AlmacenamientoArchivosPort almacenamiento;
    private final CalculadoraCotizacion calculadora;
    private final ReglasPago reglasPago;
    private final EnsambladorDetalle ensamblador;
    private final Notificador notificador;
    private final ParametrosNegocio parametros;
    private final Clock reloj;

    public SolicitudService(SolicitudRepositoryPort solicitudes, UsuarioRepositoryPort usuarios,
                            TarifaRepositoryPort tarifas, CotizacionRepositoryPort cotizaciones,
                            PagoRepositoryPort pagos, AvanceRepositoryPort avances,
                            AlmacenamientoArchivosPort almacenamiento, CalculadoraCotizacion calculadora,
                            ReglasPago reglasPago, EnsambladorDetalle ensamblador, Notificador notificador,
                            ParametrosNegocio parametros, Clock reloj) {
        this.solicitudes = solicitudes;
        this.usuarios = usuarios;
        this.tarifas = tarifas;
        this.cotizaciones = cotizaciones;
        this.pagos = pagos;
        this.avances = avances;
        this.almacenamiento = almacenamiento;
        this.calculadora = calculadora;
        this.reglasPago = reglasPago;
        this.ensamblador = ensamblador;
        this.notificador = notificador;
        this.parametros = parametros;
        this.reloj = reloj;
    }

    @Override
    public SolicitudDetalle registrar(RegistrarSolicitudComando c, Actor actor) {
        actor.exigir(Rol.CLIENTE);
        Usuario cliente = ensamblador.usuario(actor.id());   // SWR-06: cliente registrado
        if (c.tipoServicio() == null) {
            throw new ReglaNegocioException("Seleccione el tipo de servicio.");
        }
        if (c.categoria() == null) {
            throw new ReglaNegocioException("RN-08", "Seleccione un tamaño de la lista establecida.");
        }
        Tarifa tarifa = tarifas.buscarPorTipoYCategoria(c.tipoServicio(), c.categoria())
                .orElseThrow(() -> new ReglaNegocioException("RF-04",
                        "El estudio aún no ha configurado la tarifa para este tamaño."));

        LocalDateTime ahora = LocalDateTime.now(reloj);
        construir(c, cliente.getId(), null, ahora);           // valida los datos antes de guardar archivos
        String imagen = guardarImagen(c.imagen());
        Solicitud guardada = solicitudes.guardar(construir(c, cliente.getId(), imagen, ahora));

        // CU-02 paso 1: el sistema calcula la cotización preliminar al recibir la solicitud (RF-04)
        ResultadoCotizacion calculo = calculadora.calcular(tarifa, c.estilo(), c.ciudadDestino());
        cotizaciones.guardar(Cotizacion.preliminar(guardada.getId(), calculo, ahora));

        // CU-01 paso 6: la asistente recibe notificación de la nueva solicitud
        notificador.alertaInterna("Nueva solicitud · " + guardada.getTitulo(),
                cliente.getNombre() + " envió una solicitud de " + c.tipoServicio().getEtiqueta().toLowerCase()
                        + " (" + c.categoria().etiquetaPara(c.tipoServicio()) + "). Valor preliminar: $"
                        + ReglasPago.formato(calculo.valorTotal()) + ".", guardada.getId());
        notificador.correoCliente(cliente, "Handgelus · Recibimos tu solicitud",
                "Hemos recibido tu solicitud \"" + guardada.getTitulo() + "\". El artista la revisará y te "
                        + "enviaremos la cotización definitiva muy pronto.", guardada.getId());
        return ensamblador.detalle(guardada);
    }

    private Solicitud construir(RegistrarSolicitudComando c, Long clienteId, String imagen, LocalDateTime ahora) {
        return c.tipoServicio() == TipoServicio.TATUAJE
                ? Solicitud.nuevaTatuaje(clienteId, c.titulo(), c.descripcion(), c.categoria(), c.estilo(),
                c.zonaCuerpo(), imagen, ahora)
                : Solicitud.nuevaComision(clienteId, c.titulo(), c.descripcion(), c.categoria(), c.estilo(),
                c.direccionDestino(), c.ciudadDestino(), imagen, ahora);
    }

    private String guardarImagen(ImagenAdjunta imagen) {
        if (imagen == null || imagen.contenido() == null || imagen.contenido().length == 0) {
            return null;
        }
        if (imagen.contenido().length > parametros.imagenMaxBytes()) {
            throw new ReglaNegocioException("RF-02", "La imagen de referencia supera el máximo de 5 MB. "
                    + "Por favor adjunte una imagen de menor tamaño.");
        }
        if (imagen.contentType() == null || !TIPOS_IMAGEN.contains(imagen.contentType().toLowerCase())) {
            throw new ReglaNegocioException("Formato no permitido. Adjunte una imagen PNG, JPG, WEBP o un PDF.");
        }
        return almacenamiento.guardar(imagen.nombreOriginal(), imagen.contentType(), imagen.contenido());
    }

    @Override
    public List<SolicitudResumen> listar(EstadoSolicitud estado, TipoServicio tipo, String grupo, Actor actor) {
        Set<EstadoSolicitud> estados = estado != null ? EnumSet.of(estado) : estadosDelGrupo(grupo);
        Long clienteId = actor.esCliente() ? actor.id() : null;
        List<Solicitud> lista = solicitudes.buscar(estados, tipo, clienteId);
        return ensamblador.resumir(lista);
    }

    /** Pestañas de la bandeja (Imagen 8): nuevas, pendientes, activas, finalizadas. */
    private Set<EstadoSolicitud> estadosDelGrupo(String grupo) {
        if (grupo == null || grupo.isBlank()) {
            return null;
        }
        return switch (grupo.toUpperCase()) {
            case "NUEVAS" -> EnumSet.of(EstadoSolicitud.RECIBIDA);
            case "PENDIENTES" -> EnumSet.of(EstadoSolicitud.COTIZACION_GENERADA, EstadoSolicitud.COTIZACION_ACEPTADA,
                    EstadoSolicitud.PAGO_EN_PROCESO);
            case "ACTIVAS" -> EnumSet.of(EstadoSolicitud.PAGO_PARCIAL_CONFIRMADO, EstadoSolicitud.CITA_AGENDADA,
                    EstadoSolicitud.EN_PRODUCCION, EstadoSolicitud.TERMINADA);
            case "FINALIZADAS" -> EnumSet.of(EstadoSolicitud.ENVIADA, EstadoSolicitud.SERVICIO_COMPLETADO,
                    EstadoSolicitud.CANCELADA);
            default -> null;
        };
    }

    @Override
    public SolicitudDetalle detalle(Long solicitudId, Actor actor) {
        Solicitud solicitud = ensamblador.solicitud(solicitudId);
        actor.exigirPropietarioOPersonal(solicitud.getClienteId());
        return ensamblador.detalle(solicitud);
    }

    @Override
    public SolicitudDetalle cancelar(Long solicitudId, String motivo, Actor actor) {
        Solicitud solicitud = ensamblador.solicitud(solicitudId);
        actor.exigirPropietarioOPersonal(solicitud.getClienteId());
        if (actor.esCliente()) {
            solicitud.exigirEstado(EstadoSolicitud.RECIBIDA, EstadoSolicitud.COTIZACION_GENERADA,
                    EstadoSolicitud.COTIZACION_ACEPTADA);
        } else {
            actor.exigir(Rol.ASISTENTE, Rol.TATUADOR, Rol.ADMINISTRADOR);
        }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        String motivoFinal = motivo == null || motivo.isBlank()
                ? (actor.esCliente() ? "Cancelada por el cliente" : "Archivada por el estudio")
                : motivo.trim();
        solicitud.cancelar(motivoFinal, ahora);
        solicitudes.guardar(solicitud);

        Usuario cliente = ensamblador.usuario(solicitud.getClienteId());
        if (actor.esCliente()) {
            notificador.alertaInterna("Solicitud cancelada · " + solicitud.getTitulo(),
                    cliente.getNombre() + " canceló su solicitud. Motivo: " + motivoFinal, solicitud.getId());
        } else {
            notificador.correoCliente(cliente, "Handgelus · Actualización de tu solicitud",
                    "Tu solicitud \"" + solicitud.getTitulo() + "\" fue cerrada por el estudio. Motivo: "
                            + motivoFinal + ".", solicitud.getId());
        }
        return ensamblador.detalle(solicitud);
    }

    @Override
    public ArchivoAlmacenado archivo(String nombre, Actor actor) {
        if (actor.esCliente() && !clientePuedeVer(nombre, actor.id())) {
            throw new AccesoDenegadoException("No tiene acceso a este archivo.");
        }
        return almacenamiento.obtener(nombre)
                .orElseThrow(() -> new RecursoNoEncontradoException("El archivo solicitado no existe."));
    }

    private boolean clientePuedeVer(String nombre, Long clienteId) {
        for (Solicitud s : solicitudes.buscar(null, null, clienteId)) {
            if (nombre.equals(s.getImagenReferencia())) {
                return true;
            }
            for (RegistroAvance a : avances.listarPorSolicitud(s.getId())) {
                if (nombre.equals(a.getImagen())) {
                    return true;
                }
            }
        }
        return false;
    }
}
