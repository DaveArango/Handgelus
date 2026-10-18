package com.handgelus.application.service;

import com.handgelus.application.port.in.GestionTarifasUseCase;
import com.handgelus.application.port.out.TarifaRepositoryPort;
import com.handgelus.domain.exception.ReglaNegocioException;
import com.handgelus.domain.model.Actor;
import com.handgelus.domain.model.Rol;
import com.handgelus.domain.model.Tarifa;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * F-02 · Configurar tabla de tarifas. La configura el propietario (tatuador/artista)
 * o el administrador. Las cotizaciones guardan su propia copia de la tarifa, por lo que
 * modificar la tabla no las altera (SWR-05).
 */
public class GestionTarifasService implements GestionTarifasUseCase {

    private final TarifaRepositoryPort tarifas;
    private final Clock reloj;

    public GestionTarifasService(TarifaRepositoryPort tarifas, Clock reloj) {
        this.tarifas = tarifas;
        this.reloj = reloj;
    }

    @Override
    public List<Tarifa> listar() {
        return tarifas.listar().stream()
                .sorted(Comparator.comparing(Tarifa::getTipoServicio).thenComparing(Tarifa::getCategoria))
                .toList();
    }

    @Override
    public Tarifa guardar(TarifaComando c, Actor actor) {
        actor.exigir(Rol.TATUADOR, Rol.ADMINISTRADOR);
        if (c.tipoServicio() == null || c.categoria() == null) {
            throw new ReglaNegocioException("SWR-04", "Indique el tipo de servicio y el tamaño de la tarifa.");
        }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Tarifa tarifa = tarifas.buscarPorTipoYCategoria(c.tipoServicio(), c.categoria())
                .map(t -> {
                    t.actualizar(c.valorBase(), c.valorHora(), c.horasEstimadas(), c.tarifaFija(), c.descripcion(), ahora);
                    return t;
                })
                .orElseGet(() -> Tarifa.nueva(c.tipoServicio(), c.categoria(), c.valorBase(), c.valorHora(),
                        c.horasEstimadas(), c.tarifaFija(), c.descripcion(), ahora));
        return tarifas.guardar(tarifa);
    }
}
