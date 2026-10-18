import { Component, OnInit, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { ESTADOS_PAGO_SERVICIO, TIPOS_COMPROBANTE, TIPOS_SERVICIO, TONO_PAGO_SERVICIO } from '../../core/etiquetas';
import { mensajeError } from '../../core/interceptor';
import { Comprobante, HistorialCliente } from '../../core/modelos';
import { ComprobanteComponent } from '../../shared/comprobante.component';
import { CopPipe, EtiquetaPipe, FechaPipe, InicialesPipe } from '../../shared/pipes';
import { SelloEstadoComponent } from '../../shared/sello-estado.component';

/** F-08 · SWR-21: historial con tipo de servicio, fecha, valor, estado y comprobantes. */
@Component({
  selector: 'hg-historial-cliente',
  standalone: true,
  imports: [RouterLink, CopPipe, EtiquetaPipe, FechaPipe, InicialesPipe, SelloEstadoComponent, ComprobanteComponent],
  template: `
    <a routerLink="/estudio/clientes" class="volver pequeno">Volver a clientes</a>
    @if (historial(); as h) {
      <header class="cabeza">
        <span class="monograma">{{ h.cliente.nombre | iniciales }}</span>
        <div>
          <h1>{{ h.cliente.nombre }}</h1>
          <p class="suave">{{ h.cliente.correo }} · {{ h.cliente.telefono }} · cliente desde {{ h.cliente.fechaRegistro | fecha }}</p>
        </div>
      </header>

      <div class="rejilla rejilla-3 indicadores">
        <div class="indicador"><div class="valor">{{ h.totalServicios }}</div><div class="nombre">Servicios solicitados</div></div>
        <div class="indicador"><div class="valor">{{ h.serviciosCompletados }}</div><div class="nombre">Servicios completados</div></div>
        <div class="indicador"><div class="valor cifra">{{ h.totalPagado | cop }}</div><div class="nombre">Total pagado</div></div>
      </div>

      <div class="tabla-marco">
        <table class="tabla">
          <thead><tr><th>Servicio</th><th>Fecha</th><th>Estado</th><th>Pago</th><th class="derecha">Valor</th><th>Comprobantes</th></tr></thead>
          <tbody>
            @for (s of h.servicios; track s.solicitudId) {
              <tr>
                <td><a [routerLink]="['/estudio/solicitudes', s.solicitudId]"><strong>{{ s.titulo }}</strong></a>
                  <div class="mini suave">{{ s.tipoServicio | etiqueta: tipos }}</div></td>
                <td class="suave">{{ s.fecha | fecha }}</td>
                <td><hg-sello-estado [estado]="s.estado" /></td>
                <td><span class="sello" [class]="'sello ' + tono[s.estadoPago]">{{ s.estadoPago | etiqueta: estadosPago }}</span>
                  <div class="mini suave cifra">{{ s.pagado | cop }} pagado</div></td>
                <td class="derecha cifra">{{ s.valor | cop }}</td>
                <td>
                  @for (c of s.comprobantes; track c.id) {
                    <button type="button" class="enlace mini" (click)="comprobante.set(c)">{{ c.codigo }} · {{ c.tipo | etiqueta: tiposComprobante }}</button>
                  } @empty { <span class="mini suave">—</span> }
                </td>
              </tr>
            } @empty {
              <tr><td colspan="6" class="suave">Este cliente aún no tiene servicios.</td></tr>
            }
          </tbody>
        </table>
      </div>
    } @else if (error()) {
      <p class="aviso aviso-error">{{ error() }}</p>
    } @else {
      <p class="cargando">Cargando historial…</p>
    }

    @if (comprobante(); as c) {
      <hg-comprobante [comprobante]="c" (cerrar)="comprobante.set(null)" />
    }
  `,
  styles: [`
    .volver { display: inline-block; margin-bottom: var(--e-5); text-decoration: none; }
    .cabeza { display: flex; align-items: center; gap: var(--e-5); margin-bottom: var(--e-6); }
    .cabeza h1 { margin: 0; }
    .cabeza p { margin: var(--e-1) 0 0; }
    .monograma { width: 64px; height: 64px; border-radius: 50%; display: grid; place-items: center; background: var(--tinta); color: var(--lino); font-family: var(--f-display); font-size: 1.6rem; flex-shrink: 0; }
    .indicadores { margin-bottom: var(--e-5); }
    .enlace { display: block; background: none; border: 0; padding: 0; font: inherit; color: var(--sepia); cursor: pointer; text-align: left; }
  `]
})
export class HistorialClienteComponent implements OnInit {
  private readonly api = inject(ApiService);
  readonly id = input.required<string>();
  readonly historial = signal<HistorialCliente | null>(null);
  readonly error = signal<string | null>(null);
  readonly comprobante = signal<Comprobante | null>(null);
  readonly tipos = TIPOS_SERVICIO;
  readonly estadosPago = ESTADOS_PAGO_SERVICIO;
  readonly tono = TONO_PAGO_SERVICIO;
  readonly tiposComprobante = TIPOS_COMPROBANTE;

  ngOnInit(): void {
    this.api.historial(Number(this.id())).subscribe({
      next: h => this.historial.set(h),
      error: e => this.error.set(mensajeError(e))
    });
  }
}
