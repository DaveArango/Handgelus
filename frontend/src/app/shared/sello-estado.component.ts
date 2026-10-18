import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { ESTADOS_SOLICITUD, TONO_ESTADO } from '../core/etiquetas';
import { EstadoSolicitud } from '../core/modelos';

@Component({
  selector: 'hg-sello-estado',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="sello" [class]="'sello ' + tono()">{{ texto() }}</span>`
})
export class SelloEstadoComponent {
  readonly estado = input.required<EstadoSolicitud>();
  readonly texto = computed(() => ESTADOS_SOLICITUD[this.estado()] ?? this.estado());
  readonly tono = computed(() => TONO_ESTADO[this.estado()] ?? 'sello-neutro');
}
