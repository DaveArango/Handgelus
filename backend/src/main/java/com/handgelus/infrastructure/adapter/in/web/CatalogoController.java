package com.handgelus.infrastructure.adapter.in.web;

import com.handgelus.domain.model.CategoriaTamano;
import com.handgelus.domain.model.EstadoObra;
import com.handgelus.domain.model.EstadoSolicitud;
import com.handgelus.domain.model.Estilo;
import com.handgelus.domain.model.Jornada;
import com.handgelus.domain.model.MetodoPago;
import com.handgelus.domain.model.ParametrosNegocio;
import com.handgelus.domain.model.Rol;
import com.handgelus.domain.model.TipoServicio;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Catálogos (listas preestablecidas) con sus etiquetas en español para el frontend. */
@RestController
public class CatalogoController {

    private final ParametrosNegocio parametros;

    public CatalogoController(ParametrosNegocio parametros) {
        this.parametros = parametros;
    }

    public record Opcion(String valor, String etiqueta, String detalle) {
    }

    @GetMapping("/api/catalogos")
    public Map<String, Object> catalogos() {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("tiposServicio", Arrays.stream(TipoServicio.values()).map(t -> new Opcion(t.name(), t.getEtiqueta(), null)).toList());
        c.put("categoriasTatuaje", Arrays.stream(CategoriaTamano.values())
                .map(t -> new Opcion(t.name(), t.getEtiquetaTatuaje(), null)).toList());
        c.put("formatosComision", Arrays.stream(CategoriaTamano.values())
                .map(t -> new Opcion(t.name(), t.getEtiquetaComision(), null)).toList());
        c.put("estilos", Arrays.stream(Estilo.values()).map(e -> new Opcion(e.name(), e.getEtiqueta(),
                e.getFactor().toPlainString())).toList());
        c.put("estados", Arrays.stream(EstadoSolicitud.values()).map(e -> new Opcion(e.name(), e.getEtiqueta(), null)).toList());
        c.put("estadosObra", Arrays.stream(EstadoObra.values()).map(e -> new Opcion(e.name(), e.getEtiqueta(), null)).toList());
        c.put("jornadas", Arrays.stream(Jornada.values()).map(j -> new Opcion(j.name(), j.getEtiqueta(),
                j.getHoraInicio() + " – " + j.getHoraFin())).toList());
        c.put("metodosPago", Arrays.stream(MetodoPago.values()).map(m -> new Opcion(m.name(), m.getEtiqueta(),
                m.usaPasarela() ? "pasarela" : "manual")).toList());
        c.put("roles", Arrays.stream(Rol.values()).map(r -> new Opcion(r.name(), r.getEtiqueta(), null)).toList());
        c.put("parametros", Map.of(
                "porcentajeAbonoTatuaje", parametros.porcentajeAbonoTatuaje(),
                "porcentajeAnticipoComision", parametros.porcentajeAnticipoComision(),
                "horasExpiracionPago", parametros.horasExpiracionPago(),
                "maxCitasPorDia", parametros.maxCitasPorDia(),
                "imagenMaxBytes", parametros.imagenMaxBytes(),
                "ciudadLocal", parametros.ciudadLocal()));
        return c;
    }
}
