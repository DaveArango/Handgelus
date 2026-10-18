package com.handgelus.application.service;

import com.handgelus.application.port.in.AvanceObraUseCase;
import com.handgelus.application.port.in.SolicitudUseCase.ImagenAdjunta;
import com.handgelus.application.port.in.modelo.ObraVista;
import com.handgelus.application.port.in.modelo.ResumenPago;
import com.handgelus.application.port.in.modelo.SolicitudDetalle;
import com.handgelus.application.port.out.AlmacenamientoArchivosPort;
import com.handgelus.application.port.out.AvanceRepositoryPort;
import com.handgelus.application.port.out.ObraRepositoryPort;
import com.handgelus.application.port.out.SolicitudRepositoryPort;
import com.handgelus.application.port.out.UsuarioRepositoryPort;
import com.handgelus.application.service.soporte.EnsambladorDetalle;
import com.handgelus.application.service.soporte.Notificador;
import com.handgelus.domain.exception.ReglaNegocioException;
import com.handgelus.domain.model.Actor;
import com.handgelus.domain.model.EstadoObra;
import com.handgelus.domain.model.EstadoPagoServicio;
import com.handgelus.domain.model.EstadoSolicitud;
import com.handgelus.domain.model.Obra;
import com.handgelus.domain.model.ParametrosNegocio;
import com.handgelus.domain.model.RegistroAvance;
import com.handgelus.domain.model.Rol;
import com.handgelus.domain.model.Solicitud;
import com.handgelus.domain.model.Usuario;
import com.handgelus.domain.service.ReglasPago;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** F-07 · Actualizar avance de obra (RF-12, RF-13, RF-14 · SWR-18/19/20 · RN-10, RN-11). */
public class AvanceObraService implements AvanceObraUseCase {

    private static final Set<EstadoSolicitud> EN_CURSO = EnumSet.of(EstadoSolicitud.PAGO_PARCIAL_CONFIRMADO,
            EstadoSolicitud.CITA_AGENDADA, EstadoSolicitud.EN_PRODUCCION, EstadoSolicitud.TERMINADA);
    private static final Set<String> TIPOS_IMAGEN = Set.of("image/png", "image/jpeg", "image/webp");

    private final SolicitudRepositoryPort solicitudes;
    private final ObraRepositoryPort obras;
    private final AvanceRepositoryPort avances;
    private final UsuarioRepositoryPort usuarios;
    private final AlmacenamientoArchivosPort almacenamiento;
    private final EnsambladorDetalle ensamblador;
    private final Notificador notificador;
    private final ParametrosNegocio parametros;
    private final Clock reloj;

    public AvanceObraService(SolicitudRepositoryPort solicitudes, ObraRepositoryPort obras,
                             AvanceRepositoryPort avances, UsuarioRepositoryPort usuarios,
                             AlmacenamientoArchivosPort almacenamiento, EnsambladorDetalle ensamblador,
                             Notificador notificador, ParametrosNegocio parametros, Clock reloj) {
        this.solicitudes = solicitudes;
        this.obras = obras;
        this.avances = avances;
        this.usuarios = usuarios;
        this.almacenamiento = almacenamiento;
        this.ensamblador = ensamblador;
        this.notificador = notificador;
        this.parametros = parametros;
        this.reloj = reloj;
    }

    @Override
    public SolicitudDetalle cambiarEstadoObra(Long solicitudId, EstadoObra estado, String guiaEnvio, Actor actor) {
        actor.exigir(Rol.TATUADOR, Rol.ASISTENTE, Rol.ADMINISTRADOR);
        if (estado == null) {
            throw new ReglaNegocioException("Seleccione el nuevo estado de la obra.");
        }
        Solicitud solicitud = ensamblador.solicitud(solicitudId);
        if (!solicitud.esComision()) {
            throw new ReglaNegocioException("Solo las obras por comisión tienen estados de producción.");
        }
        Obra obra = obras.buscarPorSolicitud(solicitudId).orElseThrow(() -> new ReglaNegocioException("RN-10",
                "La obra solo puede iniciar producción después de confirmar el anticipo del 50%."));
        ResumenPago resumen = ensamblador.resumenPago(solicitud);
        LocalDateTime ahora = LocalDateTime.now(reloj);

        switch (estado) {
            case EN_PRODUCCION -> {
                if (!resumen.pagoInicialConfirmado()) {
                    throw new ReglaNegocioException("RN-10", "El anticipo aún no ha sido confirmado.");
                }
                obra.cambiarEstado(estado, ahora);
                solicitud.cambiarEstado(EstadoSolicitud.EN_PRODUCCION, ahora);
            }
            case TERMINADA -> {
                obra.cambiarEstado(estado, ahora);
                solicitud.cambiarEstado(EstadoSolicitud.TERMINADA, ahora);
            }
            case ENVIADA -> {
                if (resumen.estado() != EstadoPagoServicio.COMPLETO) {   // SWR-20 · RN-11
                    throw new ReglaNegocioException("SWR-20", "La obra no puede marcarse como enviada: hay un saldo "
                            + "pendiente de $" + ReglasPago.formato(resumen.saldo()) + ".");
                }
                obra.cambiarEstado(estado, ahora);
                obra.registrarGuia(guiaEnvio);
                solicitud.cambiarEstado(EstadoSolicitud.ENVIADA, ahora);
            }
            default -> throw new ReglaNegocioException("La obra no puede regresar al estado 'En espera'.");
        }
        obras.guardar(obra);
        solicitudes.guardar(solicitud);
        avances.guardar(RegistroAvance.nuevo(solicitudId, actor.id(), "Obra " + estado.getEtiqueta().toLowerCase(),
                estado == EstadoObra.ENVIADA && guiaEnvio != null && !guiaEnvio.isBlank()
                        ? "Guía de envío: " + guiaEnvio.trim() : null,
                obra.getProgreso(), null, ahora));

        Usuario cliente = ensamblador.usuario(solicitud.getClienteId());
        String mensaje = switch (estado) {
            case EN_PRODUCCION -> "¡Tu obra \"" + solicitud.getTitulo() + "\" entró en producción! Podrás seguir su "
                    + "avance en tiempo real desde HandgelusApp.";
            case TERMINADA -> "Tu obra \"" + solicitud.getTitulo() + "\" está terminada. "
                    + (resumen.saldo().signum() > 0
                    ? "Para coordinar el envío, realiza el pago del saldo restante: $" + ReglasPago.formato(resumen.saldo()) + "."
                    : "Coordinaremos el envío a " + solicitud.getCiudadDestino() + ".");
            case ENVIADA -> "Tu obra \"" + solicitud.getTitulo() + "\" va en camino a " + solicitud.getDireccionDestino()
                    + ", " + solicitud.getCiudadDestino() + "."
                    + (guiaEnvio == null || guiaEnvio.isBlank() ? "" : " Número de guía: " + guiaEnvio.trim() + ".");
            default -> "";
        };
        notificador.correoCliente(cliente, "Handgelus · Tu obra: " + estado.getEtiqueta(), mensaje, solicitudId);
        return ensamblador.detalle(solicitud);
    }

    @Override
    public SolicitudDetalle registrarAvance(RegistrarAvanceComando c, Actor actor) {
        actor.exigir(Rol.TATUADOR, Rol.ASISTENTE, Rol.ADMINISTRADOR);
        Solicitud solicitud = ensamblador.solicitud(c.solicitudId());
        if (!EN_CURSO.contains(solicitud.getEstado())) {
            throw new ReglaNegocioException("Solo se registra avance en servicios en curso (con pago inicial confirmado).");
        }
        Integer progreso = c.progreso();
        if (solicitud.esComision()) {
            Obra obra = obras.buscarPorSolicitud(solicitud.getId())
                    .orElseThrow(() -> new ReglaNegocioException("RN-10", "La obra aún no tiene anticipo confirmado."));
            if (progreso != null) {
                obra.actualizarProgreso(progreso);
                obras.guardar(obra);
            }
        } else if (progreso != null && (progreso < 0 || progreso > 100)) {
            throw new ReglaNegocioException("El progreso debe estar entre 0 y 100.");
        }
        String imagen = guardarImagen(c.imagen());
        avances.guardar(RegistroAvance.nuevo(solicitud.getId(), actor.id(), c.titulo(), c.descripcion(), progreso,
                imagen, LocalDateTime.now(reloj)));
        notificador.correoCliente(ensamblador.usuario(solicitud.getClienteId()),
                "Handgelus · Nuevo avance en tu proyecto",
                "El artista registró un nuevo avance en \"" + solicitud.getTitulo() + "\": " + c.titulo()
                        + (progreso != null ? " (" + progreso + "%)" : "") + ".", solicitud.getId());
        return ensamblador.detalle(solicitud);
    }

    private String guardarImagen(ImagenAdjunta imagen) {
        if (imagen == null || imagen.contenido() == null || imagen.contenido().length == 0) {
            return null;
        }
        if (imagen.contenido().length > parametros.imagenMaxBytes()) {
            throw new ReglaNegocioException("La imagen supera el máximo de 5 MB.");
        }
        if (imagen.contentType() == null || !TIPOS_IMAGEN.contains(imagen.contentType().toLowerCase())) {
            throw new ReglaNegocioException("Adjunte una imagen PNG, JPG o WEBP.");
        }
        return almacenamiento.guardar(imagen.nombreOriginal(), imagen.contentType(), imagen.contenido());
    }

    @Override
    public List<ObraVista> proyectosEnCurso(Actor actor) {
        Long clienteId = actor.esCliente() ? actor.id() : null;
        List<Solicitud> lista = solicitudes.buscar(EN_CURSO, null, clienteId);
        if (lista.isEmpty()) {
            return List.of();
        }
        Map<Long, Usuario> clientes = usuarios.buscarPorIds(
                        lista.stream().map(Solicitud::getClienteId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Usuario::getId, Function.identity()));
        return lista.stream().map(s -> {
            List<RegistroAvance> bitacora = avances.listarPorSolicitud(s.getId());
            RegistroAvance ultimo = bitacora.stream().max(Comparator.comparing(RegistroAvance::getFecha)).orElse(null);
            Usuario cliente = clientes.get(s.getClienteId());
            return new ObraVista(s, obras.buscarPorSolicitud(s.getId()).orElse(null),
                    cliente == null ? "—" : cliente.getNombre(), ultimo, bitacora.size());
        }).toList();
    }
}
