package com.handgelus.infrastructure.adapter.in.web;

import com.handgelus.application.port.in.PagoUseCase;
import com.handgelus.application.port.in.modelo.CheckoutVista;
import com.handgelus.application.port.in.modelo.PagoVista;
import com.handgelus.application.port.in.modelo.SolicitudDetalle;
import com.handgelus.domain.model.Actor;
import com.handgelus.domain.model.EstadoPago;
import com.handgelus.domain.model.MetodoPago;
import com.handgelus.domain.model.Pago;
import com.handgelus.infrastructure.adapter.out.pasarela.PasarelaPagoSimuladaAdapter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/** F-05 · Registrar pago de abono o anticipo (RF-08, RF-09, RF-14). */
@RestController
@RequestMapping("/api")
public class PagoController {

    private final PagoUseCase pagos;
    private final PasarelaPagoSimuladaAdapter pasarelaSimulada;

    public PagoController(PagoUseCase pagos, PasarelaPagoSimuladaAdapter pasarelaSimulada) {
        this.pagos = pagos;
        this.pasarelaSimulada = pasarelaSimulada;
    }

    public record IniciarPagoRequest(@DecimalMin(value = "1", message = "Debe ser mayor a cero") BigDecimal monto,
                                     MetodoPago metodo) {
    }

    public record PagoManualRequest(@NotNull(message = "Ingrese el monto") @DecimalMin(value = "1", message = "Debe ser mayor a cero") BigDecimal monto,
                                    MetodoPago metodo) {
    }

    public record WebhookRequest(@NotBlank String referencia, boolean aprobado) {
    }

    /** El cliente inicia el pago y recibe la URL de checkout de la pasarela. */
    @PostMapping("/solicitudes/{id}/pagos")
    public CheckoutVista iniciar(@PathVariable Long id, @Valid @RequestBody IniciarPagoRequest r, Actor actor) {
        return pagos.iniciarPago(id, r.monto(), r.metodo(), actor);
    }

    /** La asistente registra un pago recibido en el atelier (efectivo o transferencia). */
    @PostMapping("/solicitudes/{id}/pagos/manual")
    public SolicitudDetalle manual(@PathVariable Long id, @Valid @RequestBody PagoManualRequest r, Actor actor) {
        return pagos.registrarPagoManual(id, r.monto(), r.metodo(), actor);
    }

    @GetMapping("/pagos")
    public List<PagoVista> listar(@RequestParam(required = false) EstadoPago estado, Actor actor) {
        return pagos.listar(estado, actor);
    }

    @GetMapping("/pagos/checkout/{referencia}")
    public CheckoutVista checkout(@PathVariable String referencia, Actor actor) {
        return pagos.consultarCheckout(referencia, actor);
    }

    /** Webhook que invoca la pasarela externa para confirmar o rechazar la transacción (SWR-13). */
    @PostMapping("/pagos/webhook")
    public Pago webhook(@RequestHeader(value = "X-Firma-Pasarela", required = false) String firma,
                        @Valid @RequestBody WebhookRequest r) {
        return pagos.procesarRespuestaPasarela(r.referencia(), r.aprobado(), firma);
    }

    /**
     * Pantalla de la pasarela simulada: el cliente aprueba o rechaza el cobro y la pasarela
     * dispara internamente su webhook firmado. Con una pasarela real este endpoint no existe.
     */
    @PostMapping("/pasarela-simulada/{referencia}/{resultado}")
    public Pago pasarelaSimulada(@PathVariable String referencia, @PathVariable String resultado, Actor actor) {
        pagos.consultarCheckout(referencia, actor);   // valida que el pago pertenezca al usuario
        return pagos.procesarRespuestaPasarela(referencia, "aprobar".equalsIgnoreCase(resultado),
                pasarelaSimulada.firmaConfigurada());
    }
}
