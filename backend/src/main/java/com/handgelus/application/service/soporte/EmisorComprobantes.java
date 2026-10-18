package com.handgelus.application.service.soporte;

import com.handgelus.application.port.out.ComprobanteRepositoryPort;
import com.handgelus.domain.model.Comprobante;
import com.handgelus.domain.model.Solicitud;
import com.handgelus.domain.model.TipoComprobante;
import com.handgelus.domain.model.Usuario;
import com.handgelus.domain.service.ReglasPago;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

/** Genera comprobantes digitales con código consecutivo y los envía al cliente (RF-11). */
public class EmisorComprobantes {

    private final ComprobanteRepositoryPort comprobantes;
    private final Notificador notificador;
    private final Clock reloj;

    public EmisorComprobantes(ComprobanteRepositoryPort comprobantes, Notificador notificador, Clock reloj) {
        this.comprobantes = comprobantes;
        this.notificador = notificador;
        this.reloj = reloj;
    }

    public Comprobante emitir(Solicitud solicitud, Usuario cliente, TipoComprobante tipo, String detalle,
                              BigDecimal valor) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        String codigo = String.format("HG-%d-%05d", ahora.getYear(), comprobantes.contar() + 1);
        Comprobante comprobante = comprobantes.guardar(
                Comprobante.emitir(solicitud.getId(), cliente.getId(), tipo, codigo, detalle, valor, ahora));

        String cuerpo = tipo.getEtiqueta() + " · " + codigo + "\n"
                + "Proyecto: " + solicitud.getTitulo() + "\n"
                + detalle + "\n"
                + "Valor: $" + ReglasPago.formato(valor) + " COP\n\n"
                + "Puede consultar este comprobante en cualquier momento desde su perfil en HandgelusApp.";
        notificador.correoCliente(cliente, "Handgelus · " + tipo.getEtiqueta() + " " + codigo, cuerpo,
                solicitud.getId());
        return comprobante;
    }
}
