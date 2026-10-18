import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { tamanoPara, TIPOS_SERVICIO } from '../../core/etiquetas';
import { mensajeError } from '../../core/interceptor';
import { SolicitudResumen } from '../../core/modelos';
import { CopPipe, EtiquetaPipe, FechaPipe } from '../../shared/pipes';
import { SelloEstadoComponent } from '../../shared/sello-estado.component';

@Component({
  selector: 'hg-mis-solicitudes',
  standalone: true,
  imports: [RouterLink, CopPipe, EtiquetaPipe, FechaPipe, SelloEstadoComponent],
  template: `
    <section class="contenedor pagina">
      <header class="encabezado-pagina">
        <div>
          <h1>Hola, {{ primerNombre() }}</h1>
          <p>Aquí están tus proyectos con el atelier y el punto en el que va cada uno.</p>
        </div>
        <a routerLink="/mi-espacio/nueva" class="btn btn-primario">Nueva solicitud</a>
      </header>

      @if (cargando()) {
        <p class="cargando">Reuniendo tus bocetos…</p>
      } @else if (error()) {
        <p class="aviso aviso-error">{{ error() }}</p>
      } @else if (lista().length === 0) {
        <div class="vacio">
          <h3>Tu primera pieza empieza con una idea</h3>
          <p>Cuéntanos qué imaginas y te enviaremos una cotización. Puedes adjuntar referencias.</p>
          <a routerLink="/mi-espacio/nueva" class="btn btn-primario">Enviar mi primera solicitud</a>
        </div>
      } @else {
        <ul class="proyectos">
          @for (item of lista(); track item.solicitud.id) {
            <li>
              <a [routerLink]="['/mi-espacio/solicitud', item.solicitud.id]" class="proyecto">
                <div class="proyecto-principal">
                  <h3>{{ item.solicitud.titulo }}</h3>
                  <p class="suave pequeno">
                    {{ item.solicitud.tipoServicio | etiqueta: tipos }} · {{ tamano(item) }} · enviada el {{ item.solicitud.fechaCreacion | fecha }}
                  </p>
                </div>
                <div class="proyecto-estado">
                  <hg-sello-estado [estado]="item.solicitud.estado" />
                  <span class="valor cifra">
                    @if (item.cotizacionConfirmada) { {{ item.valor | cop }} } @else { <span class="suave pequeno">Cotización en revisión</span> }
                  </span>
                </div>
              </a>
            </li>
          }
        </ul>
      }
    </section>
  `,
  styles: [`
    .pagina { padding-top: var(--e-7); }
    .proyectos { list-style: none; margin: 0; padding: 0; border-top: 1px solid var(--arena); }
    .proyecto {
      display: flex; justify-content: space-between; align-items: center; gap: var(--e-5);
      padding: var(--e-5) var(--e-2); border-bottom: 1px solid var(--arena);
      text-decoration: none; color: inherit; transition: background-color .15s ease;
    }
    .proyecto:hover { background: var(--pergamino); }
    .proyecto h3 { margin: 0 0 var(--e-1); }
    .proyecto p { margin: 0; }
    .proyecto-estado { display: flex; flex-direction: column; align-items: flex-end; gap: var(--e-2); }
    .valor { font-family: var(--f-display); font-size: var(--t-lg); }
    @media (max-width: 640px) { .proyecto { flex-direction: column; align-items: flex-start; } .proyecto-estado { align-items: flex-start; } }
  `]
})
export class MisSolicitudesComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);

  readonly lista = signal<SolicitudResumen[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly tipos = TIPOS_SERVICIO;

  primerNombre(): string {
    return (this.auth.usuario()?.nombre ?? '').split(' ')[0];
  }

  tamano(item: SolicitudResumen): string {
    return tamanoPara(item.solicitud.tipoServicio, item.solicitud.categoria);
  }

  ngOnInit(): void {
    this.api.solicitudes().subscribe({
      next: l => { this.lista.set(l); this.cargando.set(false); },
      error: e => { this.error.set(mensajeError(e)); this.cargando.set(false); }
    });
  }
}
