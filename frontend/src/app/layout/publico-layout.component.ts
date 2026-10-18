import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';

/** Navegación superior del sitio público y del portal del cliente. */
@Component({
  selector: 'hg-publico-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="barra no-imprimir">
      <div class="contenedor barra-interior">
        <a routerLink="/" class="logotipo" aria-label="Handgelus, inicio">
          <span class="firma">Handgelus</span>
        </a>

        <button type="button" class="menu-movil" (click)="abierto.set(!abierto())"
                [attr.aria-expanded]="abierto()" aria-controls="navegacion">
          {{ abierto() ? 'Cerrar' : 'Menú' }}
        </button>

        <nav id="navegacion" class="navegacion" [class.abierta]="abierto()" (click)="abierto.set(false)">
          @if (auth.esCliente()) {
            <a routerLink="/mi-espacio" routerLinkActive="activo" [routerLinkActiveOptions]="{ exact: true }">Mis proyectos</a>
            <a routerLink="/mi-espacio/nueva" routerLinkActive="activo">Nueva solicitud</a>
            <a routerLink="/mi-espacio/perfil" routerLinkActive="activo">Perfil y pagos</a>
          } @else {
            <a routerLink="/" fragment="proceso">El proceso</a>
            <a routerLink="/" fragment="servicios">Servicios</a>
          }
          <span class="espacio"></span>
          @if (auth.autenticado()) {
            @if (auth.esPersonal()) {
              <a routerLink="/estudio" class="btn btn-secundario btn-sm">Ir al atelier</a>
            }
            <span class="saludo">{{ auth.usuario()?.nombre }}</span>
            <button type="button" class="btn btn-fantasma btn-sm" (click)="auth.cerrarSesion()">Salir</button>
          } @else {
            <a routerLink="/ingresar" routerLinkActive="activo">Ingresar</a>
            <a routerLink="/registro" class="btn btn-primario btn-sm">Iniciar solicitud</a>
          }
        </nav>
      </div>
    </header>

    <main>
      <router-outlet />
    </main>

    <footer class="pie no-imprimir">
      <div class="contenedor pie-interior">
        <div>
          <span class="firma">Handgelus</span>
          <p class="suave pequeno">Arte por comisión y tatuaje de autor · Armenia, Quindío</p>
        </div>
        <p class="mini suave">
          Tratamos tus datos conforme a la Ley 1581 de 2012. © {{ anio }} Handgelus Atelier.
        </p>
      </div>
    </footer>
  `,
  styles: [`
    .barra {
      position: sticky; top: 0; z-index: 20;
      background: rgba(239, 231, 218, .92);
      backdrop-filter: blur(8px);
      border-bottom: 1px solid var(--arena);
    }
    .barra-interior { display: flex; align-items: center; gap: var(--e-5); min-height: 68px; }
    .logotipo { text-decoration: none; color: var(--tinta); }
    .firma { font-family: var(--f-display); font-style: italic; font-size: 1.65rem; letter-spacing: -.01em; }
    .navegacion { flex: 1; display: flex; align-items: center; gap: var(--e-5); }
    .navegacion a:not(.btn) { color: var(--grafito); text-decoration: none; white-space: nowrap; font-size: var(--t-sm); font-weight: 600; padding: 6px 0; border-bottom: 1px solid transparent; }
    .navegacion a:not(.btn):hover { color: var(--tinta); }
    .navegacion a.activo { color: var(--tinta); border-bottom-color: var(--sepia); }
    .espacio { flex: 1; }
    .saludo { font-size: var(--t-sm); color: var(--grafito); }
    .menu-movil { display: none; margin-left: auto; background: none; border: 1px solid var(--arena); border-radius: var(--r-control); padding: 6px 12px; font: inherit; font-size: var(--t-sm); color: var(--tinta); }
    main { min-height: calc(100vh - 68px - 120px); }
    .pie { border-top: 1px solid var(--arena); margin-top: var(--e-8); padding: var(--e-6) 0; }
    .pie-interior { display: flex; justify-content: space-between; align-items: flex-end; gap: var(--e-5); flex-wrap: wrap; }
    .pie p { margin: 0; }
    @media (max-width: 820px) {
      .menu-movil { display: inline-flex; }
      .navegacion {
        display: none; position: absolute; top: 68px; left: 0; right: 0;
        flex-direction: column; align-items: stretch; gap: var(--e-3);
        padding: var(--e-4) var(--e-5) var(--e-5); background: var(--lino); border-bottom: 1px solid var(--arena);
      }
      .navegacion.abierta { display: flex; }
      .espacio { display: none; }
    }
  `]
})
export class PublicoLayoutComponent {
  readonly auth = inject(AuthService);
  readonly abierto = signal(false);
  readonly anio = new Date().getFullYear();
}
