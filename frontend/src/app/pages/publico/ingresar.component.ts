import { Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { mensajeError } from '../../core/interceptor';

@Component({
  selector: 'hg-ingresar',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="contenedor acceso">
      <div class="acceso-texto">
        <h1>Bienvenido de nuevo</h1>
        <p class="suave">Ingresa para revisar tus cotizaciones, pagos y el avance de tus proyectos.
          El equipo del atelier usa esta misma entrada.</p>
        <details class="demo">
          <summary>Cuentas de demostración</summary>
          <dl class="datos">
            <dt>Cliente</dt><dd>valeria&#64;correo.com · Cliente2026</dd>
            <dt>Asistente</dt><dd>asistente&#64;handgelus.co · Asistente2026</dd>
            <dt>Artista</dt><dd>artista&#64;handgelus.co · Artista2026</dd>
            <dt>Administrador</dt><dd>admin&#64;handgelus.co · Admin2026</dd>
          </dl>
        </details>
      </div>

      <form class="lamina-hoja" [formGroup]="form" (ngSubmit)="ingresar()" novalidate>
        <div class="campo">
          <label for="correo">Correo electrónico</label>
          <input id="correo" class="control" type="email" formControlName="correo" autocomplete="email"
                 placeholder="tu@correo.com" [class.invalido]="invalido('correo')">
          @if (invalido('correo')) { <span class="error">Ingresa un correo válido.</span> }
        </div>
        <div class="campo">
          <label for="password">Contraseña</label>
          <input id="password" class="control" type="password" formControlName="password"
                 autocomplete="current-password" [class.invalido]="invalido('password')">
          @if (invalido('password')) { <span class="error">Ingresa tu contraseña.</span> }
        </div>
        @if (error()) { <p class="aviso aviso-error">{{ error() }}</p> }
        <button class="btn btn-primario btn-bloque" type="submit" [disabled]="enviando()">
          {{ enviando() ? 'Ingresando…' : 'Ingresar' }}
        </button>
        <p class="pequeno suave alterno">¿Primera vez? <a routerLink="/registro">Crea tu cuenta</a> para enviar una solicitud.</p>
      </form>
    </section>
  `,
  styles: [`
    .acceso { display: grid; grid-template-columns: 1fr 420px; gap: var(--e-8); align-items: center; padding-top: var(--e-8); }
    .demo { margin-top: var(--e-5); font-size: var(--t-sm); }
    .demo summary { cursor: pointer; color: var(--sepia); font-weight: 600; }
    .demo .datos { margin-top: var(--e-3); max-width: 420px; }
    .demo dd { font-weight: 500; }
    .alterno { margin: var(--e-4) 0 0; text-align: center; }
    @media (max-width: 860px) { .acceso { grid-template-columns: minmax(0, 1fr); gap: var(--e-6); padding-top: var(--e-6); } }
  `]
})
export class IngresarComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly volver = input<string>();
  readonly enviando = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    correo: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });

  invalido(campo: 'correo' | 'password'): boolean {
    const c = this.form.controls[campo];
    return c.invalid && (c.dirty || c.touched);
  }

  ingresar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    const { correo, password } = this.form.getRawValue();
    this.auth.login(correo, password).subscribe({
      next: () => {
        const destino = this.volver();
        this.router.navigateByUrl(destino && destino.startsWith('/') ? destino : this.auth.rutaInicio());
      },
      error: e => {
        this.error.set(mensajeError(e));
        this.enviando.set(false);
      }
    });
  }
}
