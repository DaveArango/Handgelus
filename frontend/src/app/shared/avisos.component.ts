import { Component, inject } from '@angular/core';
import { AvisosService } from '../core/avisos.service';

@Component({
  selector: 'hg-avisos',
  standalone: true,
  template: `
    <div class="avisos-flotantes" role="status" aria-live="polite">
      @for (aviso of avisos.lista(); track aviso.id) {
        <div class="aviso-flotante" [class.error]="aviso.tipo === 'error'" [class.exito]="aviso.tipo === 'exito'"
             (click)="avisos.cerrar(aviso.id)">
          {{ aviso.texto }}
        </div>
      }
    </div>
  `
})
export class AvisosComponent {
  readonly avisos = inject(AvisosService);
}
