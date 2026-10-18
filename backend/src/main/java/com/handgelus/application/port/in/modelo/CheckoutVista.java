package com.handgelus.application.port.in.modelo;

import com.handgelus.domain.model.Pago;

/** Datos de la sesión de pago que muestra la pasarela. */
public record CheckoutVista(Pago pago, String urlCheckout, String proveedor, String concepto, String clienteNombre,
                            Long solicitudId) {
}
