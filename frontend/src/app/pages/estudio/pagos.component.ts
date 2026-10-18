import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { ESTADOS_PAGO, METODOS_PAGO, TIPOS_PAGO, TONO_PAGO } from '../../core/etiquetas';
import { mensajeError } from '../../core/interceptor';
import { EstadoPago, PagoVista } from '../../core/modelos';
import { CopPipe, EtiquetaPipe, FechaPipe, InicialesPipe } from '../../shared/pipes';

/** F-05 · Control de pagos (Imagen 6): abonos, anticipos y saldos con su estado. */
@Component({
  selector: 'hg-pagos',
  standalone: true,
  imports: [RouterLink, CopPipe, EtiquetaPipe, FechaPipe, InicialesPipe],
  template: `
    <header class="encabezado-pagina">
      <div>
        <h1>Control de pagos</h1>
        <p>Registro de abonos, anticipos y saldos, en línea o recibidos en el atelier.</p>
      </div>
    </header>

    <div class="rejilla rejilla-3 indicadores">
      <div class="indicador"><div class="valor cifra">{{ confirmado() | cop }}</div><div class="nombre">Pagos confirmados</div></div>
      <div class="indicador"><div class="valor cifra">{{ enProceso() | cop }}</div><div class="nombre">En proceso con la pasarela</div></div>
      <div class="indicador"><div class="valor">{{ distribucion() }}</div><div class="nombre">Por método de pago</div></div>
    </div>

    <nav class="pestanas" aria-label="Estado del pago">
      @for (f of filtros; track f.texto) {
        <button type="button" class="pestana" [class.activa]="filtro() === f.valor" (click)="filtro.set(f.valor)">{{ f.texto }}</button>
      }
    </nav>

    @if (error()) {
      <p class="aviso aviso-error">{{ error() }}</p>
    } @else {
      <div class="tabla-marco">
        <table class="tabla">
          <thead>
            <tr><th>Cliente</th><th>Servicio</th><th>Concepto</th><th class="derecha">Monto</th><th>Fecha</th><th>Estado</th></tr>
          </thead>
          <tbody>
            @for (p of filtrados(); track p.pago.id) {
              <tr>
                <td><span class="monograma">{{ p.clienteNombre | iniciales }}</span> {{ p.clienteNombre }}</td>
                <td><a [routerLink]="['/estudio/solicitudes', p.pago.solicitudId]">{{ p.tituloSolicitud }}</a></td>
                <td>{{ p.pago.tipo | etiqueta: tiposPago }} <span class="mini suave">· {{ p.pago.metodo | etiqueta: metodos }}</span>
                  <div class="mini suave cifra">{{ p.pago.referencia }}</div></td>
                <td class="derecha cifra">{{ p.pago.monto | cop }}</td>
                <td class="suave">{{ p.pago.fechaCreacion | fecha }}</td>
                <td><span class="sello" [class]="'sello ' + tono[p.pago.estado]">{{ p.pago.estado | etiqueta: estados }}</span></td>
              </tr>
            } @empty {
              <tr><td colspan="6" class="suave">No hay pagos con este estado.</td></tr>
            }
          </tbody>
        </table>
      </div>
    }
  `,
  styles: [`
    .indicadores { margin-bottom: var(--e-5); }
    .indicador .valor { font-size: var(--t-xl); }
    .monograma { display: inline-grid; place-items: center; width: 28px; height: 28px; margin-right: 6px; border-radius: 50%; background: var(--arena-suave); font-size: .7rem; font-weight: 700; }
  `]
})
export class PagosComponent implements OnInit {
  private readonly api = inject(ApiService);
  readonly pagos = signal<PagoVista[]>([]);
  readonly filtro = signal<EstadoPago | null>(null);
  readonly error = signal<string | null>(null);
  readonly filtros: { valor: EstadoPago | null; texto: string }[] = [
    { valor: null, texto: 'Todos' },
    { valor: 'CONFIRMADO', texto: 'Confirmados' },
    { valor: 'PENDIENTE', texto: 'En proceso' },
    { valor: 'RECHAZADO', texto: 'Rechazados' },
    { valor: 'EXPIRADO', texto: 'Expirados' }
  ];
  readonly tiposPago = TIPOS_PAGO;
  readonly metodos = METODOS_PAGO;
  readonly estados = ESTADOS_PAGO;
  readonly tono = TONO_PAGO;

  readonly filtrados = computed(() => this.filtro() ? this.pagos().filter(p => p.pago.estado === this.filtro()) : this.pagos());
  readonly confirmado = computed(() => this.suma('CONFIRMADO'));
  readonly enProceso = computed(() => this.suma('PENDIENTE'));
  readonly distribucion = computed(() => {
    const confirmados = this.pagos().filter(p => p.pago.estado === 'CONFIRMADO');
    if (confirmados.length === 0) {
      return '—';
    }
    const enLinea = confirmados.filter(p => ['TARJETA', 'PSE', 'NEQUI'].includes(p.pago.metodo)).length;
    const porcentaje = Math.round((enLinea / confirmados.length) * 100);
    return `${porcentaje} % en línea · ${100 - porcentaje} % en atelier`;
  });

  ngOnInit(): void {
    this.api.pagos().subscribe({ next: p => this.pagos.set(p), error: e => this.error.set(mensajeError(e)) });
  }

  private suma(estado: EstadoPago): number {
    return this.pagos().filter(p => p.pago.estado === estado).reduce((t, p) => t + p.pago.monto, 0);
  }
}
