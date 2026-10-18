package com.handgelus.infrastructure.config;

import com.handgelus.application.port.in.AgendaUseCase;
import com.handgelus.application.port.in.AutenticacionUseCase;
import com.handgelus.application.port.in.AvanceObraUseCase;
import com.handgelus.application.port.in.CotizacionUseCase;
import com.handgelus.application.port.in.GestionTarifasUseCase;
import com.handgelus.application.port.in.GestionUsuariosUseCase;
import com.handgelus.application.port.in.HistorialUseCase;
import com.handgelus.application.port.in.PagoUseCase;
import com.handgelus.application.port.in.SolicitudUseCase;
import com.handgelus.application.port.in.TableroUseCase;
import com.handgelus.application.port.out.AlmacenamientoArchivosPort;
import com.handgelus.application.port.out.AvanceRepositoryPort;
import com.handgelus.application.port.out.CifradoPasswordPort;
import com.handgelus.application.port.out.CitaRepositoryPort;
import com.handgelus.application.port.out.ComprobanteRepositoryPort;
import com.handgelus.application.port.out.CotizacionRepositoryPort;
import com.handgelus.application.port.out.EnvioCorreoPort;
import com.handgelus.application.port.out.NotificacionRepositoryPort;
import com.handgelus.application.port.out.ObraRepositoryPort;
import com.handgelus.application.port.out.PagoRepositoryPort;
import com.handgelus.application.port.out.PasarelaPagoPort;
import com.handgelus.application.port.out.SolicitudRepositoryPort;
import com.handgelus.application.port.out.TarifaRepositoryPort;
import com.handgelus.application.port.out.TokenPort;
import com.handgelus.application.port.out.UsuarioRepositoryPort;
import com.handgelus.application.service.AgendaService;
import com.handgelus.application.service.AutenticacionService;
import com.handgelus.application.service.AvanceObraService;
import com.handgelus.application.service.CotizacionService;
import com.handgelus.application.service.GestionTarifasService;
import com.handgelus.application.service.GestionUsuariosService;
import com.handgelus.application.service.HistorialService;
import com.handgelus.application.service.PagoService;
import com.handgelus.application.service.SolicitudService;
import com.handgelus.application.service.TableroService;
import com.handgelus.application.service.soporte.EmisorComprobantes;
import com.handgelus.application.service.soporte.EnsambladorDetalle;
import com.handgelus.application.service.soporte.Notificador;
import com.handgelus.domain.model.ParametrosNegocio;
import com.handgelus.domain.service.CalculadoraCotizacion;
import com.handgelus.domain.service.ReglasPago;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.RollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.time.Clock;
import java.time.ZoneId;
import java.util.List;

/**
 * Ensamblaje de la arquitectura hexagonal: el dominio y la capa de aplicación son Java puro
 * (sin anotaciones de Spring); aquí se conectan sus puertos con los adaptadores de infraestructura.
 */
@Configuration
public class BeanConfig {

    // ------------------------------------------------------------------ Transversales

    @Bean
    public Clock reloj() {
        return Clock.system(ZoneId.of("America/Bogota"));
    }

    @Bean
    public ParametrosNegocio parametrosNegocio(HandgelusProperties properties) {
        HandgelusProperties.Negocio n = properties.negocio();
        return new ParametrosNegocio(n.porcentajeAbonoTatuaje(), n.porcentajeAnticipoComision(),
                n.horasExpiracionPago(), n.horasAlertaCotizacion(), n.maxCitasPorDia(), n.imagenMaxBytes(),
                n.ciudadLocal(), n.costoEnvioLocal(), n.costoEnvioNacional(), n.horasPorSesion());
    }

    /**
     * Cada método público de los servicios de aplicación se ejecuta en una transacción,
     * sin que la capa de aplicación conozca Spring (se aplica por AOP desde la infraestructura).
     */
    @Bean
    public static Advisor transaccionesCasosDeUso(BeanFactory beanFactory) {
        RuleBasedTransactionAttribute atributo = new RuleBasedTransactionAttribute(
                TransactionDefinition.PROPAGATION_REQUIRED, List.of(new RollbackRuleAttribute(Exception.class)));
        NameMatchTransactionAttributeSource fuente = new NameMatchTransactionAttributeSource();
        fuente.addTransactionalMethod("*", atributo);
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionAttributeSource(fuente);
        interceptor.setBeanFactory(beanFactory);   // el TransactionManager se resuelve de forma diferida
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression("execution(public * com.handgelus.application.service..*(..))");
        return new DefaultPointcutAdvisor(pointcut, interceptor);
    }

    // ------------------------------------------------------------------ Dominio

    @Bean
    public CalculadoraCotizacion calculadoraCotizacion(ParametrosNegocio parametros) {
        return new CalculadoraCotizacion(parametros);
    }

    @Bean
    public ReglasPago reglasPago(ParametrosNegocio parametros) {
        return new ReglasPago(parametros);
    }

    // ------------------------------------------------------------------ Soporte de aplicación

    @Bean
    public Notificador notificador(NotificacionRepositoryPort notificaciones, EnvioCorreoPort correo, Clock reloj) {
        return new Notificador(notificaciones, correo, reloj);
    }

    @Bean
    public EmisorComprobantes emisorComprobantes(ComprobanteRepositoryPort comprobantes, Notificador notificador,
                                                 Clock reloj) {
        return new EmisorComprobantes(comprobantes, notificador, reloj);
    }

    @Bean
    public EnsambladorDetalle ensambladorDetalle(SolicitudRepositoryPort solicitudes, UsuarioRepositoryPort usuarios,
                                                 CotizacionRepositoryPort cotizaciones, PagoRepositoryPort pagos,
                                                 CitaRepositoryPort citas, ObraRepositoryPort obras,
                                                 AvanceRepositoryPort avances, ComprobanteRepositoryPort comprobantes,
                                                 ReglasPago reglasPago) {
        return new EnsambladorDetalle(solicitudes, usuarios, cotizaciones, pagos, citas, obras, avances, comprobantes,
                reglasPago);
    }

    // ------------------------------------------------------------------ Casos de uso (F-01 … F-08)

    @Bean
    public AutenticacionUseCase autenticacionUseCase(UsuarioRepositoryPort usuarios, CifradoPasswordPort cifrado,
                                                     TokenPort tokens, Clock reloj) {
        return new AutenticacionService(usuarios, cifrado, tokens, reloj);
    }

    @Bean
    public GestionUsuariosUseCase gestionUsuariosUseCase(UsuarioRepositoryPort usuarios, CifradoPasswordPort cifrado,
                                                         Clock reloj) {
        return new GestionUsuariosService(usuarios, cifrado, reloj);
    }

    @Bean
    public GestionTarifasUseCase gestionTarifasUseCase(TarifaRepositoryPort tarifas, Clock reloj) {
        return new GestionTarifasService(tarifas, reloj);
    }

    @Bean
    public SolicitudUseCase solicitudUseCase(SolicitudRepositoryPort solicitudes, UsuarioRepositoryPort usuarios,
                                             TarifaRepositoryPort tarifas, CotizacionRepositoryPort cotizaciones,
                                             PagoRepositoryPort pagos, AvanceRepositoryPort avances,
                                             AlmacenamientoArchivosPort almacenamiento,
                                             CalculadoraCotizacion calculadora, ReglasPago reglasPago,
                                             EnsambladorDetalle ensamblador, Notificador notificador,
                                             ParametrosNegocio parametros, Clock reloj) {
        return new SolicitudService(solicitudes, usuarios, tarifas, cotizaciones, pagos, avances, almacenamiento,
                calculadora, reglasPago, ensamblador, notificador, parametros, reloj);
    }

    @Bean
    public CotizacionUseCase cotizacionUseCase(SolicitudRepositoryPort solicitudes, CotizacionRepositoryPort cotizaciones,
                                               TarifaRepositoryPort tarifas, CalculadoraCotizacion calculadora,
                                               ReglasPago reglasPago, EnsambladorDetalle ensamblador,
                                               EmisorComprobantes emisor, Notificador notificador,
                                               ParametrosNegocio parametros, Clock reloj) {
        return new CotizacionService(solicitudes, cotizaciones, tarifas, calculadora, reglasPago, ensamblador, emisor,
                notificador, parametros, reloj);
    }

    @Bean
    public PagoUseCase pagoUseCase(SolicitudRepositoryPort solicitudes, PagoRepositoryPort pagos,
                                   ObraRepositoryPort obras, UsuarioRepositoryPort usuarios, PasarelaPagoPort pasarela,
                                   ReglasPago reglasPago, EnsambladorDetalle ensamblador, EmisorComprobantes emisor,
                                   Notificador notificador, ParametrosNegocio parametros, Clock reloj) {
        return new PagoService(solicitudes, pagos, obras, usuarios, pasarela, reglasPago, ensamblador, emisor,
                notificador, parametros, reloj);
    }

    @Bean
    public AgendaUseCase agendaUseCase(CitaRepositoryPort citas, SolicitudRepositoryPort solicitudes,
                                       UsuarioRepositoryPort usuarios, AvanceRepositoryPort avances,
                                       EnsambladorDetalle ensamblador, EmisorComprobantes emisor,
                                       Notificador notificador, ParametrosNegocio parametros, Clock reloj) {
        return new AgendaService(citas, solicitudes, usuarios, avances, ensamblador, emisor, notificador, parametros,
                reloj);
    }

    @Bean
    public AvanceObraUseCase avanceObraUseCase(SolicitudRepositoryPort solicitudes, ObraRepositoryPort obras,
                                               AvanceRepositoryPort avances, UsuarioRepositoryPort usuarios,
                                               AlmacenamientoArchivosPort almacenamiento,
                                               EnsambladorDetalle ensamblador, Notificador notificador,
                                               ParametrosNegocio parametros, Clock reloj) {
        return new AvanceObraService(solicitudes, obras, avances, usuarios, almacenamiento, ensamblador, notificador,
                parametros, reloj);
    }

    @Bean
    public HistorialUseCase historialUseCase(UsuarioRepositoryPort usuarios, SolicitudRepositoryPort solicitudes,
                                             CotizacionRepositoryPort cotizaciones, PagoRepositoryPort pagos,
                                             ComprobanteRepositoryPort comprobantes, EnsambladorDetalle ensamblador,
                                             ReglasPago reglasPago) {
        return new HistorialService(usuarios, solicitudes, cotizaciones, pagos, comprobantes, ensamblador, reglasPago);
    }

    @Bean
    public TableroUseCase tableroUseCase(SolicitudRepositoryPort solicitudes, CotizacionRepositoryPort cotizaciones,
                                         PagoRepositoryPort pagos, CitaRepositoryPort citas,
                                         NotificacionRepositoryPort notificaciones, EnsambladorDetalle ensamblador,
                                         ReglasPago reglasPago, Clock reloj) {
        return new TableroService(solicitudes, cotizaciones, pagos, citas, notificaciones, ensamblador, reglasPago,
                reloj);
    }
}
