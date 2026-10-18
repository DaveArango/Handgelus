import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AvisosService } from '../../core/avisos.service';
import {
  ESTADOS_CITA, ESTADOS_OBRA, ESTADOS_PAGO, ESTILOS, JORNADAS, METODOS_PAGO, tamanoPara, TIPOS_COMPROBANTE,
  TIPOS_PAGO, TIPOS_SERVICIO, TONO_PAGO
} from '../../core/etiquetas';
import { mensajeError } from '../../core/interceptor';
import { Comprobante, MetodoPago, SolicitudDetalle } from '../../core/modelos';
import { ComprobanteComponent } from '../../shared/comprobante.component';
import { LineaTiempoComponent } from '../../shared/linea-tiempo.component';
import { CopPipe, EtiquetaPipe, FechaPipe, ImagenSeguraPipe } from '../../shared/pipes';
import { SelloEstadoComponent } from '../../shared/sello-estado.component';

/** Vista del proyecto para el cliente (Imagen 4): estado, cotización, pagos, citas, avance y comprobantes. */
@Component({
  selector: 'hg-solicitud-cliente',
  standalone: true,
  imports: [RouterLink, FormsModule, AsyncPipe, CopPipe, EtiquetaPipe, FechaPipe, ImagenSeguraPipe,
    SelloEstadoComponent, LineaTiempoComponent, ComprobanteComponent],
  template: `
    <section class="contenedor pagina">
      <a routerLink="/mi-espacio" class="volver pequeno">Volver a mis proyectos</a>

      @if (cargando()) {
        <p class="cargando">Abriendo tu proyecto…</p>
      } @else if (detalle(); as d) {
        <header class="cabeza">
          <div>
            <p class="cursiva suave tipo">{{ d.solicitud.tipoServicio | etiqueta: tipos }}</p>
            <h1>{{ d.solicitud.titulo }}</h1>
            <p class="descripcion">{{ d.solicitud.descripcion }}</p>
            <div class="fila">
              <hg-sello-estado [estado]="d.solicitud.estado" />
              <span class="pequeno suave">{{ tamano() }}</span>
              @if (d.solicitud.estilo) { <span class="pequeno suave">· {{ d.solicitud.estilo | etiqueta: estilos }}</span> }
              @if (d.solicitud.zonaCuerpo) { <span class="pequeno suave">· {{ d.solicitud.zonaCuerpo }}</span> }
              @if (d.solicitud.ciudadDestino) { <span class="pequeno suave">· Envío a {{ d.solicitud.ciudadDestino }}</span> }
            </div>
          </div>
          @if (d.solicitud.imagenReferencia) {
            <figure class="referencia">
              @if (d.solicitud.imagenReferencia | imagenSegura | async; as url) {
                <img [src]="url" alt="Imagen de referencia enviada">
              } @else {
                <span class="mini suave">Referencia adjunta</span>
              }
            </figure>
          }
        </header>

        <div class="lamina linea">
          <hg-linea-tiempo [estado]="d.solicitud.estado" [tipo]="d.solicitud.tipoServicio" />
          @if (d.solicitud.estado === 'CANCELADA') {
            <p class="aviso aviso-error">Esta solicitud fue cancelada. {{ d.solicitud.motivoCancelacion }}</p>
          } @else {
            <p class="mensaje-etapa">{{ mensajeEtapa() }}</p>
          }
        </div>

        <div class="rejilla rejilla-detalle cuerpo">
          <div class="pila">
            @if (d.obra) {
              <section class="lamina">
                <div class="fila-entre">
                  <h3>Avance de tu obra</h3>
                  <span class="sello sello-info">{{ d.obra.estado | etiqueta: estadosObra }}</span>
                </div>
                <div class="progreso grande"><span [style.width.%]="d.obra.progreso"></span></div>
                <p class="pequeno suave">{{ d.obra.progreso }} % completado
                  @if (d.obra.guiaEnvio) { · Guía de envío <strong>{{ d.obra.guiaEnvio }}</strong> }
                </p>
              </section>
            }

            @if (d.citas.length > 0) {
              <section class="lamina">
                <h3>Tus citas</h3>
                <ul class="lista-simple">
                  @for (c of d.citas; track c.cita.id) {
                    <li class="fila-entre">
                      <div>
                        <strong>{{ c.cita.fecha | fecha: 'larga' }}</strong>
                        <span class="pequeno suave"> · {{ c.cita.jornada | etiqueta: jornadas }}</span>
                        <div class="mini suave">Sesión {{ c.cita.numeroSesion }} con {{ c.tatuadorNombre }}</div>
                      </div>
                      <span class="sello" [class.sello-exito]="c.cita.estado === 'PROGRAMADA'"
                            [class.sello-neutro]="c.cita.estado !== 'PROGRAMADA'">{{ c.cita.estado | etiqueta: estadosCita }}</span>
                    </li>
                  }
                </ul>
                <p class="mini suave">Las cancelaciones con menos de 24 horas de anticipación no son reembolsables.</p>
              </section>
            }

            @if (d.avances.length > 0) {
              <section class="lamina">
                <h3>Bitácora</h3>
                <ol class="bitacora">
                  @for (a of d.avances; track a.id) {
                    <li>
                      <span class="mini suave">{{ a.fecha | fecha: 'hora' }}</span>
                      <strong>{{ a.titulo }}</strong>
                      @if (a.descripcion) { <p class="pequeno">{{ a.descripcion }}</p> }
                      @if (a.imagen) {
                        @if (a.imagen | imagenSegura | async; as url) { <img [src]="url" alt="Imagen del avance" class="miniatura"> }
                      }
                    </li>
                  }
                </ol>
              </section>
            }

            <section class="lamina">
              <h3>Pagos</h3>
              @if (d.pagos.length === 0) {
                <p class="pequeno suave">Aún no hay pagos registrados.</p>
              } @else {
                <ul class="lista-simple">
                  @for (p of d.pagos; track p.id) {
                    <li class="fila-entre">
                      <div>
                        <strong>{{ p.tipo | etiqueta: tiposPago }}</strong>
                        <span class="pequeno suave"> · {{ p.metodo | etiqueta: metodos }} · {{ p.fechaCreacion | fecha }}</span>
                        <div class="mini suave cifra">Ref. {{ p.referencia }}</div>
                      </div>
                      <div class="derecha">
                        <div class="cifra">{{ p.monto | cop }}</div>
                        <span class="sello" [class]="'sello ' + tonoPago[p.estado]">{{ p.estado | etiqueta: estadosPago }}</span>
                        @if (p.estado === 'PENDIENTE') {
                          <a class="mini continuar" [routerLink]="['/pasarela', p.referencia]">Continuar pago</a>
                        }
                      </div>
                    </li>
                  }
                </ul>
              }
            </section>

            @if (d.comprobantes.length > 0) {
              <section class="lamina">
                <h3>Comprobantes</h3>
                <ul class="lista-simple">
                  @for (c of d.comprobantes; track c.id) {
                    <li class="fila-entre">
                      <button type="button" class="enlace" (click)="comprobante.set(c)">
                        {{ c.tipo | etiqueta: tiposComprobante }} <span class="mini suave cifra">{{ c.codigo }}</span>
                      </button>
                      <span class="cifra pequeno">{{ c.valor | cop }}</span>
                    </li>
                  }
                </ul>
              </section>
            }
          </div>

          <aside class="pila">
            <section class="lamina-hoja consulta">
              <h3>Cotización</h3>
              @if (!d.cotizacion) {
                <p class="suave pequeno">Calculando…</p>
              } @else if (d.cotizacion.estado === 'PRELIMINAR') {
                <span class="rotulo">Valor de referencia</span>
                <p class="monto cifra">{{ d.cotizacion.valorCalculado | cop }}</p>
                <p class="pequeno suave">El artista está revisando tu idea. Te escribiremos cuando la cotización definitiva esté lista.</p>
              } @else {
                <span class="rotulo">Valor del servicio</span>
                <p class="monto cifra">{{ d.resumenPago.valorTotal | cop }}</p>
                <dl class="datos">
                  @if (d.cotizacion.sesionesEstimadas > 1) { <dt>Sesiones estimadas</dt><dd>{{ d.cotizacion.sesionesEstimadas }}</dd> }
                  @if (d.cotizacion.costoEnvio > 0) { <dt>Incluye envío</dt><dd>{{ d.cotizacion.costoEnvio | cop }}</dd> }
                  <dt>{{ d.resumenPago.tipoPagoInicial | etiqueta: tiposPago }} requerido ({{ d.resumenPago.porcentajeMinimo }} %)</dt>
                  <dd>{{ d.resumenPago.montoMinimoInicial | cop }}</dd>
                  <dt>Pagado</dt><dd>{{ d.resumenPago.pagado | cop }}</dd>
                  <dt>Saldo</dt><dd>{{ d.resumenPago.saldo | cop }}</dd>
                </dl>
                @if (d.cotizacion.observaciones) {
                  <div class="notas">
                    <span class="rotulo">Notas del artista</span>
                    <p class="pequeno">{{ d.cotizacion.observaciones }}</p>
                  </div>
                }
              }

              @if (d.solicitud.estado === 'COTIZACION_GENERADA') {
                <div class="acciones-consulta">
                  <button type="button" class="btn btn-primario btn-bloque" [disabled]="trabajando()" (click)="decidir(true)">
                    Aceptar cotización
                  </button>
                  <button type="button" class="btn btn-secundario btn-bloque" [disabled]="trabajando()" (click)="confirmarRechazo.set(true)">
                    Rechazar
                  </button>
                </div>
              }

              @if (puedePagar()) {
                <form class="pago" (ngSubmit)="pagar()">
                  <h4>{{ esPagoInicial() ? 'Pagar ' + (d.resumenPago.tipoPagoInicial | etiqueta: tiposPago).toLowerCase() : 'Pagar saldo' }}</h4>
                  <div class="campo">
                    <label for="monto">Monto</label>
                    <input id="monto" class="control cifra" type="number" name="monto" [(ngModel)]="monto"
                           [min]="montoMinimo()" [max]="d.resumenPago.saldo" step="1000">
                    <span class="ayuda">Entre {{ montoMinimo() | cop }} y {{ d.resumenPago.saldo | cop }}</span>
                  </div>
                  <div class="campo">
                    <span class="etiqueta">Método</span>
                    <div class="opciones">
                      @for (m of metodosEnLinea; track m) {
                        <button type="button" class="opcion" [class.activa]="metodo === m" (click)="metodo = m">{{ m | etiqueta: metodos }}</button>
                      }
                    </div>
                  </div>
                  <button type="submit" class="btn btn-primario btn-bloque" [disabled]="trabajando()">Ir a la pasarela de pago</button>
                </form>
              }

              @if (d.solicitud.estado === 'PAGO_EN_PROCESO') {
                <p class="aviso">Tu pago está en proceso. Si no se confirma en 48 horas, podrás intentarlo de nuevo.</p>
              }
            </section>

            @if (puedeCancelar()) {
              <button type="button" class="btn btn-fantasma" (click)="confirmarCancelacion.set(true)">Cancelar solicitud</button>
            }
          </aside>
        </div>
      } @else {
        <p class="aviso aviso-error">{{ error() }}</p>
      }
    </section>

    @if (comprobante(); as c) {
      <hg-comprobante [comprobante]="c" (cerrar)="comprobante.set(null)" />
    }

    @if (confirmarRechazo() || confirmarCancelacion()) {
      <div class="velo" (click)="cerrarDialogos()">
        <div class="dialogo" (click)="$event.stopPropagation()" role="dialog" aria-modal="true">
          <h3>{{ confirmarRechazo() ? '¿Rechazar la cotización?' : '¿Cancelar la solicitud?' }}</h3>
          <p class="suave">La solicitud quedará cancelada. Si cambias de idea, podrás enviar una nueva cuando quieras.</p>
          <div class="acciones">
            <button type="button" class="btn btn-fantasma" (click)="cerrarDialogos()">Volver</button>
            <button type="button" class="btn btn-peligro" [disabled]="trabajando()"
                    (click)="confirmarRechazo() ? decidir(false) : cancelar()">
              {{ confirmarRechazo() ? 'Rechazar cotización' : 'Cancelar solicitud' }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .pagina { padding-top: var(--e-6); }
    .volver { display: inline-block; margin-bottom: var(--e-5); text-decoration: none; }
    .cabeza { display: flex; justify-content: space-between; gap: var(--e-6); margin-bottom: var(--e-6); }
    .tipo { margin: 0 0 var(--e-1); font-size: var(--t-lg); }
    .descripcion { color: var(--grafito); max-width: 70ch; }
    .referencia { margin: 0; width: 180px; flex-shrink: 0; aspect-ratio: 4/5; border-radius: var(--r-lamina); overflow: hidden; background: var(--pergamino); border: 1px solid var(--arena); display: grid; place-items: center; }
    .referencia img { width: 100%; height: 100%; object-fit: cover; }
    .linea { padding: var(--e-6) var(--e-5) var(--e-5); }
    .mensaje-etapa { margin: var(--e-5) 0 0; padding: var(--e-4); background: var(--papel); border: 1px solid var(--arena-suave); border-radius: var(--r-control); font-size: var(--t-sm); max-width: none; }
    .cuerpo { margin-top: var(--e-5); }
    .lista-simple { list-style: none; margin: 0; padding: 0; }
    .lista-simple li { padding: var(--e-3) 0; border-bottom: 1px solid var(--arena-suave); }
    .lista-simple li:last-child { border-bottom: 0; }
    .derecha { text-align: right; display: flex; flex-direction: column; align-items: flex-end; gap: 4px; }
    .continuar { font-weight: 600; }
    .progreso.grande { height: 6px; margin: var(--e-2) 0 var(--e-3); }
    .bitacora { list-style: none; margin: 0; padding: 0 0 0 var(--e-4); border-left: 1px solid var(--laton); }
    .bitacora li { position: relative; padding: 0 0 var(--e-4) var(--e-3); display: flex; flex-direction: column; }
    .bitacora li::before { content: ""; position: absolute; left: calc(-1 * var(--e-4) - 4px); top: 6px; width: 7px; height: 7px; border-radius: 50%; background: var(--laton); }
    .bitacora p { margin: var(--e-1) 0 0; }
    .miniatura { width: 120px; margin-top: var(--e-2); border-radius: var(--r-control); }
    .consulta { padding: var(--e-5); }
    .monto { font-family: var(--f-display); font-size: var(--t-3xl); line-height: 1; margin: var(--e-1) 0 var(--e-4); }
    .notas { margin-top: var(--e-4); padding-top: var(--e-3); border-top: 1px solid var(--arena-suave); }
    .notas p { margin: 0; }
    .acciones-consulta { display: flex; flex-direction: column; gap: var(--e-2); margin-top: var(--e-5); }
    .pago { margin-top: var(--e-5); padding-top: var(--e-4); border-top: 1px solid var(--arena); }
    .pago h4 { margin-bottom: var(--e-4); }
    .enlace { background: none; border: 0; padding: 0; font: inherit; color: var(--sepia); cursor: pointer; text-align: left; }
    .enlace:hover { text-decoration: underline; }
    @media (max-width: 720px) { .cabeza { flex-direction: column; } .referencia { width: 140px; } }
  `]
})
export class SolicitudClienteComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly avisos = inject(AvisosService);
  private readonly router = inject(Router);

  readonly id = input.required<string>();
  readonly detalle = signal<SolicitudDetalle | null>(null);
  readonly cargando = signal(true);
  readonly trabajando = signal(false);
  readonly error = signal<string | null>(null);
  readonly comprobante = signal<Comprobante | null>(null);
  readonly confirmarRechazo = signal(false);
  readonly confirmarCancelacion = signal(false);

  monto: number | null = null;
  metodo: MetodoPago = 'TARJETA';
  readonly metodosEnLinea: MetodoPago[] = ['TARJETA', 'PSE', 'NEQUI'];

  readonly tipos = TIPOS_SERVICIO;
  readonly estilos = ESTILOS;
  readonly estadosObra = ESTADOS_OBRA;
  readonly estadosCita = ESTADOS_CITA;
  readonly jornadas = JORNADAS;
  readonly tiposPago = TIPOS_PAGO;
  readonly estadosPago = ESTADOS_PAGO;
  readonly tonoPago = TONO_PAGO;
  readonly metodos = METODOS_PAGO;
  readonly tiposComprobante = TIPOS_COMPROBANTE;

  readonly tamano = computed(() => {
    const d = this.detalle();
    return d ? tamanoPara(d.solicitud.tipoServicio, d.solicitud.categoria) : '';
  });

  readonly esPagoInicial = computed(() => this.detalle()?.solicitud.estado === 'COTIZACION_ACEPTADA');

  readonly puedePagar = computed(() => {
    const d = this.detalle();
    if (!d || d.resumenPago.pagoPendiente) {
      return false;
    }
    if (d.solicitud.estado === 'COTIZACION_ACEPTADA') {
      return true;
    }
    const conSaldo = ['PAGO_PARCIAL_CONFIRMADO', 'CITA_AGENDADA', 'EN_PRODUCCION', 'TERMINADA'];
    return conSaldo.includes(d.solicitud.estado) && d.resumenPago.saldo > 0;
  });

  readonly montoMinimo = computed(() => {
    const d = this.detalle();
    if (!d) {
      return 0;
    }
    return this.esPagoInicial() ? d.resumenPago.montoMinimoInicial : Math.min(1000, d.resumenPago.saldo);
  });

  readonly puedeCancelar = computed(() => {
    const e = this.detalle()?.solicitud.estado;
    return e === 'RECIBIDA' || e === 'COTIZACION_GENERADA' || e === 'COTIZACION_ACEPTADA';
  });

  readonly mensajeEtapa = computed(() => {
    const d = this.detalle();
    if (!d) {
      return '';
    }
    const tatuaje = d.solicitud.tipoServicio === 'TATUAJE';
    switch (d.solicitud.estado) {
      case 'RECIBIDA': return 'Recibimos tu solicitud. El artista revisará tu idea y confirmará el valor definitivo.';
      case 'COTIZACION_GENERADA': return 'Tu cotización está lista. Revísala y acéptala para continuar con el pago inicial.';
      case 'COTIZACION_ACEPTADA': return tatuaje
        ? 'Con el abono del 30 % reservamos la jornada de tu cita.'
        : 'Con el anticipo del 50 % el artista inicia la producción de tu obra.';
      case 'PAGO_EN_PROCESO': return 'Estamos esperando la confirmación de la pasarela de pago.';
      case 'PAGO_PARCIAL_CONFIRMADO': return tatuaje
        ? 'Recibimos tu abono. La asistente te contactará para acordar la fecha de tu cita.'
        : 'Recibimos tu anticipo. Tu obra entrará en producción muy pronto.';
      case 'CITA_AGENDADA': return 'Tu cita está confirmada. Recuerda llegar con la piel hidratada y descansado.';
      case 'EN_PRODUCCION': return 'El artista está trabajando en tu obra. Aquí verás cada avance.';
      case 'TERMINADA': return d.resumenPago.saldo > 0
        ? 'Tu obra está terminada. Paga el saldo para coordinar el envío.'
        : 'Tu obra está terminada y lista para enviarse.';
      case 'ENVIADA': return 'Tu obra va en camino. ¡Gracias por confiar en el atelier!';
      case 'SERVICIO_COMPLETADO': return 'Servicio completado. Gracias por llevar una pieza de Handgelus.';
      default: return '';
    }
  });

  ngOnInit(): void {
    this.cargar();
  }

  private cargar(): void {
    this.api.solicitud(Number(this.id())).subscribe({
      next: d => this.aplicar(d),
      error: e => { this.error.set(mensajeError(e)); this.cargando.set(false); }
    });
  }

  private aplicar(d: SolicitudDetalle): void {
    this.detalle.set(d);
    this.cargando.set(false);
    this.trabajando.set(false);
    this.monto = d.solicitud.estado === 'COTIZACION_ACEPTADA' ? d.resumenPago.montoMinimoInicial : d.resumenPago.saldo;
  }

  decidir(acepta: boolean): void {
    this.trabajando.set(true);
    this.api.decidirCotizacion(Number(this.id()), acepta).subscribe({
      next: d => {
        this.cerrarDialogos();
        this.aplicar(d);
        this.avisos.exito(acepta ? 'Cotización aceptada. Te enviamos el comprobante por correo.' : 'Cotización rechazada.');
      },
      error: e => { this.trabajando.set(false); this.avisos.error(mensajeError(e)); }
    });
  }

  pagar(): void {
    const d = this.detalle();
    if (!d || !this.monto) {
      return;
    }
    this.trabajando.set(true);
    this.api.iniciarPago(d.solicitud.id, this.monto, this.metodo).subscribe({
      next: c => this.router.navigate(['/pasarela', c.pago.referencia]),
      error: e => { this.trabajando.set(false); this.avisos.error(mensajeError(e)); }
    });
  }

  cancelar(): void {
    this.trabajando.set(true);
    this.api.cancelarSolicitud(Number(this.id()), null).subscribe({
      next: d => { this.cerrarDialogos(); this.aplicar(d); this.avisos.info('Solicitud cancelada.'); },
      error: e => { this.trabajando.set(false); this.avisos.error(mensajeError(e)); }
    });
  }

  cerrarDialogos(): void {
    this.confirmarRechazo.set(false);
    this.confirmarCancelacion.set(false);
  }
}
