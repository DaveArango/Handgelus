import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { ESTADOS_SOLICITUD, JORNADAS_CORTAS, TIPOS_SERVICIO } from '../../core/etiquetas';
import { mensajeError } from '../../core/interceptor';
import { EstadoSolicitud, TableroVista } from '../../core/modelos';
import { CopPipe, EtiquetaPipe, FechaPipe } from '../../shared/pipes';
import { SelloEstadoComponent } from '../../shared/sello-estado.component';

/** Tablero del atelier (Imagen 6): la operación del día de un vistazo. */
@Component({
  selector: 'hg-tablero',
  standalone: true,
  imports: [RouterLink, CopPipe, EtiquetaPipe, FechaPipe, SelloEstadoComponent],
  template: `
    <header class="encabezado-pagina">
      <div>
        <h1>{{ saludo() }}, {{ primerNombre() }}</h1>
        <p>{{ hoyTexto }} · así va el atelier hoy.</p>
      </div>
      @if (datos(); as d) {
        @if (d.alertasSinLeer > 0) {
          <a routerLink="/estudio/correos" class="btn btn-secundario btn-sm">{{ d.alertasSinLeer }} alertas sin leer</a>
        }
      }
    </header>

    @if (datos(); as d) {
      <div class="rejilla rejilla-4 indicadores">
        <div class="indicador">
          <div class="valor cifra">{{ d.ingresosMes | cop }}</div>
          <div class="nombre">Ingresos confirmados este mes</div>
        </div>
        <div class="indicador">
          <div class="valor cifra">{{ d.saldosPendientes | cop }}</div>
          <div class="nombre">Saldos pendientes en {{ d.serviciosConSaldo }} servicios</div>
        </div>
        <a routerLink="/estudio/solicitudes" [queryParams]="{ grupo: 'NUEVAS' }" class="indicador enlace-indicador">
          <div class="valor">{{ d.solicitudesNuevas }}</div>
          <div class="nombre">Solicitudes por cotizar</div>
        </a>
        <a routerLink="/estudio/proyectos" class="indicador enlace-indicador">
          <div class="valor">{{ d.obrasEnProduccion }}</div>
          <div class="nombre">Obras en producción</div>
        </a>
      </div>

      <div class="rejilla disposicion">
        <section class="lamina">
          <div class="fila-entre">
            <h3>Próximas citas</h3>
            <a routerLink="/estudio/agenda" class="pequeno">Abrir agenda</a>
          </div>
          @if (d.proximasCitas.length === 0) {
            <p class="suave pequeno">No hay citas programadas en las próximas tres semanas.</p>
          } @else {
            <ul class="citas">
              @for (c of d.proximasCitas; track c.cita.id) {
                <li>
                  <div class="fecha-cita">
                    <span class="dia">{{ dia(c.cita.fecha) }}</span>
                    <span class="mes">{{ mes(c.cita.fecha) }}</span>
                  </div>
                  <div class="cita-datos">
                    <a [routerLink]="['/estudio/solicitudes', c.cita.solicitudId]"><strong>{{ c.clienteNombre }}</strong></a>
                    <span class="pequeno suave">{{ c.tituloSolicitud }} · sesión {{ c.cita.numeroSesion }}</span>
                  </div>
                  <span class="mini suave">{{ c.cita.jornada | etiqueta: jornadas }}</span>
                </li>
              }
            </ul>
          }
        </section>

        <section class="lamina">
          <h3>Solicitudes por etapa</h3>
          <ul class="barras">
            @for (b of barras(); track b.estado) {
              <li>
                <span class="pequeno">{{ b.estado | etiqueta: estados }}</span>
                <span class="barra"><span [style.width.%]="b.porcentaje"></span></span>
                <span class="pequeno cifra">{{ b.cantidad }}</span>
              </li>
            }
          </ul>
          <p class="mini suave">{{ d.cotizacionesPorConfirmar }} cotizaciones esperan respuesta del cliente · {{ d.pagosPendientes }} pagos en proceso.</p>
        </section>
      </div>

      <section>
        <div class="fila-entre">
          <h3>Últimas solicitudes</h3>
          <a routerLink="/estudio/solicitudes" class="pequeno">Ver la bandeja completa</a>
        </div>
        <div class="tabla-marco">
          <table class="tabla">
            <thead><tr><th>Proyecto</th><th>Cliente</th><th>Estado</th><th class="derecha">Valor</th><th>Recibida</th></tr></thead>
            <tbody>
              @for (s of d.ultimasSolicitudes; track s.solicitud.id) {
                <tr>
                  <td><a [routerLink]="['/estudio/solicitudes', s.solicitud.id]"><strong>{{ s.solicitud.titulo }}</strong></a>
                    <div class="mini suave">{{ s.solicitud.tipoServicio | etiqueta: tipos }}</div></td>
                  <td>{{ s.clienteNombre }}</td>
                  <td><hg-sello-estado [estado]="s.solicitud.estado" /></td>
                  <td class="derecha cifra">{{ s.valor | cop }}</td>
                  <td class="suave">{{ s.solicitud.fechaCreacion | fecha }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </section>
    } @else if (error()) {
      <p class="aviso aviso-error">{{ error() }}</p>
    } @else {
      <p class="cargando">Preparando el tablero…</p>
    }
  `,
  styles: [`
    .indicadores { margin-bottom: var(--e-5); }
    .enlace-indicador { text-decoration: none; color: inherit; }
    .enlace-indicador:hover { background: var(--papel); }
    .disposicion { grid-template-columns: minmax(0, 1.1fr) minmax(0, .9fr); margin-bottom: var(--e-6); align-items: start; }
    .citas { list-style: none; margin: 0; padding: 0; }
    .citas li { display: flex; align-items: center; gap: var(--e-4); padding: var(--e-3) 0; border-bottom: 1px solid var(--arena-suave); }
    .citas li:last-child { border-bottom: 0; }
    .fecha-cita { width: 48px; text-align: center; line-height: 1; }
    .dia { display: block; font-family: var(--f-display); font-size: 1.7rem; }
    .mes { font-size: var(--t-xs); color: var(--grafito); }
    .cita-datos { flex: 1; display: flex; flex-direction: column; min-width: 0; }
    .cita-datos a { color: var(--tinta); text-decoration: none; }
    .barras { list-style: none; margin: 0 0 var(--e-4); padding: 0; display: grid; gap: var(--e-2); }
    .barras li { display: grid; grid-template-columns: 150px 1fr 28px; align-items: center; gap: var(--e-3); }
    .barra { height: 6px; border-radius: 3px; background: var(--arena-suave); overflow: hidden; }
    .barra span { display: block; height: 100%; background: var(--sepia); border-radius: 3px; }
    @media (max-width: 960px) { .disposicion { grid-template-columns: minmax(0, 1fr); } }
  `]
})
export class TableroComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);

  readonly datos = signal<TableroVista | null>(null);
  readonly error = signal<string | null>(null);
  readonly estados = ESTADOS_SOLICITUD;
  readonly tipos = TIPOS_SERVICIO;
  readonly jornadas = JORNADAS_CORTAS;
  readonly hoyTexto = new Intl.DateTimeFormat('es-CO', { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date());

  readonly barras = computed(() => {
    const d = this.datos();
    if (!d) {
      return [];
    }
    const entradas = Object.entries(d.solicitudesPorEstado).filter(([, n]) => n > 0) as [EstadoSolicitud, number][];
    const max = Math.max(1, ...entradas.map(([, n]) => n));
    return entradas.map(([estado, cantidad]) => ({ estado, cantidad, porcentaje: (cantidad / max) * 100 }));
  });

  ngOnInit(): void {
    this.api.tablero().subscribe({ next: d => this.datos.set(d), error: e => this.error.set(mensajeError(e)) });
  }

  saludo(): string {
    const h = new Date().getHours();
    return h < 12 ? 'Buenos días' : h < 19 ? 'Buenas tardes' : 'Buenas noches';
  }

  primerNombre(): string {
    return (this.auth.usuario()?.nombre ?? '').split(' ')[0];
  }

  dia(fecha: string): string {
    return String(Number(fecha.substring(8, 10)));
  }

  mes(fecha: string): string {
    const meses = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
    return meses[Number(fecha.substring(5, 7)) - 1];
  }
}
