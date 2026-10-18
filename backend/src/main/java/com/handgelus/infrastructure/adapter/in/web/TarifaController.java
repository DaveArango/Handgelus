package com.handgelus.infrastructure.adapter.in.web;

import com.handgelus.application.port.in.CotizacionUseCase;
import com.handgelus.application.port.in.GestionTarifasUseCase;
import com.handgelus.application.port.in.GestionTarifasUseCase.TarifaComando;
import com.handgelus.domain.model.Actor;
import com.handgelus.domain.model.CategoriaTamano;
import com.handgelus.domain.model.Estilo;
import com.handgelus.domain.model.ResultadoCotizacion;
import com.handgelus.domain.model.Tarifa;
import com.handgelus.domain.model.TipoServicio;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/** F-02 · Configurar tabla de tarifas · F-04 estimación referencial. */
@RestController
public class TarifaController {

    private final GestionTarifasUseCase tarifas;
    private final CotizacionUseCase cotizaciones;

    public TarifaController(GestionTarifasUseCase tarifas, CotizacionUseCase cotizaciones) {
        this.tarifas = tarifas;
        this.cotizaciones = cotizaciones;
    }

    public record TarifaRequest(@NotNull(message = "Seleccione el tipo de servicio") TipoServicio tipoServicio,
                                @NotNull(message = "Seleccione el tamaño") CategoriaTamano categoria,
                                @NotNull(message = "Ingrese el valor base") @DecimalMin(value = "1", message = "Debe ser mayor a cero") BigDecimal valorBase,
                                @NotNull(message = "Ingrese el valor por hora") @DecimalMin(value = "0", message = "No puede ser negativo") BigDecimal valorHora,
                                @Min(value = 0, message = "Mínimo 0") @Max(value = 80, message = "Máximo 80") int horasEstimadas,
                                boolean tarifaFija,
                                String descripcion) {
    }

    @GetMapping("/api/tarifas")
    public List<Tarifa> listar() {
        return tarifas.listar();
    }

    @PutMapping("/api/tarifas")
    public Tarifa guardar(@Valid @RequestBody TarifaRequest r, Actor actor) {
        return tarifas.guardar(new TarifaComando(r.tipoServicio(), r.categoria(), r.valorBase(), r.valorHora(),
                r.horasEstimadas(), r.tarifaFija(), r.descripcion()), actor);
    }

    @GetMapping("/api/cotizaciones/estimar")
    public ResultadoCotizacion estimar(@RequestParam TipoServicio tipo, @RequestParam CategoriaTamano categoria,
                                       @RequestParam(required = false) Estilo estilo,
                                       @RequestParam(required = false) String ciudad) {
        return cotizaciones.estimar(tipo, categoria, estilo, ciudad);
    }
}
