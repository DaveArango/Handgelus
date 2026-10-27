package com.handgelus.infrastructure.adapter.in.web;

import com.handgelus.application.port.in.PagoUseCase;
import com.handgelus.domain.model.Actor;
import com.handgelus.domain.model.Pago;
import com.handgelus.infrastructure.config.HandgelusProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada que representa la pantalla de la pasarela simulada (entorno académico).
 * <p>
 * Actúa como lo haría la pasarela externa: conoce el secreto compartido del webhook (lo lee de la
 * configuración) y notifica el resultado al caso de uso a través de {@link PagoUseCase}.
 * No depende de ningún adaptador de salida, de modo que sustituir {@code PasarelaPagoPort} por una
 * pasarela real no obliga a tocar los controladores. Con una pasarela real se desactiva con
 * {@code handgelus.pasarela.simulada-habilitada=false}.
 */
@RestController
@RequestMapping("/api")
@ConditionalOnProperty(prefix = "handgelus.pasarela", name = "simulada-habilitada", havingValue = "true",
        matchIfMissing = true)
public class PasarelaSimuladaController {

    private final PagoUseCase pagos;
    private final String firmaWebhook;

    public PasarelaSimuladaController(PagoUseCase pagos, HandgelusProperties properties) {
        this.pagos = pagos;
        this.firmaWebhook = properties.pasarela().firmaWebhook();
    }

    /**
     * El cliente aprueba o rechaza el cobro y la pasarela simulada dispara su webhook firmado.
     * Con una pasarela real este endpoint no existe.
     */
    @PostMapping("/pasarela-simulada/{referencia}/{resultado}")
    public Pago pasarelaSimulada(@PathVariable String referencia, @PathVariable String resultado, Actor actor) {
        pagos.consultarCheckout(referencia, actor);   // valida que el pago pertenezca al usuario
        return pagos.procesarRespuestaPasarela(referencia, "aprobar".equalsIgnoreCase(resultado), firmaWebhook);
    }
}
