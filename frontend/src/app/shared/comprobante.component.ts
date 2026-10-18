import { Component, input, output } from '@angular/core';
import { TIPOS_COMPROBANTE } from '../core/etiquetas';
import { Comprobante } from '../core/modelos';
import { CopPipe, EtiquetaPipe, FechaPipe } from './pipes';

/** Comprobante digital imprimible (RF-11). */
@Component({
  selector: 'hg-comprobante',
  standalone: true,
  imports: [CopPipe, EtiquetaPipe, FechaPipe],
  template: `
    <div class="velo" (click)="cerrar.emit()">
      <article class="dialogo comprobante" (click)="$event.stopPropagation()" role="dialog" aria-modal="true"
               [attr.aria-label]="'Comprobante ' + comprobante().codigo">
        <header class="fila-entre">
          <span class="marca">Handgelus</span>
          <span class="mini suave cifra">{{ comprobante().codigo }}</span>
        </header>
        <h3>{{ comprobante().tipo | etiqueta: tipos }}</h3>
        <p class="suave pequeno">Emitido el {{ comprobante().fechaEmision | fecha: 'hora' }}</p>
        <p class="detalle">{{ comprobante().detalle }}</p>
        <div class="total fila-entre">
          <span>Valor</span>
          <strong class="cifra">{{ comprobante().valor | cop }}</strong>
        </div>
        <p class="mini suave pie">Atelier Handgelus · Armenia, Quindío. Documento generado por HandgelusApp.</p>
        <div class="acciones no-imprimir">
          <button type="button" class="btn btn-fantasma" (click)="imprimir()">Imprimir</button>
          <button type="button" class="btn btn-primario" (click)="cerrar.emit()">Cerrar</button>
        </div>
      </article>
    </div>
  `,
  styles: [`
    .comprobante { border-top: 3px solid var(--laton); }
    .marca { font-family: var(--f-display); font-size: 1.4rem; font-style: italic; }
    header { margin-bottom: var(--e-5); padding-bottom: var(--e-3); border-bottom: 1px dashed var(--arena); }
    .detalle { font-size: var(--t-sm); line-height: 1.7; }
    .total { padding: var(--e-4) 0; border-top: 1px solid var(--arena); border-bottom: 1px solid var(--arena); }
    .total strong { font-family: var(--f-display); font-size: var(--t-xl); font-weight: 500; }
    .pie { margin-top: var(--e-4); }
  `]
})
export class ComprobanteComponent {
  readonly comprobante = input.required<Comprobante>();
  readonly cerrar = output<void>();
  readonly tipos = TIPOS_COMPROBANTE;

  imprimir(): void {
    window.print();
  }
}
