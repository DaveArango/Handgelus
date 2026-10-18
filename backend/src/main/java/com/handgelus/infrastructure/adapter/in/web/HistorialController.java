package com.handgelus.infrastructure.adapter.in.web;

import com.handgelus.application.port.in.HistorialUseCase;
import com.handgelus.application.port.in.modelo.ClienteResumen;
import com.handgelus.application.port.in.modelo.HistorialCliente;
import com.handgelus.domain.model.Actor;
import com.handgelus.domain.model.Comprobante;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** F-08 · Consultar historial de servicios. */
@RestController
public class HistorialController {

    private final HistorialUseCase historial;

    public HistorialController(HistorialUseCase historial) {
        this.historial = historial;
    }

    @GetMapping("/api/clientes")
    public List<ClienteResumen> clientes(@RequestParam(required = false) String q, Actor actor) {
        return historial.clientes(q, actor);
    }

    @GetMapping("/api/clientes/{id}/historial")
    public HistorialCliente historial(@PathVariable Long id, Actor actor) {
        return historial.historial(id, actor);
    }

    @GetMapping("/api/mi-historial")
    public HistorialCliente miHistorial(Actor actor) {
        return historial.historial(actor.id(), actor);
    }

    @GetMapping("/api/comprobantes/{id}")
    public Comprobante comprobante(@PathVariable Long id, Actor actor) {
        return historial.comprobante(id, actor);
    }
}
