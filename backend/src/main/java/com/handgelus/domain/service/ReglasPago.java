package com.handgelus.domain.service;

import com.handgelus.domain.exception.ReglaNegocioException;
import com.handgelus.domain.model.EstadoPagoServicio;
import com.handgelus.domain.model.Pago;
import com.handgelus.domain.model.ParametrosNegocio;
import com.handgelus.domain.model.TipoPago;
import com.handgelus.domain.model.TipoServicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Reglas de pagos parciales y saldos (RN-02, RN-07, RN-10, RN-11 · SWR-12/SWR-20). */
public class ReglasPago {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private final ParametrosNegocio parametros;

    public ReglasPago(ParametrosNegocio parametros) {
        this.parametros = parametros;
    }

    public TipoPago tipoPagoInicial(TipoServicio tipo) {
        return tipo == TipoServicio.TATUAJE ? TipoPago.ABONO : TipoPago.ANTICIPO;
    }

    public int porcentajeMinimo(TipoServicio tipo) {
        return tipo == TipoServicio.TATUAJE
                ? parametros.porcentajeAbonoTatuaje()
                : parametros.porcentajeAnticipoComision();
    }

    public BigDecimal montoMinimoInicial(BigDecimal valorTotal, TipoServicio tipo) {
        return valorTotal.multiply(BigDecimal.valueOf(porcentajeMinimo(tipo)))
                .divide(CIEN, 0, RoundingMode.CEILING);
    }

    public BigDecimal totalConfirmado(List<Pago> pagos) {
        return pagos.stream().filter(Pago::estaConfirmado).map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal saldo(BigDecimal valorTotal, List<Pago> pagos) {
        BigDecimal saldo = valorTotal.subtract(totalConfirmado(pagos));
        return saldo.signum() < 0 ? BigDecimal.ZERO : saldo;
    }

    public EstadoPagoServicio estadoPago(BigDecimal valorTotal, List<Pago> pagos) {
        BigDecimal pagado = totalConfirmado(pagos);
        if (pagado.signum() == 0) {
            return EstadoPagoServicio.PENDIENTE;
        }
        return pagado.compareTo(valorTotal) >= 0 ? EstadoPagoServicio.COMPLETO : EstadoPagoServicio.PARCIAL;
    }

    /** SWR-12: abono ≥ 30 % (tatuaje) o anticipo ≥ 50 % (comisión), sin superar el total. */
    public void validarPagoInicial(BigDecimal monto, BigDecimal valorTotal, TipoServicio tipo) {
        BigDecimal minimo = montoMinimoInicial(valorTotal, tipo);
        if (monto.compareTo(minimo) < 0) {
            String regla = tipo == TipoServicio.TATUAJE ? "RN-02" : "RN-10";
            String concepto = tipo == TipoServicio.TATUAJE ? "abono" : "anticipo";
            throw new ReglaNegocioException(regla, "El " + concepto + " mínimo es el " + porcentajeMinimo(tipo)
                    + "% del valor cotizado: $" + formato(minimo) + ".");
        }
        if (monto.compareTo(valorTotal) > 0) {
            throw new ReglaNegocioException("El pago no puede superar el valor total del servicio.");
        }
    }

    public void validarPagoSaldo(BigDecimal monto, BigDecimal saldoPendiente) {
        if (saldoPendiente.signum() == 0) {
            throw new ReglaNegocioException("El servicio ya se encuentra pagado en su totalidad.");
        }
        if (monto.compareTo(saldoPendiente) > 0) {
            throw new ReglaNegocioException("El pago supera el saldo pendiente de $" + formato(saldoPendiente) + ".");
        }
    }

    public static String formato(BigDecimal valor) {
        return String.format(java.util.Locale.US, "%,.0f", valor).replace(',', '.');
    }
}
