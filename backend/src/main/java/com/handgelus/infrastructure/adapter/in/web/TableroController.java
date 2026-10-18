package com.handgelus.infrastructure.adapter.in.web;

import com.handgelus.application.port.in.TableroUseCase;
import com.handgelus.application.port.in.modelo.TableroVista;
import com.handgelus.domain.model.Actor;
import com.handgelus.domain.model.Notificacion;
import com.handgelus.domain.model.TipoNotificacion;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Panel del estudio: indicadores y bandeja de alertas / correos enviados. */
@RestController
public class TableroController {

    private final TableroUseCase tablero;

    public TableroController(TableroUseCase tablero) {
        this.tablero = tablero;
    }

    @GetMapping("/api/tablero")
    public TableroVista tablero(Actor actor) {
        return tablero.tablero(actor);
    }

    @GetMapping("/api/notificaciones")
    public List<Notificacion> notificaciones(@RequestParam(required = false) TipoNotificacion tipo, Actor actor) {
        return tablero.notificaciones(tipo, actor);
    }

    @PostMapping("/api/notificaciones/{id}/leida")
    public ResponseEntity<Void> leida(@PathVariable Long id, Actor actor) {
        tablero.marcarNotificacionLeida(id, actor);
        return ResponseEntity.noContent().build();
    }
}
