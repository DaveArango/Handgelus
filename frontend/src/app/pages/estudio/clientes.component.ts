import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject, Subscription, debounceTime, distinctUntilChanged, switchMap } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { ClienteResumen } from '../../core/modelos';
import { CopPipe, FechaPipe, InicialesPipe } from '../../shared/pipes';

/** F-08 · Clientes del atelier con acceso a su historial. */
@Component({
  selector: 'hg-clientes',
  standalone: true,
  imports: [FormsModule, RouterLink, CopPipe, FechaPipe, InicialesPipe],
  template: `
    <header class="encabezado-pagina">
      <div>
        <h1>Clientes</h1>
        <p>Historial de tatuajes y comisiones de cada persona, para atenderla con contexto.</p>
      </div>
      <input class="control control-caja buscador" type="search" placeholder="Buscar por nombre, correo o teléfono"
             [(ngModel)]="texto" (ngModelChange)="busqueda$.next($event)" aria-label="Buscar clientes">
    </header>

    <div class="tabla-marco">
      <table class="tabla">
        <thead><tr><th>Cliente</th><th>Contacto</th><th class="derecha">Servicios</th><th>Último servicio</th><th class="derecha">Total pagado</th></tr></thead>
        <tbody>
          @for (c of lista(); track c.cliente.id) {
            <tr>
              <td>
                <a [routerLink]="['/estudio/clientes', c.cliente.id]" class="cliente">
                  <span class="monograma">{{ c.cliente.nombre | iniciales }}</span>
                  <strong>{{ c.cliente.nombre }}</strong>
                </a>
              </td>
              <td class="pequeno">{{ c.cliente.correo }}<div class="mini suave">{{ c.cliente.telefono }}</div></td>
              <td class="derecha">{{ c.servicios }} @if (c.serviciosActivos > 0) { <span class="mini suave">({{ c.serviciosActivos }} activos)</span> }</td>
              <td class="suave">{{ c.ultimoServicio | fecha }}</td>
              <td class="derecha cifra">{{ c.totalPagado | cop }}</td>
            </tr>
          } @empty {
            <tr><td colspan="5" class="suave">{{ cargando() ? 'Buscando…' : 'No encontramos clientes con esa búsqueda.' }}</td></tr>
          }
        </tbody>
      </table>
    </div>
  `,
  styles: [`
    .buscador { min-width: 300px; }
    .cliente { display: inline-flex; align-items: center; gap: var(--e-3); color: var(--tinta); text-decoration: none; }
    .monograma { width: 34px; height: 34px; border-radius: 50%; display: grid; place-items: center; background: var(--tinta); color: var(--lino); font-family: var(--f-display); }
    @media (max-width: 640px) { .buscador { min-width: 0; width: 100%; } }
  `]
})
export class ClientesComponent implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  readonly lista = signal<ClienteResumen[]>([]);
  readonly cargando = signal(true);
  readonly busqueda$ = new Subject<string>();
  texto = '';
  private suscripcion?: Subscription;

  ngOnInit(): void {
    this.suscripcion = this.busqueda$.pipe(
      debounceTime(250),
      distinctUntilChanged(),
      switchMap(q => { this.cargando.set(true); return this.api.clientes(q); })
    ).subscribe(l => { this.lista.set(l); this.cargando.set(false); });
    this.api.clientes().subscribe(l => { this.lista.set(l); this.cargando.set(false); });
  }

  ngOnDestroy(): void {
    this.suscripcion?.unsubscribe();
  }
}
