import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CategoriaTamano, CheckoutVista, CitaAgenda, ClienteResumen, Comprobante, DisponibilidadDia, EstadoObra,
  EstadoPago, EstadoSolicitud, Estilo, HistorialCliente, Jornada, MetodoPago, Notificacion, ObraVista, Pago,
  PagoVista, ResultadoCotizacion, Rol, SolicitudDetalle, SolicitudResumen, TableroVista, Tarifa,
  TipoNotificacion, TipoServicio, Usuario
} from './modelos';

/** Cliente del API REST de HandgelusApp (adaptador de entrada web del backend). */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api';

  private params(valores: Record<string, string | number | null | undefined>): HttpParams {
    let p = new HttpParams();
    Object.entries(valores).forEach(([k, v]) => {
      if (v !== null && v !== undefined && v !== '') {
        p = p.set(k, String(v));
      }
    });
    return p;
  }

  // ---------- F-01 · Usuarios
  usuarios(rol?: Rol | null): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(`${this.base}/usuarios`, { params: this.params({ rol }) });
  }

  crearUsuario(datos: { nombre: string; correo: string; telefono: string; password: string; rol: Rol }): Observable<Usuario> {
    return this.http.post<Usuario>(`${this.base}/usuarios`, datos);
  }

  actualizarUsuario(id: number, datos: { nombre: string; telefono: string; rol: Rol; nuevaPassword?: string | null }): Observable<Usuario> {
    return this.http.put<Usuario>(`${this.base}/usuarios/${id}`, datos);
  }

  cambiarEstadoUsuario(id: number, activo: boolean): Observable<Usuario> {
    return this.http.patch<Usuario>(`${this.base}/usuarios/${id}/estado`, { activo });
  }

  tatuadores(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(`${this.base}/usuarios/tatuadores`);
  }

  // ---------- F-02 · Tarifas
  tarifas(): Observable<Tarifa[]> {
    return this.http.get<Tarifa[]>(`${this.base}/tarifas`);
  }

  guardarTarifa(t: {
    tipoServicio: TipoServicio; categoria: CategoriaTamano; valorBase: number; valorHora: number;
    horasEstimadas: number; tarifaFija: boolean; descripcion: string | null;
  }): Observable<Tarifa> {
    return this.http.put<Tarifa>(`${this.base}/tarifas`, t);
  }

  estimar(tipo: TipoServicio, categoria: CategoriaTamano, estilo: Estilo | null, ciudad: string | null): Observable<ResultadoCotizacion> {
    return this.http.get<ResultadoCotizacion>(`${this.base}/cotizaciones/estimar`,
      { params: this.params({ tipo, categoria, estilo, ciudad }) });
  }

  // ---------- F-03 · Solicitudes
  registrarSolicitud(datos: object, imagen: File | null): Observable<SolicitudDetalle> {
    const form = new FormData();
    form.append('datos', new Blob([JSON.stringify(datos)], { type: 'application/json' }));
    if (imagen) {
      form.append('imagen', imagen, imagen.name);
    }
    return this.http.post<SolicitudDetalle>(`${this.base}/solicitudes`, form);
  }

  solicitudes(filtro: { estado?: EstadoSolicitud | null; tipo?: TipoServicio | null; grupo?: string | null } = {}): Observable<SolicitudResumen[]> {
    return this.http.get<SolicitudResumen[]>(`${this.base}/solicitudes`, { params: this.params(filtro) });
  }

  solicitud(id: number): Observable<SolicitudDetalle> {
    return this.http.get<SolicitudDetalle>(`${this.base}/solicitudes/${id}`);
  }

  cancelarSolicitud(id: number, motivo: string | null): Observable<SolicitudDetalle> {
    return this.http.post<SolicitudDetalle>(`${this.base}/solicitudes/${id}/cancelar`, { motivo });
  }

  archivo(nombre: string): Observable<Blob> {
    return this.http.get(`${this.base}/archivos/${nombre}`, { responseType: 'blob' });
  }

  // ---------- F-04 · Cotización
  confirmarCotizacion(id: number, valorFinal: number | null, observaciones: string | null): Observable<SolicitudDetalle> {
    return this.http.post<SolicitudDetalle>(`${this.base}/solicitudes/${id}/cotizacion/confirmar`, { valorFinal, observaciones });
  }

  decidirCotizacion(id: number, acepta: boolean): Observable<SolicitudDetalle> {
    return this.http.post<SolicitudDetalle>(`${this.base}/solicitudes/${id}/cotizacion/decision`, { acepta });
  }

  // ---------- F-05 · Pagos
  iniciarPago(id: number, monto: number | null, metodo: MetodoPago): Observable<CheckoutVista> {
    return this.http.post<CheckoutVista>(`${this.base}/solicitudes/${id}/pagos`, { monto, metodo });
  }

  pagoManual(id: number, monto: number, metodo: MetodoPago): Observable<SolicitudDetalle> {
    return this.http.post<SolicitudDetalle>(`${this.base}/solicitudes/${id}/pagos/manual`, { monto, metodo });
  }

  pagos(estado?: EstadoPago | null): Observable<PagoVista[]> {
    return this.http.get<PagoVista[]>(`${this.base}/pagos`, { params: this.params({ estado }) });
  }

  checkout(referencia: string): Observable<CheckoutVista> {
    return this.http.get<CheckoutVista>(`${this.base}/pagos/checkout/${referencia}`);
  }

  responderPasarela(referencia: string, aprobar: boolean): Observable<Pago> {
    return this.http.post<Pago>(`${this.base}/pasarela-simulada/${referencia}/${aprobar ? 'aprobar' : 'rechazar'}`, {});
  }

  // ---------- F-06 · Agenda
  disponibilidad(desde: string, hasta: string, tatuadorId?: number | null): Observable<DisponibilidadDia[]> {
    return this.http.get<DisponibilidadDia[]>(`${this.base}/agenda/disponibilidad`, { params: this.params({ desde, hasta, tatuadorId }) });
  }

  sugerencias(desde: string, tatuadorId?: number | null, cantidad = 5): Observable<DisponibilidadDia[]> {
    return this.http.get<DisponibilidadDia[]>(`${this.base}/agenda/sugerencias`, { params: this.params({ desde, tatuadorId, cantidad }) });
  }

  citas(desde: string, hasta: string, tatuadorId?: number | null): Observable<CitaAgenda[]> {
    return this.http.get<CitaAgenda[]>(`${this.base}/agenda/citas`, { params: this.params({ desde, hasta, tatuadorId }) });
  }

  agendar(datos: { solicitudId: number; tatuadorId: number | null; fecha: string; jornada: Jornada; notas: string | null }): Observable<SolicitudDetalle> {
    return this.http.post<SolicitudDetalle>(`${this.base}/agenda/citas`, datos);
  }

  citaRealizada(id: number): Observable<CitaAgenda> {
    return this.http.post<CitaAgenda>(`${this.base}/agenda/citas/${id}/realizada`, {});
  }

  cancelarCita(id: number): Observable<CitaAgenda> {
    return this.http.post<CitaAgenda>(`${this.base}/agenda/citas/${id}/cancelar`, {});
  }

  completarServicio(id: number): Observable<SolicitudDetalle> {
    return this.http.post<SolicitudDetalle>(`${this.base}/solicitudes/${id}/completar`, {});
  }

  // ---------- F-07 · Avance de obra
  proyectos(): Observable<ObraVista[]> {
    return this.http.get<ObraVista[]>(`${this.base}/proyectos`);
  }

  cambiarEstadoObra(id: number, estado: EstadoObra, guiaEnvio: string | null): Observable<SolicitudDetalle> {
    return this.http.post<SolicitudDetalle>(`${this.base}/solicitudes/${id}/obra/estado`, { estado, guiaEnvio });
  }

  registrarAvance(id: number, datos: { titulo: string; descripcion: string | null; progreso: number | null }, imagen: File | null): Observable<SolicitudDetalle> {
    const form = new FormData();
    form.append('datos', new Blob([JSON.stringify(datos)], { type: 'application/json' }));
    if (imagen) {
      form.append('imagen', imagen, imagen.name);
    }
    return this.http.post<SolicitudDetalle>(`${this.base}/solicitudes/${id}/avances`, form);
  }

  // ---------- F-08 · Historial
  clientes(q?: string | null): Observable<ClienteResumen[]> {
    return this.http.get<ClienteResumen[]>(`${this.base}/clientes`, { params: this.params({ q }) });
  }

  historial(clienteId: number): Observable<HistorialCliente> {
    return this.http.get<HistorialCliente>(`${this.base}/clientes/${clienteId}/historial`);
  }

  miHistorial(): Observable<HistorialCliente> {
    return this.http.get<HistorialCliente>(`${this.base}/mi-historial`);
  }

  comprobante(id: number): Observable<Comprobante> {
    return this.http.get<Comprobante>(`${this.base}/comprobantes/${id}`);
  }

  // ---------- Tablero
  tablero(): Observable<TableroVista> {
    return this.http.get<TableroVista>(`${this.base}/tablero`);
  }

  notificaciones(tipo?: TipoNotificacion | null): Observable<Notificacion[]> {
    return this.http.get<Notificacion[]>(`${this.base}/notificaciones`, { params: this.params({ tipo }) });
  }

  marcarLeida(id: number): Observable<void> {
    return this.http.post<void>(`${this.base}/notificaciones/${id}/leida`, {});
  }
}
