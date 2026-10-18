import {
  CategoriaTamano, EstadoCita, EstadoObra, EstadoPago, EstadoPagoServicio, EstadoSolicitud, Estilo, Jornada,
  MetodoPago, Rol, TipoComprobante, TipoPago, TipoServicio
} from './modelos';

/** Etiquetas en español para los valores del dominio (espejo de los enums del backend). */

export const TIPOS_SERVICIO: Record<TipoServicio, string> = {
  TATUAJE: 'Tatuaje de autor',
  COMISION: 'Obra por comisión'
};

export const TAMANOS_TATUAJE: Record<CategoriaTamano, string> = {
  PEQUENO: 'Pequeño · hasta 7 cm',
  MEDIANO: 'Mediano · 8 a 15 cm',
  GRANDE: 'Grande · 16 a 25 cm',
  EXTRA_GRANDE: 'Extra grande · más de 25 cm'
};

export const FORMATOS_COMISION: Record<CategoriaTamano, string> = {
  PEQUENO: 'Formato A5 · 15 × 21 cm',
  MEDIANO: 'Formato A4 · 21 × 30 cm',
  GRANDE: 'Formato A3 · 30 × 42 cm',
  EXTRA_GRANDE: 'Formato A2 · 42 × 59 cm'
};

export const ESTILOS: Record<Estilo, string> = {
  LINEA_FINA: 'Línea fina',
  BLACKWORK: 'Blackwork',
  MICRORREALISMO: 'Microrrealismo',
  REALISMO_SOMBRAS: 'Realismo en sombras',
  ABSTRACTO: 'Abstracto',
  BOTANICO: 'Botánico',
  LETTERING: 'Lettering de autor',
  ILUSTRACION: 'Ilustración / óleo'
};

export const ESTADOS_SOLICITUD: Record<EstadoSolicitud, string> = {
  RECIBIDA: 'Recibida',
  COTIZACION_GENERADA: 'Cotización lista',
  COTIZACION_ACEPTADA: 'Cotización aceptada',
  PAGO_EN_PROCESO: 'Pago en proceso',
  PAGO_PARCIAL_CONFIRMADO: 'Pago inicial confirmado',
  CITA_AGENDADA: 'Cita agendada',
  EN_PRODUCCION: 'En producción',
  TERMINADA: 'Obra terminada',
  ENVIADA: 'Obra enviada',
  SERVICIO_COMPLETADO: 'Servicio completado',
  CANCELADA: 'Cancelada'
};

export const TONO_ESTADO: Record<EstadoSolicitud, string> = {
  RECIBIDA: 'sello-info',
  COTIZACION_GENERADA: 'sello-alerta',
  COTIZACION_ACEPTADA: 'sello-sepia',
  PAGO_EN_PROCESO: 'sello-alerta',
  PAGO_PARCIAL_CONFIRMADO: 'sello-sepia',
  CITA_AGENDADA: 'sello-exito',
  EN_PRODUCCION: 'sello-exito',
  TERMINADA: 'sello-exito',
  ENVIADA: 'sello-neutro',
  SERVICIO_COMPLETADO: 'sello-neutro',
  CANCELADA: 'sello-peligro'
};

export const ESTADOS_OBRA: Record<EstadoObra, string> = {
  EN_ESPERA: 'En espera',
  EN_PRODUCCION: 'En producción',
  TERMINADA: 'Terminada',
  ENVIADA: 'Enviada'
};

export const TIPOS_PAGO: Record<TipoPago, string> = {
  ABONO: 'Abono',
  ANTICIPO: 'Anticipo',
  SALDO: 'Saldo'
};

export const ESTADOS_PAGO: Record<EstadoPago, string> = {
  PENDIENTE: 'En proceso',
  CONFIRMADO: 'Confirmado',
  RECHAZADO: 'Rechazado',
  EXPIRADO: 'Expirado'
};

export const TONO_PAGO: Record<EstadoPago, string> = {
  PENDIENTE: 'sello-alerta',
  CONFIRMADO: 'sello-exito',
  RECHAZADO: 'sello-peligro',
  EXPIRADO: 'sello-neutro'
};

export const ESTADOS_PAGO_SERVICIO: Record<EstadoPagoServicio, string> = {
  PENDIENTE: 'Sin pagos',
  PARCIAL: 'Pago parcial',
  COMPLETO: 'Pagado'
};

export const TONO_PAGO_SERVICIO: Record<EstadoPagoServicio, string> = {
  PENDIENTE: 'sello-neutro',
  PARCIAL: 'sello-alerta',
  COMPLETO: 'sello-exito'
};

export const METODOS_PAGO: Record<MetodoPago, string> = {
  TARJETA: 'Tarjeta',
  PSE: 'PSE',
  NEQUI: 'Nequi',
  TRANSFERENCIA: 'Transferencia',
  EFECTIVO: 'Efectivo'
};

export const JORNADAS: Record<Jornada, string> = {
  MANANA: 'Mañana · 9:00 a 13:00',
  TARDE: 'Tarde · 14:00 a 18:00'
};

export const JORNADAS_CORTAS: Record<Jornada, string> = {
  MANANA: 'Mañana',
  TARDE: 'Tarde'
};

export const ESTADOS_CITA: Record<EstadoCita, string> = {
  PROGRAMADA: 'Programada',
  REALIZADA: 'Realizada',
  CANCELADA: 'Cancelada'
};

export const TIPOS_COMPROBANTE: Record<TipoComprobante, string> = {
  COTIZACION_ACEPTADA: 'Cotización aceptada',
  PAGO: 'Comprobante de pago',
  CITA_AGENDADA: 'Cita confirmada'
};

export const ROLES: Record<Rol, string> = {
  CLIENTE: 'Cliente',
  ASISTENTE: 'Asistente',
  TATUADOR: 'Tatuador / Artista',
  ADMINISTRADOR: 'Administrador'
};

export function tamanoPara(tipo: TipoServicio, categoria: CategoriaTamano): string {
  return tipo === 'TATUAJE' ? TAMANOS_TATUAJE[categoria] : FORMATOS_COMISION[categoria];
}

export function opciones<T extends string>(mapa: Record<T, string>): { valor: T; etiqueta: string }[] {
  return (Object.keys(mapa) as T[]).map(valor => ({ valor, etiqueta: mapa[valor] }));
}
