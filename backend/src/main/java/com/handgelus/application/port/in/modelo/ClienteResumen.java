package com.handgelus.application.port.in.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClienteResumen(UsuarioVista cliente, int servicios, int serviciosActivos, LocalDateTime ultimoServicio,
                             BigDecimal totalPagado) {
}
