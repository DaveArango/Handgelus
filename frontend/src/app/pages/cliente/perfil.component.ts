import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { AvisosService } from '../../core/avisos.service';
import { ESTADOS_PAGO_SERVICIO, TIPOS_COMPROBANTE, TIPOS_SERVICIO, TONO_PAGO_SERVICIO } from '../../core/etiquetas';
import { mensajeError } from '../../core/interceptor';
import { Comprobante, HistorialCliente } from '../../core/modelos';
import { ComprobanteComponent } from '../../shared/comprobante.component';
import { CopPipe, EtiquetaPipe, FechaPipe, InicialesPipe } from '../../shared/pipes';
import { SelloEstadoComponent } from '../../shared/sello-estado.component';

/** Perfil y pagos del cliente (Imagen 5) · su propio historial de servicios (F-08). */
@Component({
  selector: 'hg-perfil',
  standalone: true,
  imports: [FormsModule, RouterLink, CopPipe, EtiquetaPipe, FechaPipe, InicialesPipe, SelloEstadoComponent, ComprobanteComponent],
  template: `
    <section class="contenedor pagina">
      <header class="encabezado-pagina">
        <div>
          <h1>Perfil y pagos</h1>
          <p>Tus datos de contacto, lo que has pagado y todos tus comprobantes.</p>
        </div>
      </header>

      <div class="rejilla disposicion">
        <aside class="lamina-hoja ficha">
          <span class="monograma">{{ auth.usuario()?.nombre | iniciales }}</span>
          @if (!editando()) {
            <h2>{{ auth.usuario()?.nombre }}</h2>
            <p class="suave pequeno">{{ auth.usuario()?.correo }}<br>{{ auth.usuario()?.telefono }}</p>
            <p class="mini suave">Cliente desde {{ auth.usuario()?.fechaRegistro | fecha }}</p>
            <button type="button" class="btn btn-secundario btn-sm" (click)="editar()">Editar datos</button>
          } @else {
            <form (ngSubmit)="guardar()" class="formulario">
              <div class="campo">
                <label for="nombre">Nombre</label>
                <input id="nombre" class="control" name="nombre" [(ngModel)]="nombre" required>
              </div>
              <div class="campo">
                <label for="telefono">Teléfono</label>
                <input id="telefono" class="control" name="telefono" [(ngModel)]="telefono" required>
              </div>
              <div class="fila">
                <button type="submit" class="btn btn-primario btn-sm">Guardar</button>
                <button type="button" class="btn btn-fantasma btn-sm" (click)="editando.set(false)">Cancelar</button>
              </div>
            </form>
          }
        </aside>

        <div class="pila">
          @if (historial(); as h) {
            <div class="rejilla rejilla-3">
              <div class="indicador"><div class="valor cifra">{{ h.totalPagado | cop }}</div><div class="nombre">Pagado en total</div></div>
              <div class="indicador"><div class="valor cifra">{{ saldoPendiente() | cop }}</div><div class="nombre">Saldo pendiente</div></div>
              <div class="indicador"><div class="valor">{{ h.serviciosCompletados }} / {{ h.totalServicios }}</div><div class="nombre">Servicios completados</div></div>
            </div>

            <section>
              <h3>Historial de servicios</h3>
              @if (h.servicios.length === 0) {
                <div class="vacio"><p>Aún no tienes servicios.</p><a routerLink="/mi-espacio/nueva" class="btn btn-primario">Enviar una solicitud</a></div>
              } @else {
                <div class="tabla-marco">
                  <table class="tabla">
                    <thead><tr><th>Proyecto</th><th>Estado</th><th>Pago</th><th class="derecha">Valor</th><th>Comprobantes</th></tr></thead>
                    <tbody>
                      @for (s of h.servicios; track s.solicitudId) {
                        <tr>
                          <td>
                            <a [routerLink]="['/mi-espacio/solicitud', s.solicitudId]"><strong>{{ s.titulo }}</strong></a>
                            <div class="mini suave">{{ s.tipoServicio | etiqueta: tipos }} · {{ s.fecha | fecha }}</div>
                          </td>
                          <td><hg-sello-estado [estado]="s.estado" /></td>
                          <td><span class="sello" [class]="'sello ' + tonoPago[s.estadoPago]">{{ s.estadoPago | etiqueta: estadosPago }}</span>
                            <div class="mini suave cifra">{{ s.pagado | cop }}</div></td>
                          <td class="derecha cifra">{{ s.valor | cop }}</td>
                          <td>
                            @for (c of s.comprobantes; track c.id) {
                              <button type="button" class="enlace mini" (click)="comprobante.set(c)">{{ c.codigo }}</button>
                            } @empty { <span class="mini suave">—</span> }
                          </td>
                        </tr>
                      }
                    </tbody>
                  </table>
                </div>
              }
            </section>
          } @else if (error()) {
            <p class="aviso aviso-error">{{ error() }}</p>
          } @else {
            <p class="cargando">Cargando tu historial…</p>
          }
        </div>
      </div>
    </section>

    @if (comprobante(); as c) {
      <hg-comprobante [comprobante]="c" (cerrar)="comprobante.set(null)" />
    }
  `,
  styles: [`
    .pagina { padding-top: var(--e-7); }
    .disposicion { grid-template-columns: 300px minmax(0, 1fr); align-items: start; }
    .ficha { text-align: center; }
    .monograma { width: 72px; height: 72px; margin: 0 auto var(--e-4); border-radius: 50%; display: grid; place-items: center; background: var(--tinta); color: var(--lino); font-family: var(--f-display); font-size: 1.8rem; }
    .formulario { text-align: left; }
    .enlace { display: block; background: none; border: 0; padding: 0; font: inherit; color: var(--sepia); cursor: pointer; }
    .enlace:hover { text-decoration: underline; }
    @media (max-width: 860px) { .disposicion { grid-template-columns: minmax(0, 1fr); } }
  `]
})
export class PerfilComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);
  private readonly avisos = inject(AvisosService);

  readonly historial = signal<HistorialCliente | null>(null);
  readonly error = signal<string | null>(null);
  readonly editando = signal(false);
  readonly comprobante = signal<Comprobante | null>(null);
  nombre = '';
  telefono = '';

  readonly tipos = TIPOS_SERVICIO;
  readonly estadosPago = ESTADOS_PAGO_SERVICIO;
  readonly tonoPago = TONO_PAGO_SERVICIO;
  readonly tiposComprobante = TIPOS_COMPROBANTE;

  readonly saldoPendiente = computed(() => {
    const h = this.historial();
    if (!h) {
      return 0;
    }
    return h.servicios
      .filter(s => s.estado !== 'CANCELADA' && s.estado !== 'RECIBIDA' && s.estado !== 'COTIZACION_GENERADA')
      .reduce((total, s) => total + Math.max(0, s.valor - s.pagado), 0);
  });

  ngOnInit(): void {
    this.api.miHistorial().subscribe({
      next: h => this.historial.set(h),
      error: e => this.error.set(mensajeError(e))
    });
  }

  editar(): void {
    this.nombre = this.auth.usuario()?.nombre ?? '';
    this.telefono = this.auth.usuario()?.telefono ?? '';
    this.editando.set(true);
  }

  guardar(): void {
    this.auth.actualizarPerfil(this.nombre, this.telefono).subscribe({
      next: () => { this.editando.set(false); this.avisos.exito('Datos actualizados.'); },
      error: e => this.avisos.error(mensajeError(e))
    });
  }
}
