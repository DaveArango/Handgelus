// Tipos que reflejan las respuestas del API de HandgelusApp.

export type Rol = 'CLIENTE' | 'ASISTENTE' | 'TATUADOR' | 'ADMINISTRADOR';
export type TipoServicio = 'TATUAJE' | 'COMISION';
export type CategoriaTamano = 'PEQUENO' | 'MEDIANO' | 'GRANDE' | 'EXTRA_GRANDE';
export type Estilo = 'LINEA_FINA' | 'BLACKWORK' | 'MICRORREALISMO' | 'REALISMO_SOMBRAS' | 'ABSTRACTO'
  | 'BOTANICO' | 'LETTERING' | 'ILUSTRACION';
export type EstadoSolicitud = 'RECIBIDA' | 'COTIZACION_GENERADA' | 'COTIZACION_ACEPTADA' | 'PAGO_EN_PROCESO'
  | 'PAGO_PARCIAL_CONFIRMADO' | 'CITA_AGENDADA' | 'EN_PRODUCCION' | 'TERMINADA' | 'ENVIADA'
  | 'SERVICIO_COMPLETADO' | 'CANCELADA';
export type EstadoCotizacion = 'PRELIMINAR' | 'CONFIRMADA' | 'ACEPTADA' | 'RECHAZADA';
export type TipoPago = 'ABONO' | 'ANTICIPO' | 'SALDO';
export type EstadoPago = 'PENDIENTE' | 'CONFIRMADO' | 'RECHAZADO' | 'EXPIRADO';
export type MetodoPago = 'TARJETA' | 'PSE' | 'NEQUI' | 'TRANSFERENCIA' | 'EFECTIVO';
export type EstadoPagoServicio = 'PENDIENTE' | 'PARCIAL' | 'COMPLETO';
export type Jornada = 'MANANA' | 'TARDE';
export type EstadoCita = 'PROGRAMADA' | 'REALIZADA' | 'CANCELADA';
export type EstadoObra = 'EN_ESPERA' | 'EN_PRODUCCION' | 'TERMINADA' | 'ENVIADA';
export type TipoComprobante = 'COTIZACION_ACEPTADA' | 'PAGO' | 'CITA_AGENDADA';
export type TipoNotificacion = 'CORREO_CLIENTE' | 'ALERTA_INTERNA';

export interface Usuario {
  id: number;
  nombre: string;
  correo: string;
  telefono: string;
  rol: Rol;
  rolEtiqueta: string;
  activo: boolean;
  fechaRegistro: string;
}

export interface Sesion {
  token: string;
  usuario: Usuario;
}

export interface Tarifa {
  id: number;
  tipoServicio: TipoServicio;
  categoria: CategoriaTamano;
  valorBase: number;
  valorHora: number;
  horasEstimadas: number;
  tarifaFija: boolean;
  descripcion: string | null;
  fechaActualizacion: string;
}

export interface Solicitud {
  id: number;
  clienteId: number;
  tipoServicio: TipoServicio;
  titulo: string;
  descripcion: string;
  categoria: CategoriaTamano;
  estilo: Estilo | null;
  zonaCuerpo: string | null;
  direccionDestino: string | null;
  ciudadDestino: string | null;
  imagenReferencia: string | null;
  estado: EstadoSolicitud;
  fechaCreacion: string;
  fechaCambioEstado: string;
  motivoCancelacion: string | null;
}

export interface Cotizacion {
  id: number;
  solicitudId: number;
  valorCalculado: number;
  valorFinal: number | null;
  observaciones: string | null;
  estado: EstadoCotizacion;
  tarifaValorBase: number;
  tarifaValorHora: number;
  horasEstimadas: number;
  tarifaFija: boolean;
  factorEstilo: number;
  costoEnvio: number;
  sesionesEstimadas: number;
  fechaEmision: string;
  fechaConfirmacion: string | null;
  fechaDecision: string | null;
}

export interface ResultadoCotizacion {
  valorTotal: number;
  subtotalTrabajo: number;
  tarifaValorBase: number;
  tarifaValorHora: number;
  horasEstimadas: number;
  tarifaFija: boolean;
  factorEstilo: number;
  costoEnvio: number;
  sesionesEstimadas: number;
}

export interface Pago {
  id: number;
  solicitudId: number;
  tipo: TipoPago;
  monto: number;
  estado: EstadoPago;
  metodo: MetodoPago;
  referencia: string;
  fechaCreacion: string;
  fechaConfirmacion: string | null;
}

export interface ResumenPago {
  valorTotal: number;
  pagado: number;
  saldo: number;
  estado: EstadoPagoServicio;
  tipoPagoInicial: TipoPago;
  porcentajeMinimo: number;
  montoMinimoInicial: number;
  pagoInicialConfirmado: boolean;
  pagoPendiente: boolean;
}

export interface Cita {
  id: number;
  solicitudId: number;
  tatuadorId: number;
  fecha: string;
  jornada: Jornada;
  estado: EstadoCita;
  numeroSesion: number;
  notas: string | null;
}

export interface CitaAgenda {
  cita: Cita;
  clienteNombre: string;
  tituloSolicitud: string;
  tatuadorNombre: string;
  estadoSolicitud: EstadoSolicitud | null;
}

export interface Obra {
  id: number;
  solicitudId: number;
  estado: EstadoObra;
  progreso: number;
  fechaInicio: string | null;
  fechaTerminada: string | null;
  fechaEnvio: string | null;
  guiaEnvio: string | null;
}

export interface RegistroAvance {
  id: number;
  solicitudId: number;
  autorId: number;
  titulo: string;
  descripcion: string | null;
  progreso: number | null;
  imagen: string | null;
  fecha: string;
}

export interface Comprobante {
  id: number;
  solicitudId: number;
  clienteId: number;
  tipo: TipoComprobante;
  codigo: string;
  detalle: string;
  valor: number;
  fechaEmision: string;
}

export interface SolicitudResumen {
  solicitud: Solicitud;
  clienteNombre: string;
  valor: number;
  cotizacionConfirmada: boolean;
  estadoPago: EstadoPagoServicio;
}

export interface SolicitudDetalle {
  solicitud: Solicitud;
  cliente: Usuario | null;
  cotizacion: Cotizacion | null;
  resumenPago: ResumenPago;
  pagos: Pago[];
  citas: CitaAgenda[];
  obra: Obra | null;
  avances: RegistroAvance[];
  comprobantes: Comprobante[];
}

export interface CheckoutVista {
  pago: Pago;
  urlCheckout: string | null;
  proveedor: string;
  concepto: string;
  clienteNombre: string;
  solicitudId: number;
}

export interface DisponibilidadDia {
  fecha: string;
  jornadasLibres: Jornada[];
  citasAgendadas: number;
  disponible: boolean;
}

export interface PagoVista {
  pago: Pago;
  clienteNombre: string;
  tituloSolicitud: string;
  tipoServicio: TipoServicio | null;
}

export interface ObraVista {
  solicitud: Solicitud;
  obra: Obra | null;
  clienteNombre: string;
  ultimoAvance: RegistroAvance | null;
  totalAvances: number;
}

export interface ClienteResumen {
  cliente: Usuario;
  servicios: number;
  serviciosActivos: number;
  ultimoServicio: string | null;
  totalPagado: number;
}

export interface ServicioHistorial {
  solicitudId: number;
  tipoServicio: TipoServicio;
  titulo: string;
  fecha: string;
  valor: number;
  estado: EstadoSolicitud;
  estadoPago: EstadoPagoServicio;
  pagado: number;
  comprobantes: Comprobante[];
}

export interface HistorialCliente {
  cliente: Usuario;
  servicios: ServicioHistorial[];
  totalPagado: number;
  totalServicios: number;
  serviciosCompletados: number;
}

export interface TableroVista {
  ingresosMes: number;
  saldosPendientes: number;
  serviciosConSaldo: number;
  solicitudesNuevas: number;
  cotizacionesPorConfirmar: number;
  pagosPendientes: number;
  obrasEnProduccion: number;
  alertasSinLeer: number;
  solicitudesPorEstado: Record<string, number>;
  proximasCitas: CitaAgenda[];
  ultimasSolicitudes: SolicitudResumen[];
}

export interface Notificacion {
  id: number;
  tipo: TipoNotificacion;
  destinatario: string;
  asunto: string;
  cuerpo: string;
  solicitudId: number | null;
  fechaEnvio: string;
  leida: boolean;
}

export interface RespuestaError {
  estado: number;
  mensaje: string;
  regla?: string | null;
  campos?: Record<string, string> | null;
}
