package com.handgelus.infrastructure.adapter.in.web;

import com.handgelus.application.port.in.CotizacionUseCase;
import com.handgelus.application.port.in.SolicitudUseCase;
import com.handgelus.application.port.in.SolicitudUseCase.ImagenAdjunta;
import com.handgelus.application.port.in.SolicitudUseCase.RegistrarSolicitudComando;
import com.handgelus.application.port.in.modelo.SolicitudDetalle;
import com.handgelus.application.port.in.modelo.SolicitudResumen;
import com.handgelus.application.port.out.AlmacenamientoArchivosPort.ArchivoAlmacenado;
import com.handgelus.domain.model.Actor;
import com.handgelus.domain.model.CategoriaTamano;
import com.handgelus.domain.model.EstadoSolicitud;
import com.handgelus.domain.model.Estilo;
import com.handgelus.domain.model.TipoServicio;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

/** F-03 · Registrar solicitud de servicio · F-04 confirmar/decidir cotización. */
@RestController
@RequestMapping("/api")
public class SolicitudController {

    private final SolicitudUseCase solicitudes;
    private final CotizacionUseCase cotizaciones;

    public SolicitudController(SolicitudUseCase solicitudes, CotizacionUseCase cotizaciones) {
        this.solicitudes = solicitudes;
        this.cotizaciones = cotizaciones;
    }

    public record SolicitudRequest(@NotNull(message = "Seleccione el tipo de servicio") TipoServicio tipoServicio,
                                   @NotBlank(message = "Asigne un nombre a su proyecto") @Size(max = 150) String titulo,
                                   @NotBlank(message = "Describa su idea") @Size(min = 10, max = 2000, message = "Entre 10 y 2000 caracteres") String descripcion,
                                   @NotNull(message = "Seleccione un tamaño") CategoriaTamano categoria,
                                   Estilo estilo,
                                   @Size(max = 120) String zonaCuerpo,
                                   @Size(max = 250) String direccionDestino,
                                   @Size(max = 100) String ciudadDestino) {
    }

    public record CancelarRequest(String motivo) {
    }

    public record ConfirmarCotizacionRequest(@DecimalMin(value = "1", message = "Debe ser mayor a cero") BigDecimal valorFinal,
                                             @Size(max = 1000) String observaciones) {
    }

    public record DecisionRequest(boolean acepta) {
    }

    @PostMapping(value = "/solicitudes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudDetalle registrar(@Valid @RequestPart("datos") SolicitudRequest r,
                                      @RequestPart(value = "imagen", required = false) MultipartFile imagen,
                                      Actor actor) {
        return solicitudes.registrar(new RegistrarSolicitudComando(r.tipoServicio(), r.titulo(), r.descripcion(),
                r.categoria(), r.estilo(), r.zonaCuerpo(), r.direccionDestino(), r.ciudadDestino(), adjunto(imagen)), actor);
    }

    @GetMapping("/solicitudes")
    public List<SolicitudResumen> listar(@RequestParam(required = false) EstadoSolicitud estado,
                                         @RequestParam(required = false) TipoServicio tipo,
                                         @RequestParam(required = false) String grupo, Actor actor) {
        return solicitudes.listar(estado, tipo, grupo, actor);
    }

    @GetMapping("/solicitudes/{id}")
    public SolicitudDetalle detalle(@PathVariable Long id, Actor actor) {
        return solicitudes.detalle(id, actor);
    }

    @PostMapping("/solicitudes/{id}/cancelar")
    public SolicitudDetalle cancelar(@PathVariable Long id, @RequestBody(required = false) CancelarRequest r, Actor actor) {
        return solicitudes.cancelar(id, r == null ? null : r.motivo(), actor);
    }

    @PostMapping("/solicitudes/{id}/cotizacion/confirmar")
    public SolicitudDetalle confirmarCotizacion(@PathVariable Long id, @Valid @RequestBody ConfirmarCotizacionRequest r,
                                                Actor actor) {
        return cotizaciones.confirmar(id, r.valorFinal(), r.observaciones(), actor);
    }

    @PostMapping("/solicitudes/{id}/cotizacion/decision")
    public SolicitudDetalle decidir(@PathVariable Long id, @RequestBody DecisionRequest r, Actor actor) {
        return cotizaciones.decidir(id, r.acepta(), actor);
    }

    @GetMapping("/archivos/{nombre}")
    public ResponseEntity<byte[]> archivo(@PathVariable String nombre, Actor actor) {
        ArchivoAlmacenado archivo = solicitudes.archivo(nombre, actor);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .body(archivo.contenido());
    }

    static ImagenAdjunta adjunto(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            return null;
        }
        try {
            return new ImagenAdjunta(archivo.getOriginalFilename(), archivo.getContentType(), archivo.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("No fue posible leer el archivo adjunto.", e);
        }
    }
}
