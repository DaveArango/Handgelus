package com.handgelus.infrastructure.adapter.in.web;

import com.handgelus.application.port.in.PagoUseCase;
import com.handgelus.application.port.in.modelo.CheckoutVista;
import com.handgelus.application.port.in.modelo.PagoVista;
import com.handgelus.application.port.in.modelo.SolicitudDetalle;
import com.handgelus.domain.exception.AccesoDenegadoException;
import com.handgelus.domain.model.Actor;
import com.handgelus.domain.model.EstadoPago;
import com.handgelus.domain.model.MetodoPago;
import com.handgelus.domain.model.Pago;
import com.handgelus.infrastructure.config.HandgelusProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de caracterización del endpoint /api/pasarela-simulada/{referencia}/{resultado}.
 * Fija el comportamiento que tenía en PagoController para comprobar que se conserva tras moverlo.
 */
class PasarelaSimuladaControllerTest {

    private static final String FIRMA = "firma-de-prueba";

    /** Doble de PagoUseCase que registra las llamadas en orden. */
    static class PagoUseCaseEspia implements PagoUseCase {
        final List<String> llamadas = new ArrayList<>();
        RuntimeException errorEnCheckout;

        @Override
        public CheckoutVista consultarCheckout(String referencia, Actor actor) {
            llamadas.add("consultarCheckout:" + referencia);
            if (errorEnCheckout != null) {
                throw errorEnCheckout;
            }
            return null;
        }

        @Override
        public Pago procesarRespuestaPasarela(String referencia, boolean aprobado, String firma) {
            llamadas.add("procesar:" + referencia + ":" + aprobado + ":" + firma);
            return null;
        }

        @Override
        public CheckoutVista iniciarPago(Long solicitudId, BigDecimal monto, MetodoPago metodo, Actor actor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SolicitudDetalle registrarPagoManual(Long solicitudId, BigDecimal monto, MetodoPago metodo, Actor actor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<PagoVista> listar(EstadoPago estado, Actor actor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int expirarPagosVencidos() {
            throw new UnsupportedOperationException();
        }
    }

    private static HandgelusProperties propiedades() {
        return new HandgelusProperties(null, null, null, null,
                new HandgelusProperties.Pasarela("Simulada", FIRMA, "http://localhost/pasarela"),
                null, null, null);
    }

    @Test
    void aprobarValidaPropiedadYEnviaWebhookConLaFirmaConfigurada() {
        PagoUseCaseEspia espia = new PagoUseCaseEspia();
        new PasarelaSimuladaController(espia, propiedades()).pasarelaSimulada("HG-PAY-1", "aprobar", null);
        assertEquals(List.of("consultarCheckout:HG-PAY-1", "procesar:HG-PAY-1:true:" + FIRMA), espia.llamadas);
    }

    @Test
    void elResultadoNoDistingueMayusculas() {
        PagoUseCaseEspia espia = new PagoUseCaseEspia();
        new PasarelaSimuladaController(espia, propiedades()).pasarelaSimulada("HG-PAY-2", "APROBAR", null);
        assertTrue(espia.llamadas.contains("procesar:HG-PAY-2:true:" + FIRMA));
    }

    @Test
    void cualquierOtroResultadoSeTrataComoRechazo() {
        PagoUseCaseEspia espia = new PagoUseCaseEspia();
        new PasarelaSimuladaController(espia, propiedades()).pasarelaSimulada("HG-PAY-3", "rechazar", null);
        assertTrue(espia.llamadas.contains("procesar:HG-PAY-3:false:" + FIRMA));
    }

    @Test
    void siElPagoNoEsDelUsuarioNoSeProcesa() {
        PagoUseCaseEspia espia = new PagoUseCaseEspia();
        espia.errorEnCheckout = new AccesoDenegadoException("No autorizado");
        PasarelaSimuladaController controlador = new PasarelaSimuladaController(espia, propiedades());
        assertThrows(AccesoDenegadoException.class, () -> controlador.pasarelaSimulada("HG-PAY-4", "aprobar", null));
        assertEquals(List.of("consultarCheckout:HG-PAY-4"), espia.llamadas);
    }
}
