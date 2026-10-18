package com.handgelus.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.List;

/** Propiedades "handgelus.*" de application.properties. */
@ConfigurationProperties(prefix = "handgelus")
public record HandgelusProperties(
        Jwt jwt,
        Seguridad seguridad,
        Cors cors,
        Negocio negocio,
        Pasarela pasarela,
        Correo correo,
        Almacenamiento almacenamiento,
        DatosIniciales datosIniciales
) {

    public record Jwt(String secreto, int expiracionHoras) {
    }

    public record Seguridad(boolean requerirHttps) {
    }

    public record Cors(List<String> origenesPermitidos) {
    }

    public record Negocio(int porcentajeAbonoTatuaje, int porcentajeAnticipoComision, int horasExpiracionPago,
                          int horasAlertaCotizacion, int maxCitasPorDia, long imagenMaxBytes, String ciudadLocal,
                          BigDecimal costoEnvioLocal, BigDecimal costoEnvioNacional, int horasPorSesion) {
    }

    public record Pasarela(String nombre, String firmaWebhook, String urlCheckout) {
    }

    public record Correo(String remitente) {
    }

    public record Almacenamiento(String directorio) {
    }

    public record DatosIniciales(boolean habilitado) {
    }
}
