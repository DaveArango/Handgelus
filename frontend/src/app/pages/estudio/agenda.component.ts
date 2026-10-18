import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { AvisosService } from '../../core/avisos.service';
import { JORNADAS, JORNADAS_CORTAS } from '../../core/etiquetas';
import { fechaLarga, hoy, iso, nombreMes, sumarDias } from '../../core/fechas';
import { mensajeError } from '../../core/interceptor';
import { CitaAgenda, DisponibilidadDia, Jornada, SolicitudResumen, Usuario } from '../../core/modelos';
import { EtiquetaPipe } from '../../shared/pipes';

interface Dia {
  fecha: string;
  numero: number;
  delMes: boolean;
  esHoy: boolean;
  citas: CitaAgenda[];
  disponibilidad: DisponibilidadDia | null;
}

/** F-06 · Agenda del tatuador (Imágenes 7 y 9): mes, disponibilidad por jornada y agendamiento. */
@Component({
  selector: 'hg-agenda',
  standalone: true,
  imports: [FormsModule, RouterLink, EtiquetaPipe],
  template: `
    <header class="encabezado-pagina">
      <div>
        <h1>Agenda</h1>
        <p>Dos jornadas por día, una cita por jornada. Domingos, atelier cerrado.</p>
      </div>
      <div class="fila">
        <select class="control control-caja selector" [(ngModel)]="tatuadorId" (ngModelChange)="cargar()" aria-label="Tatuador">
          <option [ngValue]="null">Todos los tatuadores</option>
          @for (t of tatuadores(); track t.id) { <option [ngValue]="t.id">{{ t.nombre }}</option> }
        </select>
      </div>
    </header>

    <div class="rejilla disposicion">
      <section class="lamina calendario-marco">
        <div class="fila-entre cabeza-mes">
          <button type="button" class="btn btn-fantasma btn-sm" (click)="moverMes(-1)" aria-label="Mes anterior">Anterior</button>
          <h2 class="mes">{{ tituloMes() }}</h2>
          <button type="button" class="btn btn-fantasma btn-sm" (click)="moverMes(1)" aria-label="Mes siguiente">Siguiente</button>
        </div>
        <div class="semana">
          @for (d of ['lun', 'mar', 'mié', 'jue', 'vie', 'sáb', 'dom']; track d) { <span>{{ d }}</span> }
        </div>
        <div class="dias">
          @for (dia of dias(); track dia.fecha) {
            <button type="button" class="dia" [class.fuera]="!dia.delMes" [class.hoy]="dia.esHoy"
                    [class.elegido]="dia.fecha === elegido()" [class.lleno]="dia.disponibilidad && !dia.disponibilidad.disponible"
                    (click)="elegir(dia.fecha)" [attr.aria-label]="etiquetaDia(dia)">
              <span class="numero">{{ dia.numero }}</span>
              @for (c of dia.citas; track c.cita.id) {
                <span class="evento" [class.realizada]="c.cita.estado === 'REALIZADA'">{{ corto(c.cita.jornada) }} · {{ c.clienteNombre }}</span>
              }
            </button>
          }
        </div>
        <p class="mini suave leyenda"><span class="muestra lleno"></span> Día sin jornadas disponibles</p>
      </section>

      <aside class="pila">
        <section class="lamina">
          <h3>{{ elegidoTexto() }}</h3>
          @if (citasElegido().length === 0) {
            <p class="pequeno suave">Sin citas este día.</p>
          }
          @for (c of citasElegido(); track c.cita.id) {
            <div class="cita">
              <span class="mini suave">{{ c.cita.jornada | etiqueta: jornadas }}</span>
              <a [routerLink]="['/estudio/solicitudes', c.cita.solicitudId]"><strong>{{ c.clienteNombre }}</strong></a>
              <span class="pequeno suave">{{ c.tituloSolicitud }} · sesión {{ c.cita.numeroSesion }} · {{ c.tatuadorNombre }}</span>
            </div>
          }

          @if (puedeAgendarElegido()) {
            <form class="agendar" (ngSubmit)="agendar()">
              <h4>Agendar en este día</h4>
              @if (porAgendar().length === 0) {
                <p class="pequeno suave">No hay tatuajes con abono confirmado esperando cita.</p>
              } @else {
                <div class="campo">
                  <label for="solicitud">Solicitud</label>
                  <select id="solicitud" class="control" name="solicitud" [(ngModel)]="solicitudId">
                    @for (s of porAgendar(); track s.solicitud.id) {
                      <option [ngValue]="s.solicitud.id">{{ s.clienteNombre }} · {{ s.solicitud.titulo }}</option>
                    }
                  </select>
                </div>
                <div class="campo">
                  <label for="jornada">Jornada</label>
                  <select id="jornada" class="control" name="jornada" [(ngModel)]="jornada">
                    @for (j of jornadasLibres(); track j) { <option [ngValue]="j">{{ j | etiqueta: jornadas }}</option> }
                  </select>
                </div>
                <button type="submit" class="btn btn-primario btn-bloque" [disabled]="trabajando() || !solicitudId || !jornada || !tatuadorId">Agendar cita</button>
                @if (!tatuadorId) { <p class="mini suave">Selecciona un tatuador para agendar.</p> }
              }
            </form>
          }
        </section>

        <section class="lamina">
          <h3>Esperando cita</h3>
          @for (s of porAgendar(); track s.solicitud.id) {
            <a class="pendiente" [routerLink]="['/estudio/solicitudes', s.solicitud.id]">
              <strong>{{ s.clienteNombre }}</strong>
              <span class="pequeno suave">{{ s.solicitud.titulo }}</span>
            </a>
          } @empty {
            <p class="pequeno suave">Todo agendado.</p>
          }
        </section>
      </aside>
    </div>
  `,
  styles: [`
    .selector { min-width: 220px; }
    .disposicion { grid-template-columns: minmax(0, 1fr) 320px; align-items: start; }
    .cabeza-mes { margin-bottom: var(--e-4); }
    .mes { margin: 0; text-transform: capitalize; }
    .semana, .dias { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); gap: 4px; }
    .semana span { text-align: center; font-size: var(--t-xs); color: var(--grafito); padding-bottom: var(--e-2); }
    .dia {
      min-height: 92px; padding: 6px; border: 1px solid var(--arena-suave); border-radius: var(--r-control);
      background: var(--papel); font: inherit; color: inherit; text-align: left; cursor: pointer;
      display: flex; flex-direction: column; gap: 3px; overflow: hidden;
    }
    .dia:hover { border-color: var(--grafito); }
    .dia.fuera { opacity: .4; }
    .dia.lleno { background: repeating-linear-gradient(135deg, var(--pergamino), var(--pergamino) 6px, var(--arena-suave) 6px, var(--arena-suave) 7px); }
    .dia.elegido { border-color: var(--sepia); box-shadow: inset 0 0 0 1px var(--sepia); }
    .numero { font-size: var(--t-sm); font-weight: 600; }
    .dia.hoy .numero { color: var(--papel); background: var(--sepia); border-radius: 50%; width: 24px; height: 24px; display: grid; place-items: center; }
    .evento { font-size: .68rem; line-height: 1.3; padding: 2px 5px; border-radius: 2px; background: var(--tinta); color: var(--lino); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .evento.realizada { background: var(--arena); color: var(--grafito); }
    .leyenda { display: flex; align-items: center; gap: var(--e-2); margin: var(--e-4) 0 0; }
    .muestra { width: 14px; height: 14px; border-radius: 2px; border: 1px solid var(--arena); }
    .muestra.lleno { background: repeating-linear-gradient(135deg, var(--pergamino), var(--pergamino) 3px, var(--arena) 3px, var(--arena) 4px); }
    .cita { display: flex; flex-direction: column; padding: var(--e-3) 0; border-bottom: 1px solid var(--arena-suave); }
    .cita a { color: var(--tinta); text-decoration: none; }
    .agendar { margin-top: var(--e-4); padding-top: var(--e-4); border-top: 1px solid var(--arena); }
    .agendar h4 { margin-bottom: var(--e-3); }
    .pendiente { display: flex; flex-direction: column; padding: var(--e-2) 0; border-bottom: 1px solid var(--arena-suave); text-decoration: none; color: var(--tinta); }
    @media (max-width: 1080px) { .disposicion { grid-template-columns: minmax(0, 1fr); } }
    @media (max-width: 640px) { .dia { min-height: 54px; } .evento { display: none; } .dia:has(.evento)::after { content: ""; width: 6px; height: 6px; border-radius: 50%; background: var(--sepia); } }
  `]
})
export class AgendaComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly avisos = inject(AvisosService);

  readonly mesBase = signal(new Date(hoy().getFullYear(), hoy().getMonth(), 1));
  readonly citas = signal<CitaAgenda[]>([]);
  readonly disponibilidad = signal<DisponibilidadDia[]>([]);
  readonly tatuadores = signal<Usuario[]>([]);
  readonly porAgendar = signal<SolicitudResumen[]>([]);
  readonly elegido = signal(iso(hoy()));
  readonly trabajando = signal(false);
  readonly jornadas = JORNADAS;

  tatuadorId: number | null = null;
  solicitudId: number | null = null;
  jornada: Jornada | null = null;

  readonly tituloMes = computed(() => `${nombreMes(this.mesBase().getMonth())} ${this.mesBase().getFullYear()}`);

  readonly dias = computed<Dia[]>(() => {
    const base = this.mesBase();
    const inicio = sumarDias(base, -((base.getDay() + 6) % 7));   // lunes de la primera semana
    const hoyIso = iso(hoy());
    const porFecha = new Map<string, CitaAgenda[]>();
    this.citas().forEach(c => porFecha.set(c.cita.fecha, [...(porFecha.get(c.cita.fecha) ?? []), c]));
    const dispPorFecha = new Map(this.disponibilidad().map(d => [d.fecha, d] as const));
    return Array.from({ length: 42 }, (_, i) => {
      const f = sumarDias(inicio, i);
      const clave = iso(f);
      return {
        fecha: clave, numero: f.getDate(), delMes: f.getMonth() === base.getMonth(), esHoy: clave === hoyIso,
        citas: porFecha.get(clave) ?? [], disponibilidad: dispPorFecha.get(clave) ?? null
      };
    });
  });

  readonly citasElegido = computed(() => this.citas().filter(c => c.cita.fecha === this.elegido()));
  readonly elegidoTexto = computed(() => {
    const t = fechaLarga(this.elegido());
    return t.charAt(0).toUpperCase() + t.slice(1);
  });
  readonly jornadasLibres = computed<Jornada[]>(() =>
    this.disponibilidad().find(d => d.fecha === this.elegido())?.jornadasLibres ?? []);
  readonly puedeAgendarElegido = computed(() =>
    this.elegido() >= iso(hoy()) && this.jornadasLibres().length > 0);

  ngOnInit(): void {
    this.api.tatuadores().subscribe(t => {
      this.tatuadores.set(t);
      const propio = t.find(x => x.id === this.auth.usuario()?.id);
      this.tatuadorId = propio?.id ?? (t.length === 1 ? t[0].id : null);
      this.cargar();
    });
    this.cargarPendientes();
  }

  cargar(): void {
    const dias = this.dias();
    const desde = dias[0].fecha;
    const hasta = dias[dias.length - 1].fecha;
    this.api.citas(desde, hasta, this.tatuadorId).subscribe({
      next: c => this.citas.set(c),
      error: e => this.avisos.error(mensajeError(e))
    });
    if (this.tatuadorId) {
      this.api.disponibilidad(desde, hasta, this.tatuadorId).subscribe({
        next: d => { this.disponibilidad.set(d); this.ajustarJornada(); },
        error: () => this.disponibilidad.set([])
      });
    } else {
      this.disponibilidad.set([]);
    }
  }

  private cargarPendientes(): void {
    this.api.solicitudes({ estado: 'PAGO_PARCIAL_CONFIRMADO', tipo: 'TATUAJE' }).subscribe(l => {
      this.porAgendar.set(l);
      this.solicitudId = l[0]?.solicitud.id ?? null;
    });
  }

  private ajustarJornada(): void {
    const libres = this.jornadasLibres();
    if (!this.jornada || !libres.includes(this.jornada)) {
      this.jornada = libres[0] ?? null;
    }
  }

  moverMes(delta: number): void {
    const b = this.mesBase();
    this.mesBase.set(new Date(b.getFullYear(), b.getMonth() + delta, 1));
    this.cargar();
  }

  elegir(fecha: string): void {
    this.elegido.set(fecha);
    this.ajustarJornada();
  }

  corto(j: Jornada): string {
    return JORNADAS_CORTAS[j];
  }

  etiquetaDia(dia: Dia): string {
    return `${fechaLarga(dia.fecha)}, ${dia.citas.length} citas`;
  }

  agendar(): void {
    if (!this.solicitudId || !this.jornada) {
      return;
    }
    this.trabajando.set(true);
    this.api.agendar({ solicitudId: this.solicitudId, tatuadorId: this.tatuadorId, fecha: this.elegido(), jornada: this.jornada, notas: null })
      .subscribe({
        next: () => {
          this.trabajando.set(false);
          this.avisos.exito('Cita agendada. El cliente recibió su comprobante.');
          this.cargar();
          this.cargarPendientes();
        },
        error: e => { this.trabajando.set(false); this.avisos.error(mensajeError(e)); }
      });
  }
}

