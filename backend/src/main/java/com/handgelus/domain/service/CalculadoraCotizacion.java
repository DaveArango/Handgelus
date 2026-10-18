package com.handgelus.domain.service;

import com.handgelus.domain.exception.ReglaNegocioException;
import com.handgelus.domain.model.Estilo;
import com.handgelus.domain.model.ParametrosNegocio;
import com.handgelus.domain.model.ResultadoCotizacion;
import com.handgelus.domain.model.Tarifa;
import com.handgelus.domain.model.TipoServicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;

/**
 * Servicio de dominio puro que calcula la cotización preliminar (RF-04).
 * <ul>
 *   <li>RN-03: tatuaje pequeño con tarifa fija → valor base sin ajustes.</li>
 *   <li>Resto: (valor base + valor hora × horas) × factor de complejidad del estilo.</li>
 *   <li>RN-09: las comisiones suman el costo de envío según el destino.</li>
 *   <li>RN-04: si el trabajo supera las horas por sesión, se planifica en varias sesiones.</li>
 * </ul>
 * El resultado se redondea a miles de pesos y es siempre un valor referencial.
 */
public class CalculadoraCotizacion {

    private static final BigDecimal MIL = new BigDecimal("1000");

    private final ParametrosNegocio parametros;

    public CalculadoraCotizacion(ParametrosNegocio parametros) {
        this.parametros = parametros;
    }

    public ResultadoCotizacion calcular(Tarifa tarifa, Estilo estilo, String ciudadDestino) {
        if (tarifa == null) {
            throw new ReglaNegocioException("RF-04", "No existe una tarifa configurada para el tamaño seleccionado.");
        }
        BigDecimal factor = estilo == null ? BigDecimal.ONE : estilo.getFactor();
        BigDecimal subtotal;
        if (tarifa.isTarifaFija()) {
            subtotal = tarifa.getValorBase();
            factor = BigDecimal.ONE;
        } else {
            BigDecimal porHoras = tarifa.getValorHora().multiply(BigDecimal.valueOf(tarifa.getHorasEstimadas()));
            subtotal = tarifa.getValorBase().add(porHoras).multiply(factor);
        }
        subtotal = redondearAMiles(subtotal);

        BigDecimal envio = BigDecimal.ZERO;
        int sesiones = 0;
        if (tarifa.getTipoServicio() == TipoServicio.COMISION) {
            envio = costoEnvio(ciudadDestino);
        } else {
            int horas = Math.max(1, tarifa.getHorasEstimadas());
            sesiones = (int) Math.ceil(horas / (double) parametros.horasPorSesion());
        }
        return new ResultadoCotizacion(subtotal.add(envio), subtotal, tarifa.getValorBase(), tarifa.getValorHora(),
                tarifa.getHorasEstimadas(), tarifa.isTarifaFija(), factor, envio, sesiones);
    }

    public BigDecimal costoEnvio(String ciudadDestino) {
        if (ciudadDestino == null || ciudadDestino.isBlank()) {
            return parametros.costoEnvioNacional();
        }
        return normalizar(ciudadDestino).equals(normalizar(parametros.ciudadLocal()))
                ? parametros.costoEnvioLocal()
                : parametros.costoEnvioNacional();
    }

    private static BigDecimal redondearAMiles(BigDecimal valor) {
        return valor.divide(MIL, 0, RoundingMode.HALF_UP).multiply(MIL);
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto.trim().toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
