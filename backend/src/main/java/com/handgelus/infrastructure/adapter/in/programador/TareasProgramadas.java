package com.handgelus.infrastructure.adapter.in.programador;

import com.handgelus.application.port.in.CotizacionUseCase;
import com.handgelus.application.port.in.PagoUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada temporal: dispara los casos de uso que dependen del paso del tiempo.
 * <ul>
 *   <li>SWR-14 / RF-09: pagos sin confirmar en 48 h → la solicitud vuelve a 'Cotización aceptada'.</li>
 *   <li>CU-02 4.a: cotizaciones sin confirmar en 72 h → alerta interna.</li>
 * </ul>
 */
@Component
public class TareasProgramadas {

    private static final Logger log = LoggerFactory.getLogger(TareasProgramadas.class);

    private final PagoUseCase pagos;
    private final CotizacionUseCase cotizaciones;

    public TareasProgramadas(PagoUseCase pagos, CotizacionUseCase cotizaciones) {
        this.pagos = pagos;
        this.cotizaciones = cotizaciones;
    }

    @Scheduled(initialDelay = 60_000, fixedDelayString = "${handgelus.tareas.intervalo-ms:600000}")
    public void revisarVencimientos() {
        int expirados = pagos.expirarPagosVencidos();
        int alertas = cotizaciones.alertarCotizacionesSinConfirmar();
        if (expirados > 0 || alertas > 0) {
            log.info("Tareas programadas: {} pago(s) expirado(s), {} alerta(s) de cotización emitida(s).",
                    expirados, alertas);
        }
    }
}
