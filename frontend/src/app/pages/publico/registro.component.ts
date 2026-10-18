import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { mensajeError } from '../../core/interceptor';

/** F-03 · SWR-06: registro del cliente con nombre, correo único y teléfono. */
@Component({
  selector: 'hg-registro',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="contenedor registro">
      <header>
        <h1>Crea tu cuenta</h1>
        <p class="suave">Con tu cuenta envías solicitudes, recibes cotizaciones y guardas tus comprobantes en un solo lugar.</p>
      </header>

      <form class="lamina-hoja" [formGroup]="form" (ngSubmit)="registrar()" novalidate>
        <div class="campo">
          <label for="nombre">Nombre completo</label>
          <input id="nombre" class="control" formControlName="nombre" autocomplete="name" placeholder="Ej. Ana López"
                 [class.invalido]="invalido('nombre')">
          @if (invalido('nombre')) { <span class="error">Escribe tu nombre (mínimo 3 caracteres).</span> }
        </div>
        <div class="rejilla rejilla-2">
          <div class="campo">
            <label for="correo">Correo electrónico</label>
            <input id="correo" class="control" type="email" formControlName="correo" autocomplete="email"
                   placeholder="ana@correo.com" [class.invalido]="invalido('correo')">
            @if (invalido('correo')) { <span class="error">Ingresa un correo válido.</span> }
          </div>
          <div class="campo">
            <label for="telefono">Teléfono</label>
            <input id="telefono" class="control" type="tel" formControlName="telefono" autocomplete="tel"
                   placeholder="310 555 0101" [class.invalido]="invalido('telefono')">
            @if (invalido('telefono')) { <span class="error">Entre 7 y 15 dígitos.</span> }
          </div>
        </div>
        <div class="campo">
          <label for="password">Contraseña</label>
          <input id="password" class="control" type="password" formControlName="password" autocomplete="new-password"
                 [class.invalido]="invalido('password')">
          <span class="ayuda">Mínimo 8 caracteres, con letras y números.</span>
          @if (invalido('password')) { <span class="error">La contraseña aún no cumple los requisitos.</span> }
        </div>
        <label class="casilla">
          <input type="checkbox" formControlName="aceptaTratamientoDatos">
          <span>Autorizo a Handgelus a tratar mis datos personales para gestionar mis servicios,
            conforme a la Ley 1581 de 2012.</span>
        </label>
        @if (invalido('aceptaTratamientoDatos')) { <p class="error mini">Necesitamos tu autorización para continuar.</p> }
        @if (error()) { <p class="aviso aviso-error">{{ error() }}</p> }
        <button class="btn btn-primario btn-bloque" type="submit" [disabled]="enviando()">
          {{ enviando() ? 'Creando cuenta…' : 'Crear cuenta y continuar' }}
        </button>
        <p class="pequeno suave alterno">¿Ya tienes cuenta? <a routerLink="/ingresar">Ingresa aquí</a>.</p>
      </form>
    </section>
  `,
  styles: [`
    .registro { max-width: 640px; padding-top: var(--e-7); }
    header { margin-bottom: var(--e-5); }
    .casilla { margin: var(--e-2) 0 var(--e-5); }
    .error.mini { color: var(--oxido); margin-top: calc(var(--e-4) * -1); }
    .alterno { margin: var(--e-4) 0 0; text-align: center; }
  `]
})
export class RegistroComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly enviando = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.minLength(3)]],
    correo: ['', [Validators.required, Validators.email]],
    telefono: ['', [Validators.required, Validators.pattern(/^[\d\s+()-]{7,20}$/)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.pattern(/^(?=.*[A-Za-zÁÉÍÓÚáéíóúÑñ])(?=.*\d).+$/)]],
    aceptaTratamientoDatos: [false, Validators.requiredTrue]
  });

  invalido(campo: 'nombre' | 'correo' | 'telefono' | 'password' | 'aceptaTratamientoDatos'): boolean {
    const c = this.form.controls[campo];
    return c.invalid && (c.dirty || c.touched);
  }

  registrar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    this.auth.registrar(this.form.getRawValue()).subscribe({
      next: () => this.router.navigateByUrl('/mi-espacio/nueva'),
      error: e => {
        this.error.set(mensajeError(e));
        this.enviando.set(false);
      }
    });
  }
}
