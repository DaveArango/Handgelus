import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { ESTADOS_OBRA, TIPOS_SERVICIO } from '../../core/etiquetas';
import { mensajeError } from '../../core/interceptor';
import { ObraVista } from '../../core/modelos';
import { EtiquetaPipe, FechaPipe } from '../../shared/pipes';
import { SelloEstadoComponent } from '../../shared/sello-estado.component';

/** F-07 · Proyectos en curso y su avance (Imágenes 10 y 11). */
@Component({
  selector: 'hg-proyectos',
  standalone: true,
  imports: [RouterLink, EtiquetaPipe, FechaPipe, SelloEstadoComponent],
  template: `
    <header class="encabezado-pagina">
      <div>
        <h1>Proyectos en curso</h1>
        <p>Comisiones en producción y tatuajes con sesiones pendientes, con su último avance.</p>
      </div>
    </header>

    @if (cargando()) {
      <p class="cargando">Reuniendo los proyectos…</p>
    } @else if (error()) {
      <p class="aviso aviso-error">{{ error() }}</p>
    } @else if (lista().length === 0) {
      <div class="vacio"><h3>Sin proyectos activos</h3><p>Aparecen aquí cuando el cliente confirma su pago inicial.</p></div>
    } @else {
      @if (comisiones().length > 0) {
        <h3 class="grupo">Obras por comisión</h3>
        <div class="rejilla rejilla-2">
          @for (p of comisiones(); track p.solicitud.id) {
            <a class="lamina proyecto" [routerLink]="['/estudio/solicitudes', p.solicitud.id]">
              <div class="fila-entre">
                <span class="cursiva suave">{{ p.clienteNombre }}</span>
                @if (p.obra) { <span class="sello sello-info">{{ p.obra.estado | etiqueta: estadosObra }}</span> }
              </div>
              <h3>{{ p.solicitud.titulo }}</h3>
              <div class="fila-entre medida">
                <div class="progreso"><span [style.width.%]="p.obra?.progreso ?? 0"></span></div>
                <span class="cifra pequeno">{{ p.obra?.progreso ?? 0 }} %</span>
              </div>
              <p class="pequeno suave ultimo">
                @if (p.ultimoAvance) { Último avance: {{ p.ultimoAvance.titulo }} · {{ p.ultimoAvance.fecha | fecha }} }
                @else { Sin avances registrados todavía. }
              </p>
            </a>
          }
        </div>
      }
      @if (tatuajes().length > 0) {
        <h3 class="grupo">Tatuajes</h3>
        <div class="rejilla rejilla-3">
          @for (p of tatuajes(); track p.solicitud.id) {
            <a class="lamina proyecto" [routerLink]="['/estudio/solicitudes', p.solicitud.id]">
              <span class="cursiva suave">{{ p.clienteNombre }}</span>
              <h3>{{ p.solicitud.titulo }}</h3>
              <hg-sello-estado [estado]="p.solicitud.estado" />
              <p class="pequeno suave ultimo">{{ p.totalAvances }} registros en la bitácora</p>
            </a>
          }
        </div>
      }
    }
  `,
  styles: [`
    .grupo { margin: var(--e-5) 0 var(--e-4); }
    .proyecto { display: flex; flex-direction: column; gap: var(--e-2); text-decoration: none; color: inherit; }
    .proyecto:hover { border-color: var(--grafito); background: var(--papel); }
    .proyecto h3 { margin: 0; }
    .medida { flex-wrap: nowrap; }
    .medida .progreso { flex: 1; height: 6px; }
    .ultimo { margin: 0; }
  `]
})
export class ProyectosComponent implements OnInit {
  private readonly api = inject(ApiService);
  readonly lista = signal<ObraVista[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly estadosObra = ESTADOS_OBRA;
  readonly tipos = TIPOS_SERVICIO;
  readonly comisiones = computed(() => this.lista().filter(p => p.solicitud.tipoServicio === 'COMISION'));
  readonly tatuajes = computed(() => this.lista().filter(p => p.solicitud.tipoServicio === 'TATUAJE'));

  ngOnInit(): void {
    this.api.proyectos().subscribe({
      next: l => { this.lista.set(l); this.cargando.set(false); },
      error: e => { this.error.set(mensajeError(e)); this.cargando.set(false); }
    });
  }
}
