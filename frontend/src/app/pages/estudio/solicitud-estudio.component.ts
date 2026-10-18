import { AsyncPipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { AvisosService } from '../../core/avisos.service';
import {
  ESTADOS_CITA, ESTADOS_OBRA, ESTADOS_PAGO, ESTILOS, JORNADAS, JORNADAS_CORTAS, METODOS_PAGO, tamanoPara,
  TIPOS_COMPROBANTE, TIPOS_PAGO, TIPOS_SERVICIO, TONO_PAGO
} from '../../core/etiquetas';
import { fechaLarga, hoy, iso, sumarDias } from '../../core/fechas';
import { mensajeError } from '../../core/interceptor';
import {
  Comprobante, DisponibilidadDia, EstadoObra, Jornada, MetodoPago, SolicitudDetalle, Usuario
} from '../../core/modelos';
import { ComprobanteComponent } from '../../shared/comprobante.component';
import { LineaTiempoComponent } from '../../shared/linea-tiempo.component';
import { CopPipe, EtiquetaPipe, FechaPipe, ImagenSeguraPipe } from '../../shared/pipes';
import { SelloEstadoComponent } from '../../shared/sello-estado.component';

const ESTADOS_EN_CURSO = ['PAGO_PARCIAL_CONFIRMADO', 'CITA_AGENDADA', 'EN_PRODUCCION', 'TERMINADA'];
const SIGUIENTE_OBRA: Record<EstadoObra, EstadoObra | null> = {
  EN_ESPERA: 'EN_PRODUCCION', EN_PRODUCCION: 'TERMINADA', TERMINADA: 'ENVIADA', ENVIADA: null
};

/** Ficha de la solicitud para el equipo: F-04 cotizar · F-05 pagos · F-06 agenda · F-07 avance. */
@Component({
  selector: 'hg-solicitud-estudio',
  standalone: true,
  imports: [RouterLink, FormsModule, AsyncPipe, CopPipe, EtiquetaPipe, FechaPipe, ImagenSeguraPipe,
    SelloEstadoComponent, LineaTiempoComponent, ComprobanteComponent],
  template: `
    <a routerLink="/estudio/solicitudes" class="volver pequeno">Volver a la bandeja</a>

    @if (detalle(); as d) {
      <header class="cabeza">
        <div>
          <p class="cursiva suave tipo">{{ d.solicitud.tipoServicio | etiqueta: tipos }} · solicitud #{{ d.solicitud.id }}</p>
          <h1>{{ d.solicitud.titulo }}</h1>
          <div class="fila">
            <hg-sello-estado [estado]="d.solicitud.estado" />
            <span class="pequeno suave">Recibida el {{ d.solicitud.fechaCreacion | fecha: 'hora' }}</span>
          </div>
        </div>
        @if (d.cliente; as c) {
          <a class="cliente" [routerLink]="['/estudio/clientes', c.id]">
            <strong>{{ c.nombre }}</strong>
            <span class="pequeno suave">{{ c.correo }}</span>
            <span class="pequeno suave">{{ c.telefono }}</span>
            <span class="mini">Ver historial</span>
          </a>
        }
      </header>

      <div class="lamina linea">
        <hg-linea-tiempo [estado]="d.solicitud.estado" [tipo]="d.solicitud.tipoServicio" />
      </div>

      <div class="rejilla rejilla-detalle cuerpo">
        <div class="pila">
          <!-- La idea -->
          <section class="lamina idea">
            <div class="idea-texto">
              <h3>La idea</h3>
              <p>{{ d.solicitud.descripcion }}</p>
              <dl class="datos">
                <dt>Tamaño</dt><dd>{{ tamano() }}</dd>
                @if (d.solicitud.estilo) { <dt>Estilo</dt><dd>{{ d.solicitud.estilo | etiqueta: estilos }}</dd> }
                @if (d.solicitud.zonaCuerpo) { <dt>Zona del cuerpo</dt><dd>{{ d.solicitud.zonaCuerpo }}</dd> }
                @if (d.solicitud.direccionDestino) { <dt>Envío a</dt><dd>{{ d.solicitud.direccionDestino }}, {{ d.solicitud.ciudadDestino }}</dd> }
              </dl>
            </div>
            @if (d.solicitud.imagenReferencia) {
              <figure class="referencia">
                @if (d.solicitud.imagenReferencia | imagenSegura | async; as url) {
                  <a [href]="url" target="_blank" rel="noopener"><img [src]="url" alt="Referencia del cliente"></a>
                } @else { <span class="mini suave">Referencia adjunta</span> }
              </figure>
            }
          </section>

          <!-- F-06 · Agenda (tatuajes) -->
          @if (d.solicitud.tipoServicio === 'TATUAJE' && (d.citas.length > 0 || puedeAgendar())) {
            <section class="lamina">
              <h3>Citas</h3>
              @for (c of d.citas; track c.cita.id) {
                <div class="cita fila-entre">
                  <div>
                    <strong>{{ c.cita.fecha | fecha: 'larga' }}</strong>
                    <span class="pequeno suave"> · {{ c.cita.jornada | etiqueta: jornadas }}</span>
                    <div class="mini suave">Sesión {{ c.cita.numeroSesion }} · {{ c.tatuadorNombre }} @if (c.cita.notas) { · {{ c.cita.notas }} }</div>
                  </div>
                  <div class="fila">
                    <span class="sello" [class.sello-exito]="c.cita.estado === 'PROGRAMADA'" [class.sello-neutro]="c.cita.estado !== 'PROGRAMADA'">
                      {{ c.cita.estado | etiqueta: estadosCita }}</span>
                    @if (c.cita.estado === 'PROGRAMADA') {
                      <button type="button" class="btn btn-fantasma btn-sm" [disabled]="trabajando()" (click)="citaRealizada(c.cita.id)">Marcar realizada</button>
                      <button type="button" class="btn btn-fantasma btn-sm" [disabled]="trabajando()" (click)="cancelarCita(c.cita.id)">Cancelar</button>
                    }
                  </div>
                </div>
              }

              @if (puedeAgendar()) {
                <form class="agendar" (ngSubmit)="agendar()">
                  <h4>{{ d.citas.length > 0 ? 'Agendar otra sesión' : 'Agendar cita' }}</h4>
                  <div class="rejilla rejilla-3">
                    <div class="campo">
                      <label for="tatuador">Tatuador</label>
                      <select id="tatuador" class="control" name="tatuador" [(ngModel)]="tatuadorId" (ngModelChange)="cargarSugerencias()">
                        @for (t of tatuadores(); track t.id) { <option [ngValue]="t.id">{{ t.nombre }}</option> }
                      </select>
                    </div>
                    <div class="campo">
                      <label for="fecha">Fecha</label>
                      <input id="fecha" class="control" type="date" name="fecha" [min]="minFecha" [(ngModel)]="fecha" (ngModelChange)="revisarFecha()">
                    </div>
                    <div class="campo">
                      <label for="jornada">Jornada</label>
                      <select id="jornada" class="control" name="jornada" [(ngModel)]="jornada">
                        <option [ngValue]="null" disabled>Selecciona</option>
                        @for (j of jornadasDelDia(); track j) { <option [ngValue]="j">{{ j | etiqueta: jornadas }}</option> }
                      </select>
                      @if (fecha && jornadasDelDia().length === 0) { <span class="error">Sin jornadas libres ese día.</span> }
                    </div>
                  </div>
                  <div class="campo">
                    <label for="notas">Notas para la sesión</label>
                    <input id="notas" class="control" name="notas" [(ngModel)]="notasCita" placeholder="Ej. Sesión de contornos">
                  </div>
                  @if (sugerencias().length > 0) {
                    <p class="mini suave">Próximas fechas disponibles:</p>
                    <div class="opciones sugerencias">
                      @for (s of sugerencias(); track s.fecha) {
                        <button type="button" class="opcion" [class.activa]="fecha === s.fecha" (click)="elegirSugerencia(s)">
                          {{ corta(s.fecha) }} · {{ jornadasTexto(s) }}
                        </button>
                      }
                    </div>
                  }
                  <button type="submit" class="btn btn-primario" [disabled]="trabajando() || !fecha || !jornada">Confirmar cita y enviar comprobante</button>
                </form>
              }

              @if (d.solicitud.estado === 'CITA_AGENDADA') {
                <div class="cierre fila-entre">
                  <span class="pequeno suave">Para cerrar el servicio, las sesiones deben estar realizadas y el pago completo.</span>
                  <button type="button" class="btn btn-secundario btn-sm" [disabled]="trabajando()" (click)="completar()">Cerrar servicio</button>
                </div>
              }
            </section>
          }

          <!-- F-07 · Obra por comisión -->
          @if (d.obra; as o) {
            <section class="lamina">
              <div class="fila-entre">
                <h3>Producción de la obra</h3>
                <span class="sello sello-info">{{ o.estado | etiqueta: estadosObra }}</span>
              </div>
              <div class="progreso grande"><span [style.width.%]="o.progreso"></span></div>
              <p class="pequeno suave">{{ o.progreso }} % · inicio {{ o.fechaInicio | fecha }}
                @if (o.guiaEnvio) { · guía {{ o.guiaEnvio }} }</p>
              @if (siguienteObra(); as sig) {
                <div class="fila avanzar">
                  @if (sig === 'ENVIADA') {
                    <input class="control guia" name="guia" [(ngModel)]="guiaEnvio" placeholder="Número de guía de envío">
                  }
                  <button type="button" class="btn btn-primario btn-sm" [disabled]="trabajando()" (click)="avanzarObra(sig)">
                    Marcar como {{ (sig | etiqueta: estadosObra).toLowerCase() }}
                  </button>
                </div>
                @if (sig === 'ENVIADA' && d.resumenPago.saldo > 0) {
                  <p class="aviso">La obra solo puede enviarse con el pago total confirmado. Saldo: {{ d.resumenPago.saldo | cop }}.</p>
                }
              }
            </section>
          }

          <!-- Bitácora y registro de avance -->
          @if (enCurso() || d.avances.length > 0) {
            <section class="lamina">
              <h3>Bitácora del proyecto</h3>
              @if (enCurso()) {
                <form class="avance" (ngSubmit)="registrarAvance()">
                  <div class="rejilla rejilla-2">
                    <div class="campo">
                      <label for="tituloAvance">Avance</label>
                      <input id="tituloAvance" class="control" name="tituloAvance" [(ngModel)]="avance.titulo" placeholder="Ej. Boceto aprobado" maxlength="150">
                    </div>
                    <div class="campo">
                      <label for="progreso">Progreso (%)</label>
                      <input id="progreso" class="control" type="number" min="0" max="100" name="progreso" [(ngModel)]="avance.progreso">
                    </div>
                  </div>
                  <div class="campo">
                    <label for="descAvance">Detalle</label>
                    <textarea id="descAvance" class="control" name="descAvance" [(ngModel)]="avance.descripcion" rows="2"></textarea>
                  </div>
                  <div class="fila-entre">
                    <label class="pequeno subir">
                      <input type="file" accept="image/png,image/jpeg,image/webp" (change)="elegirImagenAvance($event)">
                      {{ imagenAvance()?.name ?? 'Adjuntar foto del proceso' }}
                    </label>
                    <button type="submit" class="btn btn-secundario btn-sm" [disabled]="trabajando() || !avance.titulo">Registrar avance</button>
                  </div>
                </form>
              }
              <ol class="bitacora">
                @for (a of d.avances; track a.id) {
                  <li>
                    <span class="mini suave">{{ a.fecha | fecha: 'hora' }} @if (a.progreso !== null) { · {{ a.progreso }} % }</span>
                    <strong>{{ a.titulo }}</strong>
                    @if (a.descripcion) { <p class="pequeno">{{ a.descripcion }}</p> }
                    @if (a.imagen) {
                      @if (a.imagen | imagenSegura | async; as url) { <img [src]="url" alt="Foto del avance" class="miniatura"> }
                    }
                  </li>
                } @empty {
                  <li class="pequeno suave">Aún no hay avances registrados.</li>
                }
              </ol>
            </section>
          }
        </div>

        <aside class="pila">
          <!-- F-04 · Cotización -->
          <section class="lamina-hoja">
            <h3>Cotización</h3>
            @if (d.cotizacion; as q) {
              <dl class="datos desglose">
                @if (q.tarifaFija) {
                  <dt>Tarifa fija</dt><dd>{{ q.tarifaValorBase | cop }}</dd>
                } @else {
                  <dt>Valor base</dt><dd>{{ q.tarifaValorBase | cop }}</dd>
                  <dt>{{ q.horasEstimadas }} h × {{ q.tarifaValorHora | cop }}</dt><dd>{{ q.horasEstimadas * q.tarifaValorHora | cop }}</dd>
                  <dt>Factor de estilo</dt><dd>× {{ q.factorEstilo }}</dd>
                }
                @if (q.costoEnvio > 0) { <dt>Envío</dt><dd>{{ q.costoEnvio | cop }}</dd> }
                @if (q.sesionesEstimadas > 1) { <dt>Sesiones</dt><dd>{{ q.sesionesEstimadas }}</dd> }
                <dt>Calculado</dt><dd>{{ q.valorCalculado | cop }}</dd>
                @if (q.valorFinal !== null) { <dt class="final">Valor final</dt><dd class="final">{{ q.valorFinal | cop }}</dd> }
              </dl>
              @if (q.observaciones) { <p class="pequeno notas">{{ q.observaciones }}</p> }

              @if (d.solicitud.estado === 'RECIBIDA') {
                @if (esTatuador()) {
                  <form class="confirmar" (ngSubmit)="confirmarCotizacion()">
                    <div class="campo">
                      <label for="valorFinal">Valor final</label>
                      <input id="valorFinal" class="control cifra" type="number" min="1" step="1000" name="valorFinal" [(ngModel)]="valorFinal">
                      <span class="ayuda">Ajusta el valor sugerido si lo consideras necesario.</span>
                    </div>
                    <div class="campo">
                      <label for="obs">Notas para el cliente</label>
                      <textarea id="obs" class="control" name="obs" rows="3" [(ngModel)]="observaciones" placeholder="Duración, cuidados previos, alcance…"></textarea>
                    </div>
                    <button type="submit" class="btn btn-primario btn-bloque" [disabled]="trabajando() || !valorFinal">Confirmar y notificar al cliente</button>
                  </form>
                } @else {
                  <p class="aviso">Esperando que el artista revise y confirme el valor final.</p>
                }
              } @else if (d.solicitud.estado === 'COTIZACION_GENERADA') {
                <p class="aviso">Cotización enviada el {{ q.fechaConfirmacion | fecha: 'hora' }}. Esperando la respuesta del cliente.</p>
              }
            }
          </section>

          <!-- F-05 · Pagos -->
          <section class="lamina">
            <h3>Pagos</h3>
            <dl class="datos">
              <dt>Valor del servicio</dt><dd>{{ d.resumenPago.valorTotal | cop }}</dd>
              <dt>{{ d.resumenPago.tipoPagoInicial | etiqueta: tiposPago }} mínimo ({{ d.resumenPago.porcentajeMinimo }} %)</dt><dd>{{ d.resumenPago.montoMinimoInicial | cop }}</dd>
              <dt>Pagado</dt><dd>{{ d.resumenPago.pagado | cop }}</dd>
              <dt>Saldo</dt><dd>{{ d.resumenPago.saldo | cop }}</dd>
            </dl>
            <ul class="pagos">
              @for (p of d.pagos; track p.id) {
                <li class="fila-entre">
                  <div>
                    <strong class="pequeno">{{ p.tipo | etiqueta: tiposPago }}</strong>
                    <div class="mini suave">{{ p.metodo | etiqueta: metodos }} · {{ p.fechaCreacion | fecha }}</div>
                  </div>
                  <div class="derecha">
                    <span class="cifra pequeno">{{ p.monto | cop }}</span>
                    <span class="sello" [class]="'sello ' + tonoPago[p.estado]">{{ p.estado | etiqueta: estadosPago }}</span>
                  </div>
                </li>
              }
            </ul>
            @if (puedeRegistrarPago()) {
              <form class="manual" (ngSubmit)="registrarPago()">
                <h4>Registrar pago recibido</h4>
                <div class="campo">
                  <label for="montoManual">Monto</label>
                  <input id="montoManual" class="control cifra" type="number" min="1" step="1000" name="montoManual" [(ngModel)]="montoManual">
                </div>
                <div class="opciones">
                  @for (m of metodosManuales; track m) {
                    <button type="button" class="opcion" [class.activa]="metodoManual === m" (click)="metodoManual = m">{{ m | etiqueta: metodos }}</button>
                  }
                </div>
                <button type="submit" class="btn btn-secundario btn-bloque" [disabled]="trabajando() || !montoManual">Registrar y emitir comprobante</button>
              </form>
            }
          </section>

          @if (d.comprobantes.length > 0) {
            <section class="lamina">
              <h3>Comprobantes</h3>
              @for (c of d.comprobantes; track c.id) {
                <button type="button" class="enlace" (click)="comprobante.set(c)">
                  {{ c.tipo | etiqueta: tiposComprobante }} <span class="mini suave cifra">{{ c.codigo }}</span>
                </button>
              }
            </section>
          }

          @if (puedeArchivar()) {
            <button type="button" class="btn btn-fantasma" (click)="archivando.set(true)">Archivar solicitud</button>
          }
        </aside>
      </div>
    } @else if (cargando()) {
      <p class="cargando">Abriendo la solicitud…</p>
    } @else {
      <p class="aviso aviso-error">{{ error() }}</p>
    }

    @if (comprobante(); as c) {
      <hg-comprobante [comprobante]="c" (cerrar)="comprobante.set(null)" />
    }

    @if (archivando()) {
      <div class="velo" (click)="archivando.set(false)">
        <form class="dialogo" (click)="$event.stopPropagation()" (ngSubmit)="archivar()" role="dialog" aria-modal="true">
          <h3>Archivar solicitud</h3>
          <p class="suave pequeno">La solicitud pasará a "Cancelada" y el cliente recibirá un correo con el motivo.</p>
          <div class="campo">
            <label for="motivo">Motivo</label>
            <input id="motivo" class="control" name="motivo" [(ngModel)]="motivo" placeholder="Ej. El cliente no respondió">
          </div>
          <div class="acciones">
            <button type="button" class="btn btn-fantasma" (click)="archivando.set(false)">Volver</button>
            <button type="submit" class="btn btn-peligro" [disabled]="trabajando()">Archivar</button>
          </div>
        </form>
      </div>
    }
  `,
  styles: [`
    .volver { display: inline-block; margin-bottom: var(--e-5); text-decoration: none; }
    .cabeza { display: flex; justify-content: space-between; gap: var(--e-5); margin-bottom: var(--e-5); flex-wrap: wrap; }
    .tipo { margin: 0 0 var(--e-1); font-size: var(--t-lg); }
    .cliente { display: flex; flex-direction: column; padding: var(--e-4) var(--e-5); border: 1px solid var(--arena); border-radius: var(--r-lamina); background: var(--pergamino); text-decoration: none; color: var(--tinta); min-width: 220px; }
    .cliente .mini { color: var(--sepia); margin-top: var(--e-2); font-weight: 600; }
    .linea { padding: var(--e-5); }
    .cuerpo { margin-top: var(--e-5); }
    .idea { display: flex; gap: var(--e-5); }
    .idea-texto { flex: 1; }
    .referencia { margin: 0; width: 160px; flex-shrink: 0; aspect-ratio: 4/5; border-radius: var(--r-lamina); overflow: hidden; border: 1px solid var(--arena); display: grid; place-items: center; background: var(--papel); }
    .referencia img { width: 100%; height: 100%; object-fit: cover; }
    .cita { padding: var(--e-3) 0; border-bottom: 1px solid var(--arena-suave); }
    .agendar, .avance, .manual, .confirmar { margin-top: var(--e-4); padding-top: var(--e-4); border-top: 1px solid var(--arena); }
    .agendar h4, .manual h4 { margin-bottom: var(--e-4); }
    .sugerencias { margin-bottom: var(--e-4); }
    .cierre { margin-top: var(--e-4); padding-top: var(--e-4); border-top: 1px solid var(--arena); }
    .progreso.grande { height: 6px; margin: var(--e-2) 0 var(--e-3); }
    .avanzar { margin-top: var(--e-3); }
    .guia { max-width: 240px; }
    .subir { cursor: pointer; color: var(--sepia); font-weight: 600; }
    .subir input { display: none; }
    .bitacora { list-style: none; margin: var(--e-5) 0 0; padding: 0 0 0 var(--e-4); border-left: 1px solid var(--laton); }
    .bitacora li { position: relative; padding: 0 0 var(--e-4) var(--e-3); display: flex; flex-direction: column; }
    .bitacora li::before { content: ""; position: absolute; left: calc(-1 * var(--e-4) - 4px); top: 6px; width: 7px; height: 7px; border-radius: 50%; background: var(--laton); }
    .bitacora p { margin: var(--e-1) 0 0; }
    .miniatura { width: 140px; margin-top: var(--e-2); border-radius: var(--r-control); }
    .desglose .final { font-family: var(--f-display); font-size: var(--t-lg); color: var(--sepia); }
    .notas { margin: var(--e-3) 0 0; padding: var(--e-3); background: var(--pergamino); border-radius: var(--r-control); }
    .pagos { list-style: none; margin: var(--e-4) 0 0; padding: 0; }
    .pagos li { padding: var(--e-2) 0; border-top: 1px solid var(--arena-suave); }
    .derecha { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; }
    .manual .opciones { margin-bottom: var(--e-4); }
    .enlace { display: block; background: none; border: 0; padding: var(--e-1) 0; font: inherit; color: var(--sepia); cursor: pointer; text-align: left; }
    @media (max-width: 720px) { .idea { flex-direction: column; } }
  `]
})
export class SolicitudEstudioComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly avisos = inject(AvisosService);

  readonly id = input.required<string>();
  readonly detalle = signal<SolicitudDetalle | null>(null);
  readonly cargando = signal(true);
  readonly trabajando = signal(false);
  readonly error = signal<string | null>(null);
  readonly comprobante = signal<Comprobante | null>(null);
  readonly archivando = signal(false);
  readonly tatuadores = signal<Usuario[]>([]);
  readonly sugerencias = signal<DisponibilidadDia[]>([]);
  readonly jornadasDelDia = signal<Jornada[]>(['MANANA', 'TARDE']);
  readonly imagenAvance = signal<File | null>(null);

  // Formularios
  valorFinal: number | null = null;
  observaciones = '';
  montoManual: number | null = null;
  metodoManual: MetodoPago = 'EFECTIVO';
  readonly metodosManuales: MetodoPago[] = ['EFECTIVO', 'TRANSFERENCIA', 'NEQUI', 'TARJETA'];
  tatuadorId: number | null = null;
  fecha = '';
  jornada: Jornada | null = null;
  notasCita = '';
  guiaEnvio = '';
  motivo = '';
  avance: { titulo: string; descripcion: string; progreso: number | null } = {
    titulo: '',
    descripcion: '',
    progreso: null
  };
  readonly minFecha = iso(hoy());

  // Etiquetas
  readonly tipos = TIPOS_SERVICIO;
  readonly estilos = ESTILOS;
  readonly jornadas = JORNADAS;
  readonly estadosCita = ESTADOS_CITA;
  readonly estadosObra = ESTADOS_OBRA;
  readonly tiposPago = TIPOS_PAGO;
  readonly estadosPago = ESTADOS_PAGO;
  readonly tonoPago = TONO_PAGO;
  readonly metodos = METODOS_PAGO;
  readonly tiposComprobante = TIPOS_COMPROBANTE;

  readonly esTatuador = computed(() => this.auth.tieneRol('TATUADOR'));
  readonly tamano = computed(() => {
    const d = this.detalle();
    return d ? tamanoPara(d.solicitud.tipoServicio, d.solicitud.categoria) : '';
  });
  readonly enCurso = computed(() => ESTADOS_EN_CURSO.includes(this.detalle()?.solicitud.estado ?? ''));
  readonly puedeAgendar = computed(() => {
    const e = this.detalle()?.solicitud.estado;
    return e === 'PAGO_PARCIAL_CONFIRMADO' || e === 'CITA_AGENDADA';
  });
  readonly puedeRegistrarPago = computed(() => {
    const d = this.detalle();
    if (!d || d.resumenPago.pagoPendiente) {
      return false;
    }
    return d.solicitud.estado === 'COTIZACION_ACEPTADA' || (ESTADOS_EN_CURSO.includes(d.solicitud.estado) && d.resumenPago.saldo > 0);
  });
  readonly puedeArchivar = computed(() => {
    const e = this.detalle()?.solicitud.estado;
    return !!e && !['CANCELADA', 'SERVICIO_COMPLETADO', 'ENVIADA', 'EN_PRODUCCION', 'TERMINADA'].includes(e);
  });
  readonly siguienteObra = computed(() => {
    const o = this.detalle()?.obra;
    return o ? SIGUIENTE_OBRA[o.estado] : null;
  });

  ngOnInit(): void {
    this.cargar();
    this.api.tatuadores().subscribe(t => {
      this.tatuadores.set(t);
      const propio = this.auth.tieneRol('TATUADOR') ? t.find(x => x.id === this.auth.usuario()?.id) : undefined;
      this.tatuadorId = (propio ?? t[0])?.id ?? null;
      this.cargarSugerencias();
    });
  }

  private cargar(): void {
    this.api.solicitud(Number(this.id())).subscribe({
      next: d => this.aplicar(d),
      error: e => {
        this.error.set(mensajeError(e));
        this.cargando.set(false);
      }
    });
  }

  private aplicar(d: SolicitudDetalle, mensaje?: string): void {
    this.detalle.set(d);
    this.cargando.set(false);
    this.trabajando.set(false);
    this.valorFinal = d.cotizacion ? (d.cotizacion.valorFinal ?? d.cotizacion.valorCalculado) : null;
    this.montoManual = d.solicitud.estado === 'COTIZACION_ACEPTADA' ? d.resumenPago.montoMinimoInicial : d.resumenPago.saldo;
    if (mensaje) {
      this.avisos.exito(mensaje);
    }
  }

  private fallo(e: unknown): void {
    this.trabajando.set(false);
    this.avisos.error(mensajeError(e));
  }

  confirmarCotizacion(): void {
    this.trabajando.set(true);
    this.api.confirmarCotizacion(Number(this.id()), this.valorFinal, this.observaciones || null).subscribe({
      next: d => this.aplicar(d, 'Cotización confirmada. El cliente recibió el correo.'),
      error: e => this.fallo(e)
    });
  }

  registrarPago(): void {
    if (!this.montoManual) {
      return;
    }
    this.trabajando.set(true);
    this.api.pagoManual(Number(this.id()), this.montoManual, this.metodoManual).subscribe({
      next: d => this.aplicar(d, 'Pago registrado y comprobante enviado.'),
      error: e => this.fallo(e)
    });
  }

  cargarSugerencias(): void {
    this.api.sugerencias(iso(sumarDias(hoy(), 1)), this.tatuadorId, 6).subscribe({
      next: s => this.sugerencias.set(s),
      error: () => this.sugerencias.set([])
    });
    if (this.fecha) {
      this.revisarFecha();
    }
  }

  revisarFecha(): void {
    if (!this.fecha) {
      return;
    }
    this.api.disponibilidad(this.fecha, this.fecha, this.tatuadorId).subscribe({
      next: dias => {
        const libres = dias[0]?.jornadasLibres ?? [];
        this.jornadasDelDia.set(libres);
        if (!this.jornada || !libres.includes(this.jornada)) {
          this.jornada = libres[0] ?? null;
        }
      },
      error: () => this.jornadasDelDia.set([])
    });
  }

  elegirSugerencia(s: DisponibilidadDia): void {
    this.fecha = s.fecha;
    this.jornadasDelDia.set(s.jornadasLibres);
    this.jornada = s.jornadasLibres[0] ?? null;
  }

  corta(fecha: string): string {
    return fechaLarga(fecha);
  }

  jornadasTexto(s: DisponibilidadDia): string {
    return s.jornadasLibres.map(j => JORNADAS_CORTAS[j].toLowerCase()).join(' y ');
  }

  agendar(): void {
    if (!this.fecha || !this.jornada) {
      return;
    }
    this.trabajando.set(true);
    this.api.agendar({
      solicitudId: Number(this.id()), tatuadorId: this.tatuadorId, fecha: this.fecha, jornada: this.jornada,
      notas: this.notasCita || null
    }).subscribe({
      next: d => {
        this.fecha = '';
        this.notasCita = '';
        this.jornada = null;
        this.aplicar(d, 'Cita agendada. El cliente recibió su comprobante.');
        this.cargarSugerencias();
      },
      error: e => this.fallo(e)
    });
  }

  citaRealizada(citaId: number): void {
    this.trabajando.set(true);
    this.api.citaRealizada(citaId).subscribe({
      next: () => {
        this.avisos.exito('Sesión marcada como realizada.');
        this.cargar();
      },
      error: e => this.fallo(e)
    });
  }

  cancelarCita(citaId: number): void {
    this.trabajando.set(true);
    this.api.cancelarCita(citaId).subscribe({
      next: () => {
        this.avisos.info('Cita cancelada. Se notificó al cliente.');
        this.cargar();
        this.cargarSugerencias();
      },
      error: e => this.fallo(e)
    });
  }

  completar(): void {
    this.trabajando.set(true);
    this.api.completarServicio(Number(this.id())).subscribe({
      next: d => this.aplicar(d, 'Servicio cerrado. ¡Buen trabajo!'),
      error: e => this.fallo(e)
    });
  }

  avanzarObra(estado: EstadoObra): void {
    this.trabajando.set(true);
    this.api.cambiarEstadoObra(Number(this.id()), estado, estado === 'ENVIADA' ? (this.guiaEnvio || null) : null).subscribe({
      next: d => this.aplicar(d, `Obra marcada como ${ESTADOS_OBRA[estado].toLowerCase()}. Se notificó al cliente.`),
      error: e => this.fallo(e)
    });
  }

  elegirImagenAvance(evento: Event): void {
    const archivo = (evento.target as HTMLInputElement).files?.[0] ?? null;
    if (archivo && archivo.size > 5 * 1024 * 1024) {
      this.avisos.error('La imagen supera 5 MB.');
      return;
    }
    this.imagenAvance.set(archivo);
  }

  registrarAvance(): void {
    if (!this.avance.titulo) {
      return;
    }
    this.trabajando.set(true);
    const progreso = this.avance.progreso === null || this.avance.progreso === undefined || (this.avance.progreso as unknown) === ''
        ? null : Number(this.avance.progreso);
    this.api.registrarAvance(Number(this.id()), {
      titulo: this.avance.titulo, descripcion: this.avance.descripcion || null, progreso
    }, this.imagenAvance()).subscribe({
      next: d => {
        this.avance = {titulo: '', descripcion: '', progreso: null};
        this.imagenAvance.set(null);
        this.aplicar(d, 'Avance registrado. El cliente ya puede verlo.');
      },
      error: e => this.fallo(e)
    });
  }

  archivar(): void {
    this.trabajando.set(true);
    this.api.cancelarSolicitud(Number(this.id()), this.motivo || null).subscribe({
      next: d => {
        this.archivando.set(false);
        this.aplicar(d, 'Solicitud archivada.');
      },
      error: e => this.fallo(e)
    });
  }
}