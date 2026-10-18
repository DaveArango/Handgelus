package com.handgelus.application;

import com.handgelus.application.port.in.*;
import com.handgelus.application.port.in.modelo.*;
import com.handgelus.application.port.out.*;
import com.handgelus.application.service.*;
import com.handgelus.application.service.soporte.*;
import com.handgelus.domain.exception.*;
import com.handgelus.domain.model.*;
import com.handgelus.domain.service.*;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Prueba del flujo completo de negocio (F-01 a F-08) sobre los casos de uso reales,
 * usando adaptadores en memoria: demuestra que el núcleo hexagonal no depende de Spring ni de Oracle.
 */
class FlujoNegocioTest {

    static class Mem<T> {
        final Map<Long, T> datos = new LinkedHashMap<>();
        final AtomicLong seq = new AtomicLong();
        final Function<T, Long> getId;
        Mem(Function<T, Long> getId) { this.getId = getId; }
        T save(T t) {
            Long id = getId.apply(t);
            if (id == null) {
                id = seq.incrementAndGet();
                try {
                    Field f = t.getClass().getDeclaredField("id");
                    f.setAccessible(true);
                    f.set(t, id);
                } catch (Exception e) { throw new RuntimeException(e); }
            }
            datos.put(id, t);
            return t;
        }
        List<T> all() { return new ArrayList<>(datos.values()); }
    }

    static void check(String nombre, boolean cond) {
        org.junit.jupiter.api.Assertions.assertTrue(cond, nombre);
    }

    static void espera(String nombre, Class<? extends Exception> tipo, org.junit.jupiter.api.function.Executable r) {
        org.junit.jupiter.api.Assertions.assertThrows(tipo, r, nombre);
    }

    @Test
    void flujoCompletoDeLasOchoFuncionalidades() {
        MutableClock reloj = new MutableClock(LocalDateTime.of(2026, 10, 19, 10, 0)); // lunes
        ParametrosNegocio p = ParametrosNegocio.porDefecto();

        Mem<Usuario> mu = new Mem<>(Usuario::getId);
        Mem<Tarifa> mt = new Mem<>(Tarifa::getId);
        Mem<Solicitud> ms = new Mem<>(Solicitud::getId);
        Mem<Cotizacion> mc = new Mem<>(Cotizacion::getId);
        Mem<Pago> mp = new Mem<>(Pago::getId);
        Mem<Cita> mci = new Mem<>(Cita::getId);
        Mem<Obra> mo = new Mem<>(Obra::getId);
        Mem<RegistroAvance> ma = new Mem<>(RegistroAvance::getId);
        Mem<Comprobante> mco = new Mem<>(Comprobante::getId);
        Mem<Notificacion> mn = new Mem<>(Notificacion::getId);

        UsuarioRepositoryPort ur = new UsuarioRepositoryPort() {
            public Usuario guardar(Usuario u) { return mu.save(u); }
            public Optional<Usuario> buscarPorId(Long id) { return Optional.ofNullable(mu.datos.get(id)); }
            public Optional<Usuario> buscarPorCorreo(String c) { return mu.all().stream().filter(u -> u.getCorreo().equals(c)).findFirst(); }
            public boolean existeCorreo(String c) { return buscarPorCorreo(c).isPresent(); }
            public List<Usuario> listar() { return mu.all(); }
            public List<Usuario> listarPorRol(Rol r) { return mu.all().stream().filter(u -> u.getRol() == r).toList(); }
            public List<Usuario> buscarPorIds(Collection<Long> ids) { return mu.all().stream().filter(u -> ids.contains(u.getId())).toList(); }
            public long contar() { return mu.datos.size(); }
        };
        TarifaRepositoryPort tr = new TarifaRepositoryPort() {
            public Tarifa guardar(Tarifa t) { return mt.save(t); }
            public Optional<Tarifa> buscarPorId(Long id) { return Optional.ofNullable(mt.datos.get(id)); }
            public Optional<Tarifa> buscarPorTipoYCategoria(TipoServicio t, CategoriaTamano c) { return mt.all().stream().filter(x -> x.getTipoServicio() == t && x.getCategoria() == c).findFirst(); }
            public List<Tarifa> listar() { return mt.all(); }
            public long contar() { return mt.datos.size(); }
        };
        SolicitudRepositoryPort sr = new SolicitudRepositoryPort() {
            public Solicitud guardar(Solicitud s) { return ms.save(s); }
            public Optional<Solicitud> buscarPorId(Long id) { return Optional.ofNullable(ms.datos.get(id)); }
            public List<Solicitud> buscar(Collection<EstadoSolicitud> e, TipoServicio t, Long c) {
                return ms.all().stream().filter(s -> e == null || e.contains(s.getEstado()))
                        .filter(s -> t == null || s.getTipoServicio() == t).filter(s -> c == null || s.getClienteId().equals(c))
                        .sorted(Comparator.comparing(Solicitud::getFechaCreacion).reversed()).toList();
            }
            public List<Solicitud> buscarPorIds(Collection<Long> ids) { return ms.all().stream().filter(s -> ids.contains(s.getId())).toList(); }
        };
        CotizacionRepositoryPort cr = new CotizacionRepositoryPort() {
            public Cotizacion guardar(Cotizacion c) { return mc.save(c); }
            public Optional<Cotizacion> buscarPorSolicitud(Long id) { return mc.all().stream().filter(c -> c.getSolicitudId().equals(id)).findFirst(); }
            public List<Cotizacion> buscarPorSolicitudes(Collection<Long> ids) { return mc.all().stream().filter(c -> ids.contains(c.getSolicitudId())).toList(); }
            public List<Cotizacion> listarPreliminaresSinAlertaAntesDe(LocalDateTime l) { return mc.all().stream().filter(c -> c.getEstado() == EstadoCotizacion.PRELIMINAR && !c.isAlertaEmitida() && c.getFechaEmision().isBefore(l)).toList(); }
        };
        PagoRepositoryPort pr = new PagoRepositoryPort() {
            public Pago guardar(Pago x) { return mp.save(x); }
            public Optional<Pago> buscarPorReferencia(String r) { return mp.all().stream().filter(x -> r.equals(x.getReferencia())).findFirst(); }
            public List<Pago> listarPorSolicitud(Long id) { return mp.all().stream().filter(x -> x.getSolicitudId().equals(id)).toList(); }
            public List<Pago> listarPorSolicitudes(Collection<Long> ids) { return mp.all().stream().filter(x -> ids.contains(x.getSolicitudId())).toList(); }
            public List<Pago> listarPendientesCreadosAntesDe(LocalDateTime l) { return mp.all().stream().filter(x -> x.estaPendiente() && x.getFechaCreacion().isBefore(l)).toList(); }
            public List<Pago> listar(EstadoPago e) { return mp.all().stream().filter(x -> e == null || x.getEstado() == e).toList(); }
        };
        CitaRepositoryPort cir = new CitaRepositoryPort() {
            public Cita guardar(Cita c) { return mci.save(c); }
            public Optional<Cita> buscarPorId(Long id) { return Optional.ofNullable(mci.datos.get(id)); }
            public List<Cita> listarPorTatuadorEntre(Long t, LocalDate d, LocalDate h) { return listarEntre(d, h).stream().filter(c -> c.getTatuadorId().equals(t)).toList(); }
            public List<Cita> listarEntre(LocalDate d, LocalDate h) { return mci.all().stream().filter(c -> !c.getFecha().isBefore(d) && !c.getFecha().isAfter(h)).toList(); }
            public List<Cita> listarPorSolicitud(Long id) { return mci.all().stream().filter(c -> c.getSolicitudId().equals(id)).toList(); }
        };
        ObraRepositoryPort or = new ObraRepositoryPort() {
            public Obra guardar(Obra o) { return mo.save(o); }
            public Optional<Obra> buscarPorSolicitud(Long id) { return mo.all().stream().filter(o -> o.getSolicitudId().equals(id)).findFirst(); }
            public List<Obra> listar() { return mo.all(); }
        };
        AvanceRepositoryPort ar = new AvanceRepositoryPort() {
            public RegistroAvance guardar(RegistroAvance a) { return ma.save(a); }
            public List<RegistroAvance> listarPorSolicitud(Long id) { return ma.all().stream().filter(a -> a.getSolicitudId().equals(id)).toList(); }
        };
        ComprobanteRepositoryPort cor = new ComprobanteRepositoryPort() {
            public Comprobante guardar(Comprobante c) { return mco.save(c); }
            public Optional<Comprobante> buscarPorId(Long id) { return Optional.ofNullable(mco.datos.get(id)); }
            public List<Comprobante> listarPorSolicitud(Long id) { return mco.all().stream().filter(c -> c.getSolicitudId().equals(id)).toList(); }
            public List<Comprobante> listarPorCliente(Long id) { return mco.all().stream().filter(c -> c.getClienteId().equals(id)).toList(); }
            public long contar() { return mco.datos.size(); }
        };
        NotificacionRepositoryPort nr = new NotificacionRepositoryPort() {
            public Notificacion guardar(Notificacion n) { return mn.save(n); }
            public Optional<Notificacion> buscarPorId(Long id) { return Optional.ofNullable(mn.datos.get(id)); }
            public List<Notificacion> listar(TipoNotificacion t, int l) { return mn.all().stream().filter(n -> t == null || n.getTipo() == t).limit(l).toList(); }
            public long contarNoLeidas(TipoNotificacion t) { return mn.all().stream().filter(n -> n.getTipo() == t && !n.isLeida()).count(); }
        };
        AtomicLong refSeq = new AtomicLong();
        PasarelaPagoPort pasarela = new PasarelaPagoPort() {
            public String generarReferencia() { return "REF-" + refSeq.incrementAndGet(); }
            public SesionPago crearSesion(String r, BigDecimal m, String c, String co) { return new SesionPago(r, "http://x/pasarela/" + r, "Simulada"); }
            public boolean firmaValida(String f) { return "ok".equals(f); }
        };
        List<String> correos = new ArrayList<>();
        EnvioCorreoPort correo = (d, a, c) -> correos.add(d + " | " + a);
        AlmacenamientoArchivosPort alm = new AlmacenamientoArchivosPort() {
            public String guardar(String n, String ct, byte[] c) { return "img-" + n; }
            public Optional<ArchivoAlmacenado> obtener(String n) { return Optional.of(new ArchivoAlmacenado(n, "image/png", new byte[1])); }
        };
        CifradoPasswordPort cif = new CifradoPasswordPort() {
            public String cifrar(String t) { return "H(" + t + ")"; }
            public boolean coincide(String t, String h) { return ("H(" + t + ")").equals(h); }
        };
        TokenPort tok = u -> "token-" + u.getId();

        CalculadoraCotizacion calc = new CalculadoraCotizacion(p);
        ReglasPago reglas = new ReglasPago(p);
        Notificador notif = new Notificador(nr, correo, reloj);
        EmisorComprobantes emisor = new EmisorComprobantes(cor, notif, reloj);
        EnsambladorDetalle ens = new EnsambladorDetalle(sr, ur, cr, pr, cir, or, ar, cor, reglas);

        AutenticacionUseCase auth = new AutenticacionService(ur, cif, tok, reloj);
        GestionUsuariosUseCase gu = new GestionUsuariosService(ur, cif, reloj);
        GestionTarifasUseCase gt = new GestionTarifasService(tr, reloj);
        SolicitudUseCase sol = new SolicitudService(sr, ur, tr, cr, pr, ar, alm, calc, reglas, ens, notif, p, reloj);
        CotizacionUseCase cot = new CotizacionService(sr, cr, tr, calc, reglas, ens, emisor, notif, p, reloj);
        PagoUseCase pag = new PagoService(sr, pr, or, ur, pasarela, reglas, ens, emisor, notif, p, reloj);
        AgendaUseCase age = new AgendaService(cir, sr, ur, ar, ens, emisor, notif, p, reloj);
        AvanceObraUseCase av = new AvanceObraService(sr, or, ar, ur, alm, ens, notif, p, reloj);
        HistorialUseCase his = new HistorialService(ur, sr, cr, pr, cor, ens, reglas);
        TableroUseCase tab = new TableroService(sr, cr, pr, cir, nr, ens, reglas, reloj);

        // ---------------- F-01
        Usuario adminU = ur.guardar(Usuario.nuevo("Admin Sistema", "admin@handgelus.co", "3000000000", cif.cifrar("Admin2026"), Rol.ADMINISTRADOR, LocalDateTime.now(reloj)));
        Actor admin = new Actor(adminU.getId(), "Admin", Rol.ADMINISTRADOR);
        UsuarioVista asis = gu.crear(new GestionUsuariosUseCase.CrearUsuarioComando("Laura Asistente", "laura@handgelus.co", "3001112233", "Asistente1", Rol.ASISTENTE), admin);
        UsuarioVista tat = gu.crear(new GestionUsuariosUseCase.CrearUsuarioComando("Ángel Tatuador", "angel@handgelus.co", "3002223344", "Tatuador1", Rol.TATUADOR), admin);
        check("admin crea asistente con rol único", asis.rol() == Rol.ASISTENTE);
        Actor asistente = new Actor(asis.id(), asis.nombre(), Rol.ASISTENTE);
        Actor tatuador = new Actor(tat.id(), tat.nombre(), Rol.TATUADOR);
        espera("asistente no puede crear cuentas", AccesoDenegadoException.class, () -> gu.crear(new GestionUsuariosUseCase.CrearUsuarioComando("X Y Z", "x@h.co", "3001112233", "Clave1234", Rol.ASISTENTE), asistente));
        espera("correo duplicado", ReglaNegocioException.class, () -> gu.crear(new GestionUsuariosUseCase.CrearUsuarioComando("Laura Dos", "LAURA@handgelus.co", "3001112233", "Clave1234", Rol.TATUADOR), admin));
        check("login asistente", auth.iniciarSesion("laura@handgelus.co", "Asistente1").token().startsWith("token-"));
        espera("login con clave errada", CredencialesInvalidasException.class, () -> auth.iniciarSesion("laura@handgelus.co", "mala"));
        check("contraseña no se guarda en texto plano", !ur.buscarPorCorreo("laura@handgelus.co").get().getPasswordHash().equals("Asistente1"));

        // ---------------- F-02
        gt.guardar(new GestionTarifasUseCase.TarifaComando(TipoServicio.TATUAJE, CategoriaTamano.PEQUENO, new BigDecimal("150000"), BigDecimal.ZERO, 1, true, null), tatuador);
        gt.guardar(new GestionTarifasUseCase.TarifaComando(TipoServicio.TATUAJE, CategoriaTamano.GRANDE, new BigDecimal("500000"), new BigDecimal("120000"), 6, false, null), tatuador);
        gt.guardar(new GestionTarifasUseCase.TarifaComando(TipoServicio.COMISION, CategoriaTamano.MEDIANO, new BigDecimal("350000"), new BigDecimal("40000"), 6, false, null), tatuador);
        espera("asistente no configura tarifas", AccesoDenegadoException.class, () -> gt.guardar(new GestionTarifasUseCase.TarifaComando(TipoServicio.TATUAJE, CategoriaTamano.PEQUENO, BigDecimal.TEN, BigDecimal.ZERO, 1, true, null), asistente));
        check("3 tarifas", gt.listar().size() == 3);

        // ---------------- F-03
        SesionVista sc = auth.registrarCliente(new AutenticacionUseCase.RegistroClienteComando("Valeria Méndez", "valeria@correo.com", "3105556677", "Valeria2026", true));
        Actor cliente = new Actor(sc.usuario().id(), "Valeria", Rol.CLIENTE);
        espera("SWR-06 correo único al registrarse", ReglaNegocioException.class, () -> auth.registrarCliente(new AutenticacionUseCase.RegistroClienteComando("Otra", "valeria@correo.com", "3105556677", "Valeria2026", true)));
        espera("RF-02 imagen > 5MB", ReglaNegocioException.class, () -> sol.registrar(new SolicitudUseCase.RegistrarSolicitudComando(TipoServicio.TATUAJE, "Rosa", "Rosa en línea fina en el brazo", CategoriaTamano.PEQUENO, Estilo.LINEA_FINA, "Antebrazo", null, null, new SolicitudUseCase.ImagenAdjunta("a.png", "image/png", new byte[6 * 1024 * 1024])), cliente));
        espera("SWR-08 comisión sin dirección", ReglaNegocioException.class, () -> sol.registrar(new SolicitudUseCase.RegistrarSolicitudComando(TipoServicio.COMISION, "Óleo", "Retrato al óleo de mi abuela", CategoriaTamano.MEDIANO, Estilo.ILUSTRACION, null, "", "Pereira", null), cliente));
        SolicitudDetalle s1 = sol.registrar(new SolicitudUseCase.RegistrarSolicitudComando(TipoServicio.TATUAJE, "Botanical Sleeve", "Composición de flora autóctona en el antebrazo", CategoriaTamano.GRANDE, Estilo.BOTANICO, "Antebrazo derecho", null, null, new SolicitudUseCase.ImagenAdjunta("ref.png", "image/png", new byte[100])), cliente);
        check("estado inicial Recibida", s1.solicitud().getEstado() == EstadoSolicitud.RECIBIDA);
        check("cotización preliminar automática", s1.cotizacion() != null && s1.cotizacion().getEstado() == EstadoCotizacion.PRELIMINAR);
        // (500000 + 120000*6) * 1.10 = 1,342,000
        check("cálculo preliminar = 1.342.000 → " + s1.cotizacion().getValorCalculado(), s1.cotizacion().getValorCalculado().compareTo(new BigDecimal("1342000")) == 0);
        check("RN-04 sesiones = 2", s1.cotizacion().getSesionesEstimadas() == 2);
        check("alerta interna a la asistente", mn.all().stream().anyMatch(n -> n.getTipo() == TipoNotificacion.ALERTA_INTERNA && n.getAsunto().startsWith("Nueva solicitud")));
        ResultadoCotizacion peq = cot.estimar(TipoServicio.TATUAJE, CategoriaTamano.PEQUENO, Estilo.MICRORREALISMO, null);
        check("RN-03 tarifa fija pequeño = 150.000", peq.valorTotal().compareTo(new BigDecimal("150000")) == 0);

        // ---------------- F-04
        Long id1 = s1.solicitud().getId();
        espera("asistente no confirma cotización", AccesoDenegadoException.class, () -> cot.confirmar(id1, null, null, asistente));
        // cambio de tarifa antes de confirmar NO debe alterar la cotización (SWR-05)
        gt.guardar(new GestionTarifasUseCase.TarifaComando(TipoServicio.TATUAJE, CategoriaTamano.GRANDE, new BigDecimal("900000"), new BigDecimal("150000"), 6, false, null), tatuador);
        check("SWR-05 cotización conserva su valor tras cambio de tarifa", cr.buscarPorSolicitud(id1).get().getValorCalculado().compareTo(new BigDecimal("1342000")) == 0);
        SolicitudDetalle d1 = cot.confirmar(id1, new BigDecimal("1300000"), "Duración estimada 8 horas", tatuador);
        check("estado Cotización generada", d1.solicitud().getEstado() == EstadoSolicitud.COTIZACION_GENERADA);
        check("valor final ajustado", d1.cotizacion().getValorFinal().compareTo(new BigDecimal("1300000")) == 0);
        check("RF-06 correo al cliente", correos.stream().anyMatch(c -> c.contains("cotización está lista")));
        Actor intruso = new Actor(999L, "Otro", Rol.CLIENTE);
        espera("SWR-19 otro cliente no ve la solicitud", AccesoDenegadoException.class, () -> sol.detalle(id1, intruso));
        d1 = cot.decidir(id1, true, cliente);
        check("cotización aceptada + comprobante", d1.solicitud().getEstado() == EstadoSolicitud.COTIZACION_ACEPTADA && d1.comprobantes().size() == 1);

        // ---------------- F-05
        espera("SWR-12 abono < 30%", ReglaNegocioException.class, () -> pag.iniciarPago(id1, new BigDecimal("100000"), MetodoPago.TARJETA, cliente));
        CheckoutVista ck = pag.iniciarPago(id1, new BigDecimal("390000"), MetodoPago.TARJETA, cliente);
        check("pago pendiente + estado Pago en proceso", ck.pago().estaPendiente() && sr.buscarPorId(id1).get().getEstado() == EstadoSolicitud.PAGO_EN_PROCESO);
        espera("RN-01 no agenda sin abono confirmado", ReglaNegocioException.class, () -> age.agendar(new AgendaUseCase.AgendarCitaComando(id1, tat.id(), LocalDate.of(2026, 10, 21), Jornada.MANANA, null), asistente));
        espera("webhook con firma inválida", AccesoDenegadoException.class, () -> pag.procesarRespuestaPasarela(ck.pago().getReferencia(), true, "mala"));
        // SWR-14: 49 horas sin confirmar
        reloj.avanzarHoras(49);
        check("SWR-14 expira 1 pago", pag.expirarPagosVencidos() == 1);
        check("vuelve a Cotización aceptada", sr.buscarPorId(id1).get().getEstado() == EstadoSolicitud.COTIZACION_ACEPTADA);
        CheckoutVista ck2 = pag.iniciarPago(id1, new BigDecimal("390000"), MetodoPago.PSE, cliente);
        pag.procesarRespuestaPasarela(ck2.pago().getReferencia(), true, "ok");
        check("SWR-13 Pago parcial confirmado", sr.buscarPorId(id1).get().getEstado() == EstadoSolicitud.PAGO_PARCIAL_CONFIRMADO);
        check("webhook idempotente", pag.procesarRespuestaPasarela(ck2.pago().getReferencia(), true, "ok").estaConfirmado());

        // ---------------- F-06
        LocalDate miercoles = LocalDate.of(2026, 10, 21);
        SolicitudDetalle ag = age.agendar(new AgendaUseCase.AgendarCitaComando(id1, tat.id(), miercoles, Jornada.MANANA, null), asistente);
        check("Cita agendada", ag.solicitud().getEstado() == EstadoSolicitud.CITA_AGENDADA);
        check("RF-11 comprobante de cita", ag.comprobantes().stream().anyMatch(c -> c.getTipo() == TipoComprobante.CITA_AGENDADA));
        espera("RN-05 jornada ocupada sugiere fechas", ReglaNegocioException.class, () -> age.agendar(new AgendaUseCase.AgendarCitaComando(id1, tat.id(), miercoles, Jornada.MANANA, null), asistente));
        age.agendar(new AgendaUseCase.AgendarCitaComando(id1, tat.id(), miercoles, Jornada.TARDE, null), asistente);
        espera("SWR-15 tercera cita del día", ReglaNegocioException.class, () -> age.agendar(new AgendaUseCase.AgendarCitaComando(id1, tat.id(), miercoles, Jornada.MANANA, null), asistente));
        check("día lleno sin disponibilidad", !age.disponibilidad(tat.id(), miercoles, miercoles).get(0).disponible());
        check("domingo cerrado", !age.disponibilidad(tat.id(), LocalDate.of(2026, 10, 25), LocalDate.of(2026, 10, 25)).get(0).disponible());
        List<CitaAgenda> lista = age.citas(miercoles, miercoles, null, asistente);
        age.marcarRealizada(lista.get(0).cita().getId(), tatuador);
        age.marcarRealizada(lista.get(1).cita().getId(), tatuador);
        espera("RN-07 no cierra con saldo", ReglaNegocioException.class, () -> age.completarServicio(id1, asistente));
        pag.registrarPagoManual(id1, new BigDecimal("910000"), MetodoPago.EFECTIVO, asistente);
        check("servicio completado", age.completarServicio(id1, asistente).solicitud().getEstado() == EstadoSolicitud.SERVICIO_COMPLETADO);

        // ---------------- F-07
        SolicitudDetalle s2 = sol.registrar(new SolicitudUseCase.RegistrarSolicitudComando(TipoServicio.COMISION, "Retrato de la abuela", "Retrato al óleo de mi abuela con flores", CategoriaTamano.MEDIANO, Estilo.ILUSTRACION, null, "Calle 10 # 20-30 Barrio Centro", "Pereira", null), cliente);
        Long id2 = s2.solicitud().getId();
        // (350000 + 40000*6) * 1.15 = 678,500 → 679,000 (redondeo) + 28,000 envío nacional
        check("RN-09 comisión con envío nacional → " + s2.cotizacion().getValorCalculado(), s2.cotizacion().getValorCalculado().compareTo(new BigDecimal("707000")) == 0);
        cot.confirmar(id2, null, null, tatuador);
        cot.decidir(id2, true, cliente);
        espera("RN-10 anticipo < 50%", ReglaNegocioException.class, () -> pag.iniciarPago(id2, new BigDecimal("300000"), MetodoPago.NEQUI, cliente));
        espera("RN-10 sin anticipo no hay obra", ReglaNegocioException.class, () -> av.cambiarEstadoObra(id2, EstadoObra.EN_PRODUCCION, null, tatuador));
        CheckoutVista ck3 = pag.iniciarPago(id2, null, MetodoPago.NEQUI, cliente);
        check("anticipo por defecto = 50% (353.500)", ck3.pago().getMonto().compareTo(new BigDecimal("353500")) == 0);
        pag.procesarRespuestaPasarela(ck3.pago().getReferencia(), true, "ok");
        check("obra creada en espera", or.buscarPorSolicitud(id2).get().getEstado() == EstadoObra.EN_ESPERA);
        espera("no salta a terminada", ReglaNegocioException.class, () -> av.cambiarEstadoObra(id2, EstadoObra.TERMINADA, null, tatuador));
        av.cambiarEstadoObra(id2, EstadoObra.EN_PRODUCCION, null, tatuador);
        av.registrarAvance(new AvanceObraUseCase.RegistrarAvanceComando(id2, "Boceto aprobado", "Paleta cálida", 35, null), tatuador);
        check("progreso 35%", or.buscarPorSolicitud(id2).get().getProgreso() == 35);
        check("cliente consulta su obra", sol.detalle(id2, cliente).obra().getEstado() == EstadoObra.EN_PRODUCCION);
        av.cambiarEstadoObra(id2, EstadoObra.TERMINADA, null, tatuador);
        espera("SWR-20 no se envía con saldo pendiente", ReglaNegocioException.class, () -> av.cambiarEstadoObra(id2, EstadoObra.ENVIADA, "GUIA-1", tatuador));
        CheckoutVista ck4 = pag.iniciarPago(id2, null, MetodoPago.TARJETA, cliente);
        check("saldo restante = 353.500", ck4.pago().getTipo() == TipoPago.SALDO && ck4.pago().getMonto().compareTo(new BigDecimal("353500")) == 0);
        pag.procesarRespuestaPasarela(ck4.pago().getReferencia(), true, "ok");
        check("obra enviada", av.cambiarEstadoObra(id2, EstadoObra.ENVIADA, "GUIA-1", tatuador).solicitud().getEstado() == EstadoSolicitud.ENVIADA);

        // ---------------- F-08
        HistorialCliente h = his.historial(cliente.id(), asistente);
        check("2 servicios en historial", h.totalServicios() == 2);
        check("total pagado = 2.007.000 → " + h.totalPagado(), h.totalPagado().compareTo(new BigDecimal("2007000")) == 0);
        check("servicios con comprobantes", h.servicios().stream().allMatch(x -> !x.comprobantes().isEmpty()));
        check("cliente ve su propio historial", his.historial(cliente.id(), cliente).totalServicios() == 2);
        espera("cliente no ve historial ajeno", AccesoDenegadoException.class, () -> his.historial(cliente.id(), intruso));
        check("búsqueda de clientes sin tildes", his.clientes("mendez", asistente).size() == 1);

        // ---------------- Extra
        SolicitudDetalle s3 = sol.registrar(new SolicitudUseCase.RegistrarSolicitudComando(TipoServicio.TATUAJE, "Golondrina", "Golondrina pequeña en la muñeca", CategoriaTamano.PEQUENO, Estilo.LINEA_FINA, "Muñeca", null, null, null), cliente);
        reloj.avanzarHoras(73);
        check("CU-02 4.a alerta 72h", cot.alertarCotizacionesSinConfirmar() == 1);
        cot.confirmar(s3.solicitud().getId(), null, null, tatuador);
        check("rechazo → cancelada", cot.decidir(s3.solicitud().getId(), false, cliente).solicitud().getEstado() == EstadoSolicitud.CANCELADA);
        TableroVista tv = tab.tablero(asistente);
        check("tablero con ingresos " + tv.ingresosMes(), tv.ingresosMes().signum() > 0);
        check("bandeja finalizadas = 3", sol.listar(null, null, "FINALIZADAS", asistente).size() == 3);

    }

    static class MutableClock extends Clock {
        Instant instante;
        final ZoneId zona = ZoneId.of("America/Bogota");
        MutableClock(LocalDateTime inicio) { this.instante = inicio.atZone(zona).toInstant(); }
        void avanzarHoras(long h) { instante = instante.plus(Duration.ofHours(h)); }
        public ZoneId getZone() { return zona; }
        public Clock withZone(ZoneId z) { return this; }
        public Instant instant() { return instante; }
    }
}
