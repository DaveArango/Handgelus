import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { Rol } from '../core/modelos';
import { IconoComponent } from '../shared/icono.component';
import { InicialesPipe } from '../shared/pipes';

interface Enlace {
  ruta: string;
  texto: string;
  icono: string;
  roles?: Rol[];
  exacto?: boolean;
}

const ENLACES: Enlace[] = [
  { ruta: '/estudio', texto: 'Tablero', icono: 'tablero', exacto: true },
  { ruta: '/estudio/solicitudes', texto: 'Solicitudes', icono: 'bandeja' },
  { ruta: '/estudio/agenda', texto: 'Agenda', icono: 'agenda' },
  { ruta: '/estudio/proyectos', texto: 'Proyectos en curso', icono: 'pincel' },
  { ruta: '/estudio/pagos', texto: 'Control de pagos', icono: 'pagos' },
  { ruta: '/estudio/clientes', texto: 'Clientes', icono: 'clientes' },
  { ruta: '/estudio/tarifas', texto: 'Tarifas', icono: 'tarifas', roles: ['TATUADOR', 'ADMINISTRADOR'] },
  { ruta: '/estudio/usuarios', texto: 'Usuarios', icono: 'usuarios', roles: ['ADMINISTRADOR'] },
  { ruta: '/estudio/correos', texto: 'Notificaciones', icono: 'correo' }
];

/** Panel del atelier con barra lateral (asistente, tatuador/artista y administrador). */
@Component({
  selector: 'hg-estudio-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, IconoComponent, InicialesPipe],
  template: `
    <div class="marco">
      <aside class="lateral no-imprimir" [class.abierto]="abierto()">
        <a routerLink="/estudio" class="marca">
          <span class="firma">Handgelus</span>
          <span class="mini suave">Atelier · gestión interna</span>
        </a>
        <nav (click)="abierto.set(false)">
          @for (enlace of enlaces(); track enlace.ruta) {
            <a [routerLink]="enlace.ruta" routerLinkActive="activo"
               [routerLinkActiveOptions]="{ exact: !!enlace.exacto }">
              <hg-icono [nombre]="enlace.icono" />
              <span>{{ enlace.texto }}</span>
            </a>
          }
        </nav>
        <div class="usuario">
          <span class="monograma">{{ auth.usuario()?.nombre | iniciales }}</span>
          <div class="usuario-datos">
            <strong>{{ auth.usuario()?.nombre }}</strong>
            <span class="mini suave">{{ auth.usuario()?.rolEtiqueta }}</span>
          </div>
          <button type="button" class="btn btn-fantasma btn-sm" (click)="auth.cerrarSesion()" title="Cerrar sesión">
            <hg-icono nombre="salir" />
          </button>
        </div>
      </aside>

      <div class="contenido">
        <header class="barra-movil no-imprimir">
          <button type="button" class="btn btn-fantasma btn-sm" (click)="abierto.set(!abierto())">Menú</button>
          <span class="firma">Handgelus</span>
        </header>
        <main class="area">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
  styles: [`
    .marco { display: grid; grid-template-columns: 252px minmax(0, 1fr); min-height: 100vh; }
    .lateral {
      position: sticky; top: 0; height: 100vh;
      display: flex; flex-direction: column;
      padding: var(--e-5) var(--e-4);
      background: var(--pergamino);
      border-right: 1px solid var(--arena);
    }
    .marca { display: flex; flex-direction: column; text-decoration: none; color: var(--tinta); padding: 0 var(--e-3) var(--e-5); }
    .firma { font-family: var(--f-display); font-style: italic; font-size: 1.6rem; line-height: 1.1; }
    nav { display: flex; flex-direction: column; gap: 2px; flex: 1; overflow-y: auto; }
    nav a {
      display: flex; align-items: center; gap: var(--e-3);
      padding: 9px var(--e-3); border-radius: var(--r-control);
      color: var(--grafito); text-decoration: none; font-size: var(--t-sm); font-weight: 600;
      border-left: 2px solid transparent;
    }
    nav a:hover { background: var(--lino); color: var(--tinta); }
    nav a.activo { background: var(--lino); color: var(--sepia); border-left-color: var(--sepia); border-radius: 0 var(--r-control) var(--r-control) 0; }
    .usuario { display: flex; align-items: center; gap: var(--e-3); padding-top: var(--e-4); border-top: 1px solid var(--arena); }
    .monograma {
      width: 36px; height: 36px; border-radius: 50%; display: grid; place-items: center;
      background: var(--tinta); color: var(--lino); font-family: var(--f-display); font-size: 1rem;
    }
    .usuario-datos { flex: 1; min-width: 0; display: flex; flex-direction: column; line-height: 1.3; }
    .usuario-datos strong { font-size: var(--t-sm); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .contenido { min-width: 0; }
    .area { padding: var(--e-6) var(--e-6) var(--e-8); max-width: 1240px; }
    .barra-movil { display: none; }
    @media (max-width: 900px) {
      .marco { grid-template-columns: minmax(0, 1fr); }
      .lateral { position: fixed; left: 0; top: 0; z-index: 40; width: 264px; transform: translateX(-100%); transition: transform .25s ease; box-shadow: 0 0 40px rgba(42,33,28,.25); }
      .lateral.abierto { transform: none; }
      .barra-movil { display: flex; align-items: center; gap: var(--e-3); padding: var(--e-3) var(--e-4); border-bottom: 1px solid var(--arena); background: var(--pergamino); position: sticky; top: 0; z-index: 30; }
      .area { padding: var(--e-5) var(--e-4) var(--e-7); }
    }
  `]
})
export class EstudioLayoutComponent {
  readonly auth = inject(AuthService);
  readonly abierto = signal(false);
  readonly enlaces = computed(() => ENLACES.filter(e => !e.roles || this.auth.tieneRol(...e.roles)));
}
