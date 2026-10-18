import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { mensajeError } from '../../core/interceptor';
import { Notificacion, TipoNotificacion } from '../../core/modelos';
import { FechaPipe } from '../../shared/pipes';

/** Alertas internas del equipo y registro de los correos enviados a los clientes (RF-06, RF-11). */
@Component({
  selector: 'hg-correos',
  standalone: true,
  imports: [RouterLink, FechaPipe],
  template: `
    <header class="encabezado-pagina">
      <div>
        <h1>Notificaciones</h1>
        <p>Alertas para el equipo y la copia de cada correo que recibió un cliente.</p>
      </div>
    </header>

    <nav class="pestanas">
      <button type="button" class="pestana" [class.activa]="tipo() === 'ALERTA_INTERNA'" (click)="cambiar('ALERTA_INTERNA')">Alertas del equipo</button>
      <button type="button" class="pestana" [class.activa]="tipo() === 'CORREO_CLIENTE'" (click)="cambiar('CORREO_CLIENTE')">Correos a clientes</button>
    </nav>

    @if (error()) {
      <p class="aviso aviso-error">{{ error() }}</p>
    }
    <ul class="mensajes">
      @for (n of lista(); track n.id) {
        <li [class.nueva]="!n.leida && n.tipo === 'ALERTA_INTERNA'">
          <button type="button" class="cabecera" (click)="abrir(n)" [attr.aria-expanded]="abierta() === n.id">
            <span class="asunto">{{ n.asunto }}</span>
            <span class="mini suave">{{ n.tipo === 'CORREO_CLIENTE' ? n.destinatario + ' · ' : '' }}{{ n.fechaEnvio | fecha: 'hora' }}</span>
          </button>
          @if (abierta() === n.id) {
            <div class="cuerpo">
              <pre>{{ n.cuerpo }}</pre>
              @if (n.solicitudId) {
                <a [routerLink]="['/estudio/solicitudes', n.solicitudId]" class="pequeno">Abrir la solicitud</a>
              }
            </div>
          }
        </li>
      } @empty {
        <li class="vacio">No hay mensajes todavía.</li>
      }
    </ul>
  `,
  styles: [`
    .mensajes { list-style: none; margin: 0; padding: 0; border: 1px solid var(--arena); border-radius: var(--r-lamina); background: var(--pergamino); overflow: hidden; }
    .mensajes > li { border-bottom: 1px solid var(--arena-suave); }
    .mensajes > li:last-child { border-bottom: 0; }
    .mensajes > li.vacio { border: 0; }
    .cabecera { width: 100%; display: flex; justify-content: space-between; gap: var(--e-4); padding: var(--e-4) var(--e-5); background: none; border: 0; font: inherit; color: inherit; text-align: left; cursor: pointer; }
    .cabecera:hover { background: var(--papel); }
    .nueva .asunto { font-weight: 700; }
    .nueva .asunto::before { content: ""; display: inline-block; width: 7px; height: 7px; margin-right: 8px; border-radius: 50%; background: var(--sepia); vertical-align: middle; }
    .cuerpo { padding: 0 var(--e-5) var(--e-5); }
    pre { white-space: pre-wrap; font-family: var(--f-texto); font-size: var(--t-sm); background: var(--papel); border: 1px solid var(--arena-suave); border-radius: var(--r-control); padding: var(--e-4); margin: 0 0 var(--e-3); }
    @media (max-width: 640px) { .cabecera { flex-direction: column; gap: 2px; } }
  `]
})
export class CorreosComponent implements OnInit {
  private readonly api = inject(ApiService);
  readonly tipo = signal<TipoNotificacion>('ALERTA_INTERNA');
  readonly lista = signal<Notificacion[]>([]);
  readonly abierta = signal<number | null>(null);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.cargar();
  }

  cambiar(tipo: TipoNotificacion): void {
    this.tipo.set(tipo);
    this.abierta.set(null);
    this.cargar();
  }

  private cargar(): void {
    this.api.notificaciones(this.tipo()).subscribe({
      next: l => this.lista.set(l),
      error: e => this.error.set(mensajeError(e))
    });
  }

  abrir(n: Notificacion): void {
    this.abierta.set(this.abierta() === n.id ? null : n.id);
    if (!n.leida && n.tipo === 'ALERTA_INTERNA') {
      this.api.marcarLeida(n.id).subscribe(() => {
        this.lista.update(l => l.map(x => x.id === n.id ? { ...x, leida: true } : x));
      });
    }
  }
}
