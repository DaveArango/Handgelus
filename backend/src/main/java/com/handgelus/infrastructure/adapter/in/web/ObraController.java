package com.handgelus.infrastructure.adapter.in.web;

import com.handgelus.application.port.in.AvanceObraUseCase;
import com.handgelus.application.port.in.AvanceObraUseCase.RegistrarAvanceComando;
import com.handgelus.application.port.in.modelo.ObraVista;
import com.handgelus.application.port.in.modelo.SolicitudDetalle;
import com.handgelus.domain.model.Actor;
import com.handgelus.domain.model.EstadoObra;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** F-07 · Actualizar avance de obra. */
@RestController
public class ObraController {

    private final AvanceObraUseCase avances;

    public ObraController(AvanceObraUseCase avances) {
        this.avances = avances;
    }

    public record EstadoObraRequest(@NotNull(message = "Seleccione el estado") EstadoObra estado,
                                    @Size(max = 80) String guiaEnvio) {
    }

    public record AvanceRequest(@NotBlank(message = "El avance necesita un título") @Size(max = 150) String titulo,
                                @Size(max = 1500) String descripcion,
                                @Min(value = 0, message = "Mínimo 0") @Max(value = 100, message = "Máximo 100") Integer progreso) {
    }

    @GetMapping("/api/proyectos")
    public List<ObraVista> proyectos(Actor actor) {
        return avances.proyectosEnCurso(actor);
    }

    @PostMapping("/api/solicitudes/{id}/obra/estado")
    public SolicitudDetalle cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoObraRequest r, Actor actor) {
        return avances.cambiarEstadoObra(id, r.estado(), r.guiaEnvio(), actor);
    }

    @PostMapping(value = "/api/solicitudes/{id}/avances", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SolicitudDetalle registrarAvance(@PathVariable Long id, @Valid @RequestPart("datos") AvanceRequest r,
                                            @RequestPart(value = "imagen", required = false) MultipartFile imagen,
                                            Actor actor) {
        return avances.registrarAvance(new RegistrarAvanceComando(id, r.titulo(), r.descripcion(), r.progreso(),
                SolicitudController.adjunto(imagen)), actor);
    }
}
