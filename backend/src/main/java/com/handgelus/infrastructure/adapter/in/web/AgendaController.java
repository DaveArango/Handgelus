package com.handgelus.infrastructure.adapter.in.web;

import com.handgelus.application.port.in.AgendaUseCase;
import com.handgelus.application.port.in.AgendaUseCase.AgendarCitaComando;
import com.handgelus.application.port.in.modelo.CitaAgenda;
import com.handgelus.application.port.in.modelo.DisponibilidadDia;
import com.handgelus.application.port.in.modelo.SolicitudDetalle;
import com.handgelus.domain.model.Actor;
import com.handgelus.domain.model.Jornada;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** F-06 · Agendar cita de tatuaje (CU-03). */
@RestController
public class AgendaController {

    private final AgendaUseCase agenda;

    public AgendaController(AgendaUseCase agenda) {
        this.agenda = agenda;
    }

    public record AgendarRequest(@NotNull(message = "Seleccione la solicitud") Long solicitudId,
                                 Long tatuadorId,
                                 @NotNull(message = "Seleccione la fecha") LocalDate fecha,
                                 @NotNull(message = "Seleccione la jornada") Jornada jornada,
                                 @Size(max = 500) String notas) {
    }

    @GetMapping("/api/agenda/disponibilidad")
    public List<DisponibilidadDia> disponibilidad(@RequestParam(required = false) Long tatuadorId,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return agenda.disponibilidad(tatuadorId, desde, hasta);
    }

    @GetMapping("/api/agenda/sugerencias")
    public List<DisponibilidadDia> sugerencias(@RequestParam(required = false) Long tatuadorId,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                               @RequestParam(defaultValue = "5") int cantidad) {
        return agenda.sugerirFechas(tatuadorId, desde, cantidad);
    }

    @GetMapping("/api/agenda/citas")
    public List<CitaAgenda> citas(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                  @RequestParam(required = false) Long tatuadorId, Actor actor) {
        return agenda.citas(desde, hasta, tatuadorId, actor);
    }

    @PostMapping("/api/agenda/citas")
    public SolicitudDetalle agendar(@Valid @RequestBody AgendarRequest r, Actor actor) {
        return agenda.agendar(new AgendarCitaComando(r.solicitudId(), r.tatuadorId(), r.fecha(), r.jornada(), r.notas()),
                actor);
    }

    @PostMapping("/api/agenda/citas/{id}/realizada")
    public CitaAgenda realizada(@PathVariable Long id, Actor actor) {
        return agenda.marcarRealizada(id, actor);
    }

    @PostMapping("/api/agenda/citas/{id}/cancelar")
    public CitaAgenda cancelar(@PathVariable Long id, Actor actor) {
        return agenda.cancelarCita(id, actor);
    }

    @PostMapping("/api/solicitudes/{id}/completar")
    public SolicitudDetalle completar(@PathVariable Long id, Actor actor) {
        return agenda.completarServicio(id, actor);
    }
}
