import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { EstadoSolicitud, TipoServicio } from '../core/modelos';

interface Etapa {
  nombre: string;
  estados: EstadoSolicitud[];
}

const ETAPAS_TATUAJE: Etapa[] = [
  { nombre: 'Recibida', estados: ['RECIBIDA'] },
  { nombre: 'Cotizada', estados: ['COTIZACION_GENERADA'] },
  { nombre: 'Aceptada', estados: ['COTIZACION_ACEPTADA', 'PAGO_EN_PROCESO'] },
  { nombre: 'Abono', estados: ['PAGO_PARCIAL_CONFIRMADO'] },
  { nombre: 'Cita', estados: ['CITA_AGENDADA'] },
  { nombre: 'Completado', estados: ['SERVICIO_COMPLETADO'] }
];

const ETAPAS_COMISION: Etapa[] = [
  { nombre: 'Recibida', estados: ['RECIBIDA'] },
  { nombre: 'Cotizada', estados: ['COTIZACION_GENERADA'] },
  { nombre: 'Aceptada', estados: ['COTIZACION_ACEPTADA', 'PAGO_EN_PROCESO'] },
  { nombre: 'Anticipo', estados: ['PAGO_PARCIAL_CONFIRMADO'] },
  { nombre: 'Producción', estados: ['EN_PRODUCCION'] },
  { nombre: 'Terminada', estados: ['TERMINADA'] },
  { nombre: 'Enviada', estados: ['ENVIADA'] }
];

/** Línea de tiempo del servicio (Imagen 4): etapas cumplidas, actual y pendientes. */
@Component({
  selector: 'hg-linea-tiempo',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ol class="linea" [class.cancelada]="estado() === 'CANCELADA'" aria-label="Etapas del servicio">
      @for (etapa of etapas(); track etapa.nombre; let i = $index) {
        <li [class.hecha]="i < actual()" [class.actual]="i === actual()"
            [attr.aria-current]="i === actual() ? 'step' : null">
          <span class="punto">
            @if (i < actual()) {
              <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.4"><path d="M5 12.5l4.5 4.5L19 7"/></svg>
            } @else {
              {{ i + 1 }}
            }
          </span>
          <span class="nombre">{{ etapa.nombre }}</span>
        </li>
      }
    </ol>
  `,
  styles: [`
    .linea {
      list-style: none; margin: 0; padding: 0;
      display: grid; grid-auto-flow: column; grid-auto-columns: 1fr;
      position: relative;
    }
    li { position: relative; display: flex; flex-direction: column; align-items: center; gap: 8px; text-align: center; }
    li::before {
      content: ""; position: absolute; top: 14px; left: -50%; width: 100%; height: 1px;
      background: var(--arena); z-index: 0;
    }
    li:first-child::before { display: none; }
    li.hecha::before, li.actual::before { background: var(--sepia); }
    .punto {
      position: relative; z-index: 1;
      width: 28px; height: 28px; border-radius: 50%;
      display: grid; place-items: center;
      font-size: .72rem; font-weight: 700;
      background: var(--papel); border: 1px solid var(--arena); color: var(--grafito);
    }
    .hecha .punto { background: var(--sepia); border-color: var(--sepia); color: var(--papel); }
    .actual .punto { border: 1.5px solid var(--sepia); color: var(--sepia); box-shadow: 0 0 0 4px var(--sepia-velo); }
    .nombre { font-size: .78rem; color: var(--grafito); }
    .actual .nombre { color: var(--tinta); font-weight: 700; }
    .cancelada { opacity: .45; filter: grayscale(1); }
    @media (max-width: 560px) { .nombre { font-size: .66rem; } .punto { width: 24px; height: 24px; } li::before { top: 12px; } }
  `]
})
export class LineaTiempoComponent {
  readonly estado = input.required<EstadoSolicitud>();
  readonly tipo = input.required<TipoServicio>();

  readonly etapas = computed(() => this.tipo() === 'TATUAJE' ? ETAPAS_TATUAJE : ETAPAS_COMISION);

  readonly actual = computed(() => {
    const indice = this.etapas().findIndex(e => e.estados.includes(this.estado()));
    if (this.estado() === 'SERVICIO_COMPLETADO' || this.estado() === 'ENVIADA') {
      return this.etapas().length;   // todas cumplidas
    }
    return indice < 0 ? 0 : indice;
  });
}
