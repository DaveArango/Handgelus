package com.handgelus.application.service;

import com.handgelus.application.port.in.AgendaUseCase;
import com.handgelus.application.port.in.modelo.CitaAgenda;
import com.handgelus.application.port.in.modelo.DisponibilidadDia;
import com.handgelus.application.port.in.modelo.ResumenPago;
import com.handgelus.application.port.in.modelo.SolicitudDetalle;
import com.handgelus.application.port.out.AvanceRepositoryPort;
import com.handgelus.application.port.out.CitaRepositoryPort;
import com.handgelus.application.port.out.SolicitudRepositoryPort;
import com.handgelus.application.port.out.UsuarioRepositoryPort;
import com.handgelus.application.service.soporte.EmisorComprobantes;
import com.handgelus.application.service.soporte.EnsambladorDetalle;
import com.handgelus.application.service.soporte.Notificador;
import com.handgelus.domain.exception.RecursoNoEncontradoException;
import com.handgelus.domain.exception.ReglaNegocioException;
import com.handgelus.domain.model.Actor;
import com.handgelus.domain.model.Cita;
import com.handgelus.domain.model.EstadoCita;
import com.handgelus.domain.model.EstadoPagoServicio;
import com.handgelus.domain.model.EstadoSolicitud;
import com.handgelus.domain.model.Jornada;
import com.handgelus.domain.model.ParametrosNegocio;
import com.handgelus.domain.model.RegistroAvance;
import com.handgelus.domain.model.Rol;
import com.handgelus.domain.model.Solicitud;
import com.handgelus.domain.model.TipoComprobante;
import com.handgelus.domain.model.Usuario;
import com.handgelus.domain.service.ReglasPago;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** F-06 · Agendar cita de tatuaje (CU-03 · RF-10, RF-11 · SWR-15/16/17 · RN-01, RN-05, RN-07). */
public class AgendaService implements AgendaUseCase {

    private static final DateTimeFormatter FECHA_LARGA =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-CO"));
    private static final int MAX_DIAS_CONSULTA = 92;

    private final CitaRepositoryPort citas;
    private final SolicitudRepositoryPort solicitudes;
    private final UsuarioRepositoryPort usuarios;
    private final AvanceRepositoryPort avances;
    private final EnsambladorDetalle ensamblador;
    private final EmisorComprobantes emisor;
    private final Notificador notificador;
    private final ParametrosNegocio parametros;
    private final Clock reloj;

    public AgendaService(CitaRepositoryPort citas, SolicitudRepositoryPort solicitudes, UsuarioRepositoryPort usuarios,
                         AvanceRepositoryPort avances, EnsambladorDetalle ensamblador, EmisorComprobantes emisor,
                         Notificador notificador, ParametrosNegocio parametros, Clock reloj) {
        this.citas = citas;
        this.solicitudes = solicitudes;
        this.usuarios = usuarios;
        this.avances = avances;
        this.ensamblador = ensamblador;
        this.emisor = emisor;
        this.notificador = notificador;
        this.parametros = parametros;
        this.reloj = reloj;
    }

    @Override
    public List<DisponibilidadDia> disponibilidad(Long tatuadorId, LocalDate desde, LocalDate hasta) {
        Long tatuador = resolverTatuador(tatuadorId).getId();
        if (hasta.isBefore(desde)) {
            throw new ReglaNegocioException("El rango de fechas no es válido.");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) > MAX_DIAS_CONSULTA) {
            hasta = desde.plusDays(MAX_DIAS_CONSULTA);
        }
        Map<LocalDate, List<Cita>> porDia = citas.listarPorTatuadorEntre(tatuador, desde, hasta).stream()
                .filter(Cita::ocupaAgenda).collect(Collectors.groupingBy(Cita::getFecha));
        LocalDate hoy = LocalDate.now(reloj);
        List<DisponibilidadDia> dias = new ArrayList<>();
        for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) {
            dias.add(evaluarDia(d, porDia.getOrDefault(d, List.of()), hoy));
        }
        return dias;
    }

    /** RN-05 / RF-10: jornadas libres, máximo de citas por día y atelier cerrado los domingos. */
    private DisponibilidadDia evaluarDia(LocalDate dia, List<Cita> ocupadas, LocalDate hoy) {
        boolean cerrado = dia.getDayOfWeek() == DayOfWeek.SUNDAY || dia.isBefore(hoy);
        List<Jornada> libres = cerrado || ocupadas.size() >= parametros.maxCitasPorDia()
                ? List.of()
                : Arrays.stream(Jornada.values())
                .filter(j -> ocupadas.stream().noneMatch(c -> c.getJornada() == j)).toList();
        return new DisponibilidadDia(dia, libres, ocupadas.size(), !libres.isEmpty());
    }

    @Override
    public List<DisponibilidadDia> sugerirFechas(Long tatuadorId, LocalDate desde, int cantidad) {
        LocalDate inicio = desde.isBefore(LocalDate.now(reloj)) ? LocalDate.now(reloj) : desde;
        return disponibilidad(tatuadorId, inicio, inicio.plusDays(60)).stream()
                .filter(DisponibilidadDia::disponible).limit(Math.max(1, cantidad)).toList();
    }

    @Override
    public List<CitaAgenda> citas(LocalDate desde, LocalDate hasta, Long tatuadorId, Actor actor) {
        actor.exigir(Rol.ASISTENTE, Rol.TATUADOR, Rol.ADMINISTRADOR);
        List<Cita> lista = tatuadorId == null
                ? citas.listarEntre(desde, hasta)
                : citas.listarPorTatuadorEntre(tatuadorId, desde, hasta);
        return ensamblador.citasAgenda(lista.stream().filter(Cita::ocupaAgenda).toList());
    }

    @Override
    public SolicitudDetalle agendar(AgendarCitaComando c, Actor actor) {
        actor.exigir(Rol.ASISTENTE, Rol.TATUADOR, Rol.ADMINISTRADOR);
        Solicitud solicitud = ensamblador.solicitud(c.solicitudId());
        if (!solicitud.esTatuaje()) {
            throw new ReglaNegocioException("Solo los servicios de tatuaje requieren cita.");
        }
        solicitud.exigirEstado(EstadoSolicitud.PAGO_PARCIAL_CONFIRMADO, EstadoSolicitud.CITA_AGENDADA);
        ResumenPago resumen = ensamblador.resumenPago(solicitud);
        if (!resumen.pagoInicialConfirmado()) {   // RN-01 · SWR-17
            throw new ReglaNegocioException("RN-01",
                    "No se puede agendar la cita: el abono aún no ha sido confirmado por la pasarela de pago.");
        }
        Usuario tatuador = resolverTatuador(c.tatuadorId());
        if (c.fecha() == null || c.jornada() == null) {
            throw new ReglaNegocioException("Seleccione la fecha y la jornada de la cita.");
        }
        if (c.fecha().isBefore(LocalDate.now(reloj))) {
            throw new ReglaNegocioException("No es posible agendar citas en fechas pasadas.");
        }

        // CU-03 3.a / 3.b: jornada ocupada o día lleno
        List<Cita> delDia = citas.listarPorTatuadorEntre(tatuador.getId(), c.fecha(), c.fecha()).stream()
                .filter(Cita::ocupaAgenda).toList();
        if (c.fecha().getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new ReglaNegocioException("RN-05", "El atelier no atiende los domingos." + sugerencias(tatuador, c.fecha()));
        }
        if (delDia.size() >= parametros.maxCitasPorDia()) {
            throw new ReglaNegocioException("SWR-15", "Ya existen " + parametros.maxCitasPorDia()
                    + " citas ese día." + sugerencias(tatuador, c.fecha()));
        }
        if (delDia.stream().anyMatch(x -> x.getJornada() == c.jornada())) {
            throw new ReglaNegocioException("RN-05", "La jornada seleccionada ya está ocupada."
                    + sugerencias(tatuador, c.fecha()));
        }

        LocalDateTime ahora = LocalDateTime.now(reloj);
        int sesion = (int) citas.listarPorSolicitud(solicitud.getId()).stream().filter(Cita::ocupaAgenda).count() + 1;
        Cita cita = citas.guardar(Cita.programar(solicitud.getId(), tatuador.getId(), c.fecha(), c.jornada(), sesion,
                c.notas(), ahora));
        if (solicitud.getEstado() == EstadoSolicitud.PAGO_PARCIAL_CONFIRMADO) {
            solicitud.cambiarEstado(EstadoSolicitud.CITA_AGENDADA, ahora);
            solicitudes.guardar(solicitud);
        }

        // RF-11 · SWR-16: comprobante con fecha, hora y valor total
        Usuario cliente = ensamblador.usuario(solicitud.getClienteId());
        String detalle = "Sesión " + sesion + " con " + tatuador.getNombre() + " el " + FECHA_LARGA.format(cita.getFecha())
                + ", jornada de la " + cita.getJornada().getEtiqueta().toLowerCase() + " ("
                + cita.getJornada().getHoraInicio() + " – " + cita.getJornada().getHoraFin() + "). "
                + "Valor total del servicio: $" + ReglasPago.formato(resumen.valorTotal())
                + ". Saldo pendiente: $" + ReglasPago.formato(resumen.saldo()) + ".";
        emisor.emitir(solicitud, cliente, TipoComprobante.CITA_AGENDADA, detalle, resumen.valorTotal());
        return ensamblador.detalle(solicitud);
    }

    private String sugerencias(Usuario tatuador, LocalDate desde) {
        List<DisponibilidadDia> dias = sugerirFechas(tatuador.getId(), desde == null ? LocalDate.now(reloj) : desde, 3);
        if (dias.isEmpty()) {
            return "";
        }
        return " Fechas disponibles cercanas: " + dias.stream()
                .map(d -> d.fecha() + " (" + d.jornadasLibres().stream().map(Jornada::getEtiqueta)
                        .collect(Collectors.joining(", ")) + ")")
                .collect(Collectors.joining("; ")) + ".";
    }

    @Override
    public CitaAgenda marcarRealizada(Long citaId, Actor actor) {
        actor.exigir(Rol.ASISTENTE, Rol.TATUADOR, Rol.ADMINISTRADOR);
        Cita cita = buscarCita(citaId);
        cita.marcarRealizada();
        citas.guardar(cita);
        avances.guardar(RegistroAvance.nuevo(cita.getSolicitudId(), actor.id(), "Sesión " + cita.getNumeroSesion()
                        + " realizada", "Sesión del " + cita.getFecha() + " completada en el atelier.", null, null,
                LocalDateTime.now(reloj)));
        return ensamblador.citasAgenda(List.of(cita)).get(0);
    }

    @Override
    public CitaAgenda cancelarCita(Long citaId, Actor actor) {
        actor.exigir(Rol.ASISTENTE, Rol.TATUADOR, Rol.ADMINISTRADOR);
        Cita cita = buscarCita(citaId);
        cita.cancelar();
        citas.guardar(cita);
        Solicitud solicitud = ensamblador.solicitud(cita.getSolicitudId());
        boolean quedanCitas = citas.listarPorSolicitud(solicitud.getId()).stream().anyMatch(Cita::ocupaAgenda);
        if (!quedanCitas && solicitud.getEstado() == EstadoSolicitud.CITA_AGENDADA) {
            solicitud.cambiarEstado(EstadoSolicitud.PAGO_PARCIAL_CONFIRMADO, LocalDateTime.now(reloj));
            solicitudes.guardar(solicitud);
        }
        notificador.correoCliente(ensamblador.usuario(solicitud.getClienteId()), "Handgelus · Cita reprogramada",
                "Tu cita del " + FECHA_LARGA.format(cita.getFecha()) + " fue cancelada. Nos comunicaremos contigo "
                        + "para acordar una nueva fecha. Recuerda que las cancelaciones con menos de 24 horas de "
                        + "anticipación no son reembolsables (RN-06).", solicitud.getId());
        return ensamblador.citasAgenda(List.of(cita)).get(0);
    }

    @Override
    public SolicitudDetalle completarServicio(Long solicitudId, Actor actor) {
        actor.exigir(Rol.ASISTENTE, Rol.TATUADOR, Rol.ADMINISTRADOR);
        Solicitud solicitud = ensamblador.solicitud(solicitudId);
        solicitud.exigirEstado(EstadoSolicitud.CITA_AGENDADA);
        List<Cita> lista = citas.listarPorSolicitud(solicitudId);
        if (lista.stream().noneMatch(x -> x.getEstado() == EstadoCita.REALIZADA)) {
            throw new ReglaNegocioException("Marque como realizada al menos una sesión antes de cerrar el servicio.");
        }
        if (lista.stream().anyMatch(x -> x.getEstado() == EstadoCita.PROGRAMADA)) {
            throw new ReglaNegocioException("Aún hay sesiones programadas para este servicio.");
        }
        ResumenPago resumen = ensamblador.resumenPago(solicitud);
        if (resumen.estado() != EstadoPagoServicio.COMPLETO) {   // RN-07
            throw new ReglaNegocioException("RN-07", "El pago total debe estar completo para cerrar el servicio. "
                    + "Saldo pendiente: $" + ReglasPago.formato(resumen.saldo()) + ".");
        }
        solicitud.cambiarEstado(EstadoSolicitud.SERVICIO_COMPLETADO, LocalDateTime.now(reloj));
        solicitudes.guardar(solicitud);
        notificador.correoCliente(ensamblador.usuario(solicitud.getClienteId()), "Handgelus · Gracias por confiar en nosotros",
                "Tu servicio \"" + solicitud.getTitulo() + "\" quedó cerrado. Recuerda seguir las indicaciones de "
                        + "cuidado y cicatrización. ¡Fue un honor plasmar tu idea!", solicitudId);
        return ensamblador.detalle(solicitud);
    }

    private Cita buscarCita(Long id) {
        return citas.buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("La cita", id));
    }

    private Usuario resolverTatuador(Long tatuadorId) {
        if (tatuadorId != null) {
            Usuario u = usuarios.buscarPorId(tatuadorId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("El tatuador", tatuadorId));
            if (u.getRol() != Rol.TATUADOR || !u.isActivo()) {
                throw new ReglaNegocioException("El usuario seleccionado no es un tatuador activo.");
            }
            return u;
        }
        return usuarios.listarPorRol(Rol.TATUADOR).stream().filter(Usuario::isActivo).findFirst()
                .orElseThrow(() -> new ReglaNegocioException("No hay tatuadores activos registrados."));
    }
}
