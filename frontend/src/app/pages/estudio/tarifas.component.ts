import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { AvisosService } from '../../core/avisos.service';
import { FORMATOS_COMISION, TAMANOS_TATUAJE } from '../../core/etiquetas';
import { mensajeError } from '../../core/interceptor';
import { CategoriaTamano, Tarifa, TipoServicio } from '../../core/modelos';
import { CopPipe, FechaPipe } from '../../shared/pipes';

interface FilaTarifa {
  tipoServicio: TipoServicio;
  categoria: CategoriaTamano;
  valorBase: number;
  valorHora: number;
  horasEstimadas: number;
  tarifaFija: boolean;
  descripcion: string;
  fechaActualizacion: string | null;
  guardando: boolean;
}

const CATEGORIAS: CategoriaTamano[] = ['PEQUENO', 'MEDIANO', 'GRANDE', 'EXTRA_GRANDE'];

/** F-02 · Configurar tabla de tarifas (SWR-04 / SWR-05). */
@Component({
  selector: 'hg-tarifas',
  standalone: true,
  imports: [FormsModule, CopPipe, FechaPipe],
  template: `
    <header class="encabezado-pagina">
      <div>
        <h1>Tabla de tarifas</h1>
        <p>Valores base que usa la calculadora de cotizaciones. Los cambios aplican solo a solicitudes nuevas:
          las cotizaciones ya emitidas conservan su valor.</p>
      </div>
    </header>

    @for (grupo of grupos; track grupo.tipo) {
      <section class="bloque">
        <h2>{{ grupo.titulo }}</h2>
        <p class="suave pequeno">{{ grupo.ayuda }}</p>
        <div class="tabla-marco">
          <table class="tabla">
            <thead>
              <tr>
                <th>{{ grupo.tipo === 'TATUAJE' ? 'Tamaño' : 'Formato' }}</th>
                @if (grupo.tipo === 'TATUAJE') { <th>Tarifa fija</th> }
                <th>Valor base (COP)</th><th>Valor hora (COP)</th><th>Horas</th>
                <th class="derecha">Referencia</th><th></th>
              </tr>
            </thead>
            <tbody>
              @for (f of filasDe(grupo.tipo); track f.categoria) {
                <tr>
                  <td>
                    <strong>{{ nombre(f) }}</strong>
                    <div class="mini suave">Actualizada {{ f.fechaActualizacion | fecha }}</div>
                  </td>
                  @if (grupo.tipo === 'TATUAJE') {
                    <td><input type="checkbox" [(ngModel)]="f.tarifaFija" [attr.aria-label]="'Tarifa fija ' + nombre(f)"></td>
                  }
                  <td><input class="control cifra corto" type="number" min="1" step="1000" [(ngModel)]="f.valorBase"></td>
                  <td><input class="control cifra corto" type="number" min="0" step="1000" [(ngModel)]="f.valorHora" [disabled]="f.tarifaFija"></td>
                  <td><input class="control cifra mini-campo" type="number" min="0" max="80" [(ngModel)]="f.horasEstimadas" [disabled]="f.tarifaFija"></td>
                  <td class="derecha cifra">{{ referencia(f) | cop }}</td>
                  <td class="derecha">
                    <button type="button" class="btn btn-secundario btn-sm" [disabled]="f.guardando" (click)="guardar(f)">
                      {{ f.guardando ? 'Guardando…' : 'Guardar' }}
                    </button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </section>
    }
  `,
  styles: [`
    .bloque { margin-bottom: var(--e-7); }
    .bloque h2 { margin-bottom: var(--e-1); }
    .corto { max-width: 140px; }
    .mini-campo { max-width: 70px; }
    input[type=checkbox] { accent-color: var(--sepia); width: 18px; height: 18px; }
  `]
})
export class TarifasComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly avisos = inject(AvisosService);
  readonly filas = signal<FilaTarifa[]>([]);
  readonly grupos = [
    { tipo: 'TATUAJE' as TipoServicio, titulo: 'Tatuaje de autor', ayuda: 'Fórmula: (valor base + valor hora × horas) × complejidad del estilo. Con tarifa fija se cobra solo el valor base (RN-03).' },
    { tipo: 'COMISION' as TipoServicio, titulo: 'Obra por comisión', ayuda: 'Fórmula: (valor base + valor hora × horas) × complejidad del estilo + envío según la ciudad de destino (RN-09).' }
  ];

  ngOnInit(): void {
    this.api.tarifas().subscribe({
      next: t => this.filas.set(this.construir(t)),
      error: e => this.avisos.error(mensajeError(e))
    });
  }

  private construir(tarifas: Tarifa[]): FilaTarifa[] {
    const filas: FilaTarifa[] = [];
    (['TATUAJE', 'COMISION'] as TipoServicio[]).forEach(tipo => CATEGORIAS.forEach(categoria => {
      const t = tarifas.find(x => x.tipoServicio === tipo && x.categoria === categoria);
      filas.push({
        tipoServicio: tipo, categoria,
        valorBase: t?.valorBase ?? 0, valorHora: t?.valorHora ?? 0, horasEstimadas: t?.horasEstimadas ?? 0,
        tarifaFija: t?.tarifaFija ?? false, descripcion: t?.descripcion ?? '',
        fechaActualizacion: t?.fechaActualizacion ?? null, guardando: false
      });
    }));
    return filas;
  }

  filasDe(tipo: TipoServicio): FilaTarifa[] {
    return this.filas().filter(f => f.tipoServicio === tipo);
  }

  nombre(f: FilaTarifa): string {
    return (f.tipoServicio === 'TATUAJE' ? TAMANOS_TATUAJE : FORMATOS_COMISION)[f.categoria];
  }

  referencia(f: FilaTarifa): number {
    return f.tarifaFija ? Number(f.valorBase) : Number(f.valorBase) + Number(f.valorHora) * Number(f.horasEstimadas);
  }

  guardar(f: FilaTarifa): void {
    f.guardando = true;
    this.api.guardarTarifa({
      tipoServicio: f.tipoServicio, categoria: f.categoria, valorBase: Number(f.valorBase),
      valorHora: f.tarifaFija ? 0 : Number(f.valorHora), horasEstimadas: Number(f.horasEstimadas),
      tarifaFija: f.tarifaFija, descripcion: f.descripcion || null
    }).subscribe({
      next: t => {
        f.guardando = false;
        f.fechaActualizacion = t.fechaActualizacion;
        this.avisos.exito(`Tarifa "${this.nombre(f)}" actualizada.`);
      },
      error: e => { f.guardando = false; this.avisos.error(mensajeError(e)); }
    });
  }
}
