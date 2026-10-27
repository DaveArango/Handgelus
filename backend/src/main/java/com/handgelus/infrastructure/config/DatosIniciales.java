package com.handgelus.infrastructure.config;

import com.handgelus.application.port.in.AgendaUseCase;
import com.handgelus.application.port.in.AgendaUseCase.AgendarCitaComando;
import com.handgelus.application.port.in.AvanceObraUseCase;
import com.handgelus.application.port.in.AvanceObraUseCase.RegistrarAvanceComando;
import com.handgelus.application.port.in.CotizacionUseCase;
import com.handgelus.application.port.in.GestionTarifasUseCase;
import com.handgelus.application.port.in.GestionTarifasUseCase.TarifaComando;
import com.handgelus.application.port.in.PagoUseCase;
import com.handgelus.application.port.in.SolicitudUseCase;
import com.handgelus.application.port.in.SolicitudUseCase.RegistrarSolicitudComando;
import com.handgelus.application.port.in.modelo.CheckoutVista;
import com.handgelus.application.port.in.modelo.CitaAgenda;
import com.handgelus.application.port.in.modelo.DisponibilidadDia;
import com.handgelus.application.port.in.modelo.SolicitudDetalle;
import com.handgelus.application.port.out.CifradoPasswordPort;
import com.handgelus.application.port.out.UsuarioRepositoryPort;
import com.handgelus.domain.model.Actor;
import com.handgelus.domain.model.CategoriaTamano;
import com.handgelus.domain.model.EstadoObra;
import com.handgelus.domain.model.Estilo;
import com.handgelus.domain.model.MetodoPago;
import com.handgelus.domain.model.Rol;
import com.handgelus.domain.model.TipoServicio;
import com.handgelus.domain.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Carga datos de demostración la primera vez que arranca la aplicación (tabla de usuarios vacía).
 * Usa los mismos casos de uso que la interfaz, por lo que todos los registros respetan las reglas.
 * Se desactiva con handgelus.datos-iniciales.habilitado=false.
 */
@Component
@ConditionalOnProperty(prefix = "handgelus.datos-iniciales", name = "habilitado", havingValue = "true")
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private final UsuarioRepositoryPort usuarios;
    private final CifradoPasswordPort cifrado;
    private final GestionTarifasUseCase tarifas;
    private final SolicitudUseCase solicitudes;
    private final CotizacionUseCase cotizaciones;
    private final PagoUseCase pagos;
    private final AgendaUseCase agenda;
    private final AvanceObraUseCase avances;
    private final String firmaWebhook;
    private final Clock reloj;

    public DatosIniciales(UsuarioRepositoryPort usuarios, CifradoPasswordPort cifrado, GestionTarifasUseCase tarifas,
                          SolicitudUseCase solicitudes, CotizacionUseCase cotizaciones, PagoUseCase pagos,
                          AgendaUseCase agenda, AvanceObraUseCase avances, HandgelusProperties properties,
                          Clock reloj) {
        this.usuarios = usuarios;
        this.cifrado = cifrado;
        this.tarifas = tarifas;
        this.solicitudes = solicitudes;
        this.cotizaciones = cotizaciones;
        this.pagos = pagos;
        this.agenda = agenda;
        this.avances = avances;
        this.firmaWebhook = properties.pasarela().firmaWebhook();
        this.reloj = reloj;
    }

    @Override
    public void run(String... args) {
        if (usuarios.contar() > 0) {
            return;
        }
        log.info("Cargando datos iniciales de demostración de Handgelus…");

        // F-01 · Equipo interno
        Actor admin = crear("Administrador Handgelus", "admin@handgelus.co", "3000000001", "Admin2026", Rol.ADMINISTRADOR);
        Actor asistente = crear("Laura Restrepo", "asistente@handgelus.co", "3000000002", "Asistente2026", Rol.ASISTENTE);
        Actor artista = crear("Ángel García", "artista@handgelus.co", "3000000003", "Artista2026", Rol.TATUADOR);
        // Clientes
        Actor valeria = crear("Valeria Méndez", "valeria@correo.com", "3105550101", "Cliente2026", Rol.CLIENTE);
        Actor julian = crear("Julián Castillo", "julian@correo.com", "3115550202", "Cliente2026", Rol.CLIENTE);
        Actor lucia = crear("Lucía Ferreyra", "lucia@correo.com", "3125550303", "Cliente2026", Rol.CLIENTE);
        Actor esteban = crear("Esteban Parra", "esteban@correo.com", "3135550404", "Cliente2026", Rol.CLIENTE);

        // F-02 · Tabla de tarifas (COP)
        tarifa(TipoServicio.TATUAJE, CategoriaTamano.PEQUENO, 150_000, 0, 1, true, "Tarifa fija para piezas pequeñas (RN-03).", artista);
        tarifa(TipoServicio.TATUAJE, CategoriaTamano.MEDIANO, 250_000, 110_000, 2, false, "Piezas medianas de una sesión.", artista);
        tarifa(TipoServicio.TATUAJE, CategoriaTamano.GRANDE, 450_000, 120_000, 5, false, "Piezas grandes; pueden requerir dos sesiones.", artista);
        tarifa(TipoServicio.TATUAJE, CategoriaTamano.EXTRA_GRANDE, 800_000, 120_000, 10, false, "Mangas y espaldas por sesiones.", artista);
        tarifa(TipoServicio.COMISION, CategoriaTamano.PEQUENO, 180_000, 35_000, 3, false, "Formato A5 en técnica mixta.", artista);
        tarifa(TipoServicio.COMISION, CategoriaTamano.MEDIANO, 320_000, 40_000, 6, false, "Formato A4.", artista);
        tarifa(TipoServicio.COMISION, CategoriaTamano.GRANDE, 550_000, 45_000, 10, false, "Formato A3.", artista);
        tarifa(TipoServicio.COMISION, CategoriaTamano.EXTRA_GRANDE, 900_000, 50_000, 16, false, "Formato A2, óleo o acrílico.", artista);

        // 1 · Tatuaje con cita agendada (abono pagado por la pasarela)
        SolicitudDetalle s1 = solicitudes.registrar(tatuaje("Botanical Sleeve",
                "Composición de flora autóctona del Quindío: helechos, orquídeas y café, con líneas finas y puntillismo suave.",
                CategoriaTamano.GRANDE, Estilo.BOTANICO, "Antebrazo derecho"), valeria);
        Long id1 = s1.solicitud().getId();
        cotizaciones.confirmar(id1, new BigDecimal("1300000"), "Duración estimada: dos sesiones de 4 horas. "
                + "Hidrata la piel la semana previa.", artista);
        cotizaciones.decidir(id1, true, valeria);
        CheckoutVista checkout = pagos.iniciarPago(id1, new BigDecimal("390000"), MetodoPago.TARJETA, valeria);
        pagos.procesarRespuestaPasarela(checkout.pago().getReferencia(), true, firmaWebhook);
        LocalDate fecha = proximaFecha(artista);
        agenda.agendar(new AgendarCitaComando(id1, artista.id(), fecha, primeraJornada(artista, fecha), "Sesión 1: contornos"), asistente);

        // 2 · Solicitud nueva, a la espera de la cotización del artista
        solicitudes.registrar(tatuaje("Retrato minimalista",
                "Retrato de mi abuela en línea continua, muy delicado, a partir de una fotografía antigua.",
                CategoriaTamano.MEDIANO, Estilo.LINEA_FINA, "Omóplato izquierdo"), julian);

        // 3 · Comisión en producción con avance registrado
        SolicitudDetalle s3 = solicitudes.registrar(comision("Catedral gótica",
                "Ilustración de una catedral gótica en tinta sepia con detalles en pan de oro para mi estudio.",
                CategoriaTamano.GRANDE, Estilo.ILUSTRACION, "Carrera 7 # 45-12, Apto 501, Chapinero", "Bogotá"), lucia);
        Long id3 = s3.solicitud().getId();
        cotizaciones.confirmar(id3, null, "Incluye marco de madera natural.", artista);
        cotizaciones.decidir(id3, true, lucia);
        BigDecimal anticipo = s3.cotizacion().getValorCalculado().divide(BigDecimal.valueOf(2)).setScale(0, java.math.RoundingMode.CEILING);
        pagos.registrarPagoManual(id3, anticipo, MetodoPago.TRANSFERENCIA, asistente);
        avances.cambiarEstadoObra(id3, EstadoObra.EN_PRODUCCION, null, artista);
        avances.registrarAvance(new RegistrarAvanceComando(id3, "Boceto y perspectiva",
                "Se definió la perspectiva central y la composición de los rosetones.", 40, null), artista);

        // 4 · Cotización enviada, esperando la decisión del cliente
        SolicitudDetalle s4 = solicitudes.registrar(tatuaje("Caligrafía de autor",
                "Una frase corta en mi propia letra, en tipografía personalizada, sobre la clavícula.",
                CategoriaTamano.PEQUENO, Estilo.LETTERING, "Clavícula"), esteban);
        cotizaciones.confirmar(s4.solicitud().getId(), null, "Sesión única de aproximadamente una hora.", artista);

        // 5 · Comisión terminada, pagada y enviada (historial)
        SolicitudDetalle s5 = solicitudes.registrar(comision("Símbolos alquímicos",
                "Serie de símbolos alquímicos en acuarela y tinta para enmarcar.",
                CategoriaTamano.MEDIANO, Estilo.ABSTRACTO, "Calle 21 # 14-30, Barrio Centro", "Armenia"), valeria);
        Long id5 = s5.solicitud().getId();
        cotizaciones.confirmar(id5, null, null, artista);
        cotizaciones.decidir(id5, true, valeria);
        BigDecimal total5 = s5.cotizacion().getValorCalculado();
        pagos.registrarPagoManual(id5, total5.divide(BigDecimal.valueOf(2)), MetodoPago.EFECTIVO, asistente);
        avances.cambiarEstadoObra(id5, EstadoObra.EN_PRODUCCION, null, artista);
        avances.cambiarEstadoObra(id5, EstadoObra.TERMINADA, null, artista);
        pagos.registrarPagoManual(id5, total5.divide(BigDecimal.valueOf(2)), MetodoPago.EFECTIVO, asistente);
        avances.cambiarEstadoObra(id5, EstadoObra.ENVIADA, "SERVIENTREGA-2026-88341", artista);

        // 6 · Tatuaje completado (historial)
        SolicitudDetalle s6 = solicitudes.registrar(tatuaje("Golondrina",
                "Golondrina pequeña en vuelo, en línea fina, para la muñeca.",
                CategoriaTamano.PEQUENO, Estilo.LINEA_FINA, "Muñeca izquierda"), lucia);
        Long id6 = s6.solicitud().getId();
        cotizaciones.confirmar(id6, null, null, artista);
        cotizaciones.decidir(id6, true, lucia);
        pagos.registrarPagoManual(id6, s6.cotizacion().getValorCalculado(), MetodoPago.NEQUI, asistente);
        LocalDate fecha6 = proximaFecha(artista);
        SolicitudDetalle d6 = agenda.agendar(new AgendarCitaComando(id6, artista.id(), fecha6,
                primeraJornada(artista, fecha6), null), asistente);
        CitaAgenda cita6 = d6.citas().get(0);
        agenda.marcarRealizada(cita6.cita().getId(), artista);
        agenda.completarServicio(id6, asistente);

        log.info("""

                ════════════════════════════════════════════════════════════
                  HANDGELUS · Cuentas de demostración
                  Administrador  admin@handgelus.co      / Admin2026
                  Asistente      asistente@handgelus.co  / Asistente2026
                  Artista        artista@handgelus.co    / Artista2026
                  Cliente        valeria@correo.com      / Cliente2026
                ════════════════════════════════════════════════════════════""");
        log.debug("Administrador creado con id {}", admin.id());
    }

    private Actor crear(String nombre, String correo, String telefono, String password, Rol rol) {
        Usuario u = usuarios.guardar(Usuario.nuevo(nombre, correo, telefono, cifrado.cifrar(password), rol,
                LocalDateTime.now(reloj)));
        return new Actor(u.getId(), u.getNombre(), u.getRol());
    }

    private void tarifa(TipoServicio tipo, CategoriaTamano cat, long base, long hora, int horas, boolean fija,
                        String descripcion, Actor actor) {
        tarifas.guardar(new TarifaComando(tipo, cat, BigDecimal.valueOf(base), BigDecimal.valueOf(hora), horas, fija,
                descripcion), actor);
    }

    private static RegistrarSolicitudComando tatuaje(String titulo, String idea, CategoriaTamano cat, Estilo estilo,
                                                     String zona) {
        return new RegistrarSolicitudComando(TipoServicio.TATUAJE, titulo, idea, cat, estilo, zona, null, null, null);
    }

    private static RegistrarSolicitudComando comision(String titulo, String idea, CategoriaTamano cat, Estilo estilo,
                                                      String direccion, String ciudad) {
        return new RegistrarSolicitudComando(TipoServicio.COMISION, titulo, idea, cat, estilo, null, direccion, ciudad, null);
    }

    private LocalDate proximaFecha(Actor artista) {
        List<DisponibilidadDia> dias = agenda.sugerirFechas(artista.id(), LocalDate.now(reloj).plusDays(1), 1);
        return dias.isEmpty() ? LocalDate.now(reloj).plusDays(2) : dias.get(0).fecha();
    }

    private com.handgelus.domain.model.Jornada primeraJornada(Actor artista, LocalDate fecha) {
        return agenda.disponibilidad(artista.id(), fecha, fecha).get(0).jornadasLibres().get(0);
    }
}
