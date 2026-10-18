import { Component, OnInit, inject, input, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { ESTADOS_PAGO_SERVICIO, tamanoPara, TIPOS_SERVICIO, TONO_PAGO_SERVICIO } from '../../core/etiquetas';
import { mensajeError } from '../../core/interceptor';
import { SolicitudResumen, TipoServicio } from '../../core/modelos';
import { CopPipe, EtiquetaPipe, FechaPipe, InicialesPipe } from '../../shared/pipes';
import { SelloEstadoComponent } from '../../shared/sello-estado.component';

interface Grupo {
  clave: string;
  texto: string;
}

/** Bandeja de solicitudes (Imagen 8 · CU-01 consultar). */
@Component({
  selector: 'hg-bandeja',
  standalone: true,
  imports: [RouterLink, CopPipe, EtiquetaPipe, FechaPipe, InicialesPipe, SelloEstadoComponent],
  template: `
    <header class="encabezado-pagina">
      <div>
        <h1>Bandeja de solicitudes</h1>
        <p>Consultas de tatuaje y comisiones, desde que llegan hasta que se cierran.</p>
      </div>
      <div class="opciones" role="radiogroup" aria-label="Tipo de servicio">
        <button type="button" class="opcion" [class.activa]="tipo() === null" (click)="filtrarTipo(null)">Todos</button>
        <button type="button" class="opcion" [class.activa]="tipo() === 'TATUAJE'" (click)="filtrarTipo('TATUAJE')">Tatuajes</button>
        <button type="button" class="opcion" [class.activa]="tipo() === 'COMISION'" (click)="filtrarTipo('COMISION')">Comisiones</button>
      </div>
    </header>

    <nav class="pestanas" aria-label="Etapas">
      @for (g of grupos; track g.clave) {
        <button type="button" class="pestana" [class.activa]="grupoActivo() === g.clave" (click)="cambiarGrupo(g.clave)">
          {{ g.texto }}
          @if (grupoActivo() === g.clave && !cargando()) { <span class="cuenta">{{ lista().length }}</span> }
        </button>
      }
    </nav>

    @if (cargando()) {
      <p class="cargando">Abriendo la bandeja…</p>
    } @else if (error()) {
      <p class="aviso aviso-error">{{ error() }}</p>
    } @else if (lista().length === 0) {
      <div class="vacio">
        <h3>Nada por aquí</h3>
        <p>No hay solicitudes en esta etapa. Las nuevas llegan aquí en cuanto un cliente las envía.</p>
      </div>
    } @else {
      <ul class="solicitudes">
        @for (s of lista(); track s.solicitud.id) {
          <li>
            <a [routerLink]="['/estudio/solicitudes', s.solicitud.id]" class="fila-solicitud">
              <span class="monograma" [class.comision]="s.solicitud.tipoServicio === 'COMISION'">{{ s.clienteNombre | iniciales }}</span>
              <div class="principal">
                <div class="fila">
                  <strong class="titulo">{{ s.solicitud.titulo }}</strong>
                  <span class="mini suave">{{ s.clienteNombre }}</span>
                </div>
                <p class="resumen pequeno suave">{{ s.solicitud.descripcion }}</p>
                <span class="mini suave">{{ s.solicitud.tipoServicio | etiqueta: tipos }} · {{ tamano(s) }} · recibida el {{ s.solicitud.fechaCreacion | fecha }}</span>
              </div>
              <div class="lateral">
                <hg-sello-estado [estado]="s.solicitud.estado" />
                <span class="cifra valor">{{ s.valor | cop }}</span>
                <span class="sello" [class]="'sello ' + tonoPago[s.estadoPago]">{{ s.estadoPago | etiqueta: estadosPago }}</span>
              </div>
            </a>
          </li>
        }
      </ul>
    }
  `,
  styles: [`
    .solicitudes { list-style: none; margin: 0; padding: 0; display: grid; gap: var(--e-2); }
    .fila-solicitud {
      display: grid; grid-template-columns: 44px minmax(0, 1fr) auto; gap: var(--e-4); align-items: center;
      padding: var(--e-4); background: var(--pergamino); border: 1px solid var(--arena); border-radius: var(--r-lamina);
      text-decoration: none; color: inherit;
    }
    .fila-solicitud:hover { border-color: var(--grafito); background: var(--papel); }
    .monograma { width: 44px; height: 44px; border-radius: 50%; display: grid; place-items: center; background: var(--tinta); color: var(--lino); font-family: var(--f-display); font-size: 1.05rem; }
    .monograma.comision { background: var(--laton); color: var(--papel); }
    .titulo { font-family: var(--f-display); font-size: var(--t-lg); font-weight: 500; }
    .resumen { margin: 2px 0 4px; max-width: 70ch; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .lateral { display: flex; flex-direction: column; align-items: flex-end; gap: 6px; }
    .valor { font-family: var(--f-display); font-size: var(--t-lg); }
    @media (max-width: 720px) { .fila-solicitud { grid-template-columns: 44px minmax(0, 1fr); } .lateral { grid-column: 2; align-items: flex-start; } }
  `]
})
export class BandejaComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);

  readonly grupo = input<string>();
  readonly grupos: Grupo[] = [
    { clave: 'NUEVAS', texto: 'Nuevas' },
    { clave: 'PENDIENTES', texto: 'Pendientes del cliente' },
    { clave: 'ACTIVAS', texto: 'En curso' },
    { clave: 'FINALIZADAS', texto: 'Finalizadas' },
    { clave: 'TODAS', texto: 'Todas' }
  ];
  readonly grupoActivo = signal('NUEVAS');
  readonly tipo = signal<TipoServicio | null>(null);
  readonly lista = signal<SolicitudResumen[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly tipos = TIPOS_SERVICIO;
  readonly estadosPago = ESTADOS_PAGO_SERVICIO;
  readonly tonoPago = TONO_PAGO_SERVICIO;

  ngOnInit(): void {
    const inicial = this.grupo();
    if (inicial && this.grupos.some(g => g.clave === inicial)) {
      this.grupoActivo.set(inicial);
    }
    this.cargar();
  }

  cambiarGrupo(clave: string): void {
    this.grupoActivo.set(clave);
    this.router.navigate([], { relativeTo: this.ruta, queryParams: { grupo: clave }, replaceUrl: true });
    this.cargar();
  }

  filtrarTipo(tipo: TipoServicio | null): void {
    this.tipo.set(tipo);
    this.cargar();
  }

  tamano(s: SolicitudResumen): string {
    return tamanoPara(s.solicitud.tipoServicio, s.solicitud.categoria);
  }

  private cargar(): void {
    this.cargando.set(true);
    const grupo = this.grupoActivo() === 'TODAS' ? null : this.grupoActivo();
    this.api.solicitudes({ grupo, tipo: this.tipo() }).subscribe({
      next: l => { this.lista.set(l); this.cargando.set(false); this.error.set(null); },
      error: e => { this.error.set(mensajeError(e)); this.cargando.set(false); }
    });
  }
}
