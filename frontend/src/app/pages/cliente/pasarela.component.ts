import { Component, OnInit, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { METODOS_PAGO } from '../../core/etiquetas';
import { mensajeError } from '../../core/interceptor';
import { CheckoutVista, Pago } from '../../core/modelos';
import { CopPipe, EtiquetaPipe } from '../../shared/pipes';

/**
 * Pantalla de la pasarela de pago simulada (sustituye a MercadoPago/PayU en el entorno académico).
 * Al aprobar, la pasarela envía su webhook firmado y el sistema confirma el pago (SWR-13).
 */
@Component({
  selector: 'hg-pasarela',
  standalone: true,
  imports: [RouterLink, CopPipe, EtiquetaPipe],
  template: `
    <section class="contenedor pagina">
      <div class="pasarela">
        <header class="fila-entre">
          <span class="proveedor">Pasarela de pago</span>
          <span class="mini candado">Conexión cifrada · entorno de pruebas</span>
        </header>

        @if (checkout(); as c) {
          @if (resultado(); as r) {
            <div class="resultado" [class.ok]="r.estado === 'CONFIRMADO'">
              <h2>{{ r.estado === 'CONFIRMADO' ? 'Pago aprobado' : 'Pago no aprobado' }}</h2>
              <p>{{ r.estado === 'CONFIRMADO'
                  ? 'Recibimos tu pago. El comprobante ya está en tu correo y en tu perfil.'
                  : 'La transacción no se completó. Puedes intentarlo nuevamente desde tu proyecto.' }}</p>
              <a [routerLink]="volverA()" class="btn btn-primario">Volver al proyecto</a>
            </div>
          } @else {
            <p class="mini gris">Pagar a</p>
            <h2>Handgelus Atelier</h2>
            <dl class="datos">
              <dt>Concepto</dt><dd>{{ c.concepto }}</dd>
              <dt>Pagador</dt><dd>{{ c.clienteNombre }}</dd>
              <dt>Método</dt><dd>{{ c.pago.metodo | etiqueta: metodos }}</dd>
              <dt>Referencia</dt><dd class="cifra">{{ c.pago.referencia }}</dd>
            </dl>
            <div class="total fila-entre">
              <span>Total a pagar</span>
              <strong class="cifra">{{ c.pago.monto | cop }}</strong>
            </div>
            @if (c.pago.estado !== 'PENDIENTE') {
              <p class="aviso">Este pago ya fue procesado.</p>
              <a [routerLink]="volverA()" class="btn btn-secundario btn-bloque">Volver al proyecto</a>
            } @else {
              <button type="button" class="btn btn-pagar btn-bloque" [disabled]="procesando()" (click)="responder(true)">
                {{ procesando() ? 'Procesando…' : 'Aprobar pago' }}
              </button>
              <button type="button" class="btn btn-fantasma btn-bloque" [disabled]="procesando()" (click)="responder(false)">
                Simular rechazo
              </button>
              <p class="mini gris nota">Esta pasarela es simulada para el entorno académico: no se realiza ningún cobro real.</p>
            }
          }
        } @else if (error()) {
          <p class="aviso aviso-error">{{ error() }}</p>
        } @else {
          <p class="gris">Conectando con la pasarela…</p>
        }
      </div>
    </section>
  `,
  styles: [`
    .pagina { padding-top: var(--e-7); display: grid; place-items: center; }
    .pasarela {
      width: min(440px, 100%); background: #fff; color: #1f2933;
      border-radius: 12px; padding: var(--e-6); border: 1px solid #e3e6ea;
      box-shadow: 0 24px 48px -24px rgba(15, 23, 42, .35);
      font-family: system-ui, -apple-system, "Segoe UI", sans-serif;
    }
    header { padding-bottom: var(--e-4); margin-bottom: var(--e-5); border-bottom: 1px solid #edf0f3; }
    .proveedor { font-weight: 700; letter-spacing: -.01em; color: #1d4ed8; }
    .candado, .gris { color: #6b7280; }
    h2 { font-family: inherit; font-weight: 700; font-size: 1.3rem; color: #111827; }
    .datos dt { color: #6b7280; }
    .total { margin: var(--e-5) 0; padding: var(--e-4) 0; border-top: 1px solid #edf0f3; border-bottom: 1px solid #edf0f3; }
    .total strong { font-size: 1.5rem; }
    .btn-pagar { background: #1d4ed8; color: #fff; margin-bottom: var(--e-2); border-radius: 8px; }
    .btn-pagar:hover:not(:disabled) { background: #1e40af; }
    .nota { margin: var(--e-4) 0 0; text-align: center; }
    .resultado { text-align: center; padding: var(--e-4) 0; }
    .resultado.ok h2 { color: #15803d; }
  `]
})
export class PasarelaComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);

  readonly referencia = input.required<string>();
  readonly checkout = signal<CheckoutVista | null>(null);
  readonly resultado = signal<Pago | null>(null);
  readonly procesando = signal(false);
  readonly error = signal<string | null>(null);
  readonly metodos = METODOS_PAGO;

  ngOnInit(): void {
    this.api.checkout(this.referencia()).subscribe({
      next: c => this.checkout.set(c),
      error: e => this.error.set(mensajeError(e))
    });
  }

  volverA(): unknown[] {
    const id = this.checkout()?.solicitudId;
    const base = this.auth.esCliente() ? '/mi-espacio/solicitud' : '/estudio/solicitudes';
    return [base, id];
  }

  responder(aprobar: boolean): void {
    this.procesando.set(true);
    this.api.responderPasarela(this.referencia(), aprobar).subscribe({
      next: p => { this.resultado.set(p); this.procesando.set(false); },
      error: e => { this.error.set(mensajeError(e)); this.procesando.set(false); }
    });
  }
}
