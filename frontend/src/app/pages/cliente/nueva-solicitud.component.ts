import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subscription, debounceTime, switchMap, of, catchError } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AvisosService } from '../../core/avisos.service';
import { ESTILOS, FORMATOS_COMISION, opciones, TAMANOS_TATUAJE } from '../../core/etiquetas';
import { mensajeError } from '../../core/interceptor';
import { CategoriaTamano, Estilo, ResultadoCotizacion, TipoServicio } from '../../core/modelos';
import { CopPipe } from '../../shared/pipes';

const MAX_BYTES = 5 * 1024 * 1024;
const TIPOS_PERMITIDOS = ['image/png', 'image/jpeg', 'image/webp', 'application/pdf'];

/** F-03 · Registrar solicitud de servicio (Imagen 3 · RF-02 / RF-03). Tres bloques (RNF-01). */
@Component({
  selector: 'hg-nueva-solicitud',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, CopPipe],
  template: `
    <section class="contenedor pagina">
      <header class="cabeza">
        <a routerLink="/mi-espacio" class="volver pequeno">Volver a mis proyectos</a>
        <h1>Solicitud de servicio</h1>
        <p class="suave">Revisamos cada solicitud con calma para que la dirección artística se alinee con tu idea.</p>
      </header>

      <div class="rejilla rejilla-detalle">
        <form class="lamina-hoja" [formGroup]="form" (ngSubmit)="enviar()" novalidate>
          <fieldset>
            <legend><span class="paso">1</span> ¿Qué quieres crear?</legend>
            <div class="selector-tipo" role="radiogroup" aria-label="Tipo de servicio">
              <button type="button" class="tarjeta-opcion" [class.activa]="tipo() === 'TATUAJE'"
                      role="radio" [attr.aria-checked]="tipo() === 'TATUAJE'" (click)="elegirTipo('TATUAJE')">
                <strong>Tatuaje de autor</strong>
                <span>Diseño único llevado a tu piel en el atelier.</span>
              </button>
              <button type="button" class="tarjeta-opcion" [class.activa]="tipo() === 'COMISION'"
                      role="radio" [attr.aria-checked]="tipo() === 'COMISION'" (click)="elegirTipo('COMISION')">
                <strong>Obra por comisión</strong>
                <span>Ilustración u óleo enviado a tu dirección.</span>
              </button>
            </div>
          </fieldset>

          <fieldset>
            <legend><span class="paso">2</span> Tu idea</legend>
            <div class="campo">
              <label for="titulo">Nombre del proyecto</label>
              <input id="titulo" class="control" formControlName="titulo" maxlength="150"
                     placeholder="Ej. Manga botánica" [class.invalido]="invalido('titulo')">
              @if (invalido('titulo')) { <span class="error">Dale un nombre a tu proyecto.</span> }
            </div>
            <div class="campo">
              <label for="descripcion">Descripción de la idea</label>
              <textarea id="descripcion" class="control" formControlName="descripcion" maxlength="2000"
                        placeholder="Describe el tema, el significado y los elementos que quieras incluir."
                        [class.invalido]="invalido('descripcion')"></textarea>
              @if (invalido('descripcion')) { <span class="error">Cuéntanos un poco más (mínimo 10 caracteres).</span> }
            </div>
            <div class="campo">
              <span class="etiqueta">Estilo</span>
              <div class="opciones" role="radiogroup" aria-label="Estilo">
                @for (e of estilos; track e.valor) {
                  <button type="button" class="opcion" [class.activa]="form.controls.estilo.value === e.valor"
                          (click)="elegirEstilo(e.valor)">{{ e.etiqueta }}</button>
                }
              </div>
            </div>
            <div class="rejilla rejilla-2">
              <div class="campo">
                <label for="categoria">{{ tipo() === 'TATUAJE' ? 'Tamaño estimado' : 'Formato de la obra' }}</label>
                <select id="categoria" class="control" formControlName="categoria" [class.invalido]="invalido('categoria')">
                  <option value="" disabled>Selecciona</option>
                  @for (t of tamanos(); track t.valor) {
                    <option [value]="t.valor">{{ t.etiqueta }}</option>
                  }
                </select>
                @if (invalido('categoria')) { <span class="error">Selecciona un tamaño.</span> }
              </div>
              @if (tipo() === 'TATUAJE') {
                <div class="campo">
                  <label for="zona">Ubicación en el cuerpo</label>
                  <input id="zona" class="control" formControlName="zonaCuerpo" placeholder="Ej. Antebrazo interno"
                         [class.invalido]="invalido('zonaCuerpo')">
                  @if (invalido('zonaCuerpo')) { <span class="error">Indica la zona del cuerpo.</span> }
                </div>
              } @else {
                <div class="campo">
                  <label for="ciudad">Ciudad de destino</label>
                  <input id="ciudad" class="control" formControlName="ciudadDestino" placeholder="Ej. Pereira"
                         [class.invalido]="invalido('ciudadDestino')">
                  @if (invalido('ciudadDestino')) { <span class="error">Indica la ciudad.</span> }
                </div>
              }
            </div>
            @if (tipo() === 'COMISION') {
              <div class="campo">
                <label for="direccion">Dirección completa de envío</label>
                <input id="direccion" class="control" formControlName="direccionDestino"
                       placeholder="Calle, número, barrio y referencias" [class.invalido]="invalido('direccionDestino')">
                <span class="ayuda">La usamos para calcular el costo de envío.</span>
                @if (invalido('direccionDestino')) { <span class="error">Escribe la dirección completa.</span> }
              </div>
            }
          </fieldset>

          <fieldset>
            <legend><span class="paso">3</span> Referencias</legend>
            <label class="zona-archivo" [class.sobre]="arrastrando()" for="imagen"
                   (dragover)="$event.preventDefault(); arrastrando.set(true)" (dragleave)="arrastrando.set(false)"
                   (drop)="soltar($event)">
              @if (archivo(); as a) {
                <strong>{{ a.name }}</strong>
                <span>{{ (a.size / 1024 / 1024).toFixed(2) }} MB · haz clic para cambiarla</span>
              } @else {
                <strong>Haz clic o arrastra una imagen</strong>
                <span>PNG, JPG, WEBP o PDF · máximo 5 MB</span>
              }
            </label>
            <input id="imagen" type="file" class="oculto" accept="image/png,image/jpeg,image/webp,application/pdf"
                   (change)="seleccionar($event)">
            @if (errorArchivo()) { <p class="error mini">{{ errorArchivo() }}</p> }
          </fieldset>

          @if (error()) { <p class="aviso aviso-error">{{ error() }}</p> }
          <div class="fila enviar">
            <button type="submit" class="btn btn-primario" [disabled]="enviando()">
              {{ enviando() ? 'Enviando…' : 'Enviar solicitud' }}
            </button>
            <span class="mini suave">El artista revisará tu idea y te enviará la cotización definitiva por correo.</span>
          </div>
        </form>

        <aside class="lamina estimado">
          <span class="rotulo">Valor de referencia</span>
          @if (estimado(); as e) {
            <p class="cifra monto">{{ e.valorTotal | cop }}</p>
            <dl class="datos">
              @if (e.tarifaFija) {
                <dt>Tarifa</dt><dd>Fija</dd>
              } @else {
                <dt>Horas estimadas</dt><dd>{{ e.horasEstimadas }}</dd>
                <dt>Complejidad del estilo</dt><dd>× {{ e.factorEstilo }}</dd>
              }
              @if (tipo() === 'TATUAJE' && e.sesionesEstimadas > 1) {
                <dt>Sesiones</dt><dd>{{ e.sesionesEstimadas }}</dd>
              }
              @if (tipo() === 'COMISION') {
                <dt>Envío</dt><dd>{{ e.costoEnvio | cop }}</dd>
              }
              <dt>{{ tipo() === 'TATUAJE' ? 'Abono (30 %)' : 'Anticipo (50 %)' }}</dt>
              <dd>{{ minimo(e) | cop }}</dd>
            </dl>
            <p class="mini suave nota">Es un valor orientativo según la tabla de tarifas del atelier. El artista confirma
              el valor final al revisar tu idea.</p>
          } @else {
            <p class="suave pequeno">Elige el tipo de servicio y el tamaño para ver un valor de referencia.</p>
          }
        </aside>
      </div>
    </section>
  `,
  styles: [`
    .pagina { padding-top: var(--e-6); }
    .cabeza { margin-bottom: var(--e-6); }
    .volver { display: inline-block; margin-bottom: var(--e-4); text-decoration: none; }
    fieldset { border: 0; padding: 0; margin: 0 0 var(--e-6); }
    legend { font-family: var(--f-display); font-size: var(--t-xl); margin-bottom: var(--e-4); display: flex; align-items: center; gap: var(--e-3); }
    .paso { width: 28px; height: 28px; border-radius: 50%; border: 1px solid var(--laton); color: var(--laton); display: grid; place-items: center; font-family: var(--f-texto); font-size: .8rem; font-weight: 700; }
    .oculto { position: absolute; width: 1px; height: 1px; opacity: 0; pointer-events: none; }
    .error.mini { color: var(--oxido); margin-top: var(--e-2); }
    .enviar { border-top: 1px solid var(--arena); padding-top: var(--e-5); }
    .estimado { position: sticky; top: 92px; }
    .monto { font-family: var(--f-display); font-size: var(--t-3xl); line-height: 1; margin: var(--e-2) 0 var(--e-5); }
    .nota { margin: var(--e-4) 0 0; }
    @media (max-width: 960px) { .estimado { position: static; order: -1; } }
  `]
})
export class NuevaSolicitudComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly avisos = inject(AvisosService);
  private suscripcion?: Subscription;

  readonly tipo = signal<TipoServicio>('TATUAJE');
  readonly archivo = signal<File | null>(null);
  readonly errorArchivo = signal<string | null>(null);
  readonly arrastrando = signal(false);
  readonly estimado = signal<ResultadoCotizacion | null>(null);
  readonly enviando = signal(false);
  readonly error = signal<string | null>(null);

  readonly estilos = opciones(ESTILOS);
  readonly tamanos = computed(() => opciones(this.tipo() === 'TATUAJE' ? TAMANOS_TATUAJE : FORMATOS_COMISION));

  readonly form = this.fb.nonNullable.group({
    titulo: ['', [Validators.required, Validators.maxLength(150)]],
    descripcion: ['', [Validators.required, Validators.minLength(10)]],
    estilo: ['' as Estilo | ''],
    categoria: ['' as CategoriaTamano | '', Validators.required],
    zonaCuerpo: ['', Validators.required],
    direccionDestino: [''],
    ciudadDestino: ['']
  });

  ngOnInit(): void {
    this.suscripcion = this.form.valueChanges.pipe(
      debounceTime(300),
      switchMap(v => {
        if (!v.categoria) {
          return of(null);
        }
        return this.api.estimar(this.tipo(), v.categoria as CategoriaTamano, (v.estilo || null) as Estilo | null,
          this.tipo() === 'COMISION' ? (v.ciudadDestino || null) : null).pipe(catchError(() => of(null)));
      })
    ).subscribe(r => this.estimado.set(r));
  }

  ngOnDestroy(): void {
    this.suscripcion?.unsubscribe();
  }

  elegirTipo(tipo: TipoServicio): void {
    this.tipo.set(tipo);
    const c = this.form.controls;
    if (tipo === 'TATUAJE') {
      c.zonaCuerpo.setValidators(Validators.required);
      c.direccionDestino.clearValidators();
      c.ciudadDestino.clearValidators();
    } else {
      c.zonaCuerpo.clearValidators();
      c.direccionDestino.setValidators([Validators.required, Validators.minLength(8)]);
      c.ciudadDestino.setValidators(Validators.required);
    }
    c.zonaCuerpo.updateValueAndValidity({ emitEvent: false });
    c.direccionDestino.updateValueAndValidity({ emitEvent: false });
    c.ciudadDestino.updateValueAndValidity({ emitEvent: false });
    this.form.updateValueAndValidity();
  }

  elegirEstilo(estilo: Estilo): void {
    const actual = this.form.controls.estilo.value;
    this.form.controls.estilo.setValue(actual === estilo ? '' : estilo);
  }

  invalido(campo: 'titulo' | 'descripcion' | 'categoria' | 'zonaCuerpo' | 'direccionDestino' | 'ciudadDestino'): boolean {
    const c = this.form.controls[campo];
    return c.invalid && (c.dirty || c.touched);
  }

  minimo(e: ResultadoCotizacion): number {
    const porcentaje = this.tipo() === 'TATUAJE' ? 0.3 : 0.5;
    return Math.ceil(e.valorTotal * porcentaje);
  }

  seleccionar(evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    this.validarArchivo(entrada.files?.[0] ?? null);
  }

  soltar(evento: DragEvent): void {
    evento.preventDefault();
    this.arrastrando.set(false);
    this.validarArchivo(evento.dataTransfer?.files?.[0] ?? null);
  }

  private validarArchivo(archivo: File | null): void {
    this.errorArchivo.set(null);
    if (!archivo) {
      return;
    }
    if (archivo.size > MAX_BYTES) {   // RF-02: máximo 5 MB
      this.errorArchivo.set('La imagen supera 5 MB. Adjunta una de menor tamaño.');
      return;
    }
    if (!TIPOS_PERMITIDOS.includes(archivo.type)) {
      this.errorArchivo.set('Formato no permitido. Usa PNG, JPG, WEBP o PDF.');
      return;
    }
    this.archivo.set(archivo);
  }

  enviar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error.set('Revisa los campos marcados antes de enviar.');
      return;
    }
    const v = this.form.getRawValue();
    const datos = {
      tipoServicio: this.tipo(),
      titulo: v.titulo,
      descripcion: v.descripcion,
      categoria: v.categoria,
      estilo: v.estilo || null,
      zonaCuerpo: this.tipo() === 'TATUAJE' ? v.zonaCuerpo : null,
      direccionDestino: this.tipo() === 'COMISION' ? v.direccionDestino : null,
      ciudadDestino: this.tipo() === 'COMISION' ? v.ciudadDestino : null
    };
    this.enviando.set(true);
    this.error.set(null);
    this.api.registrarSolicitud(datos, this.archivo()).subscribe({
      next: d => {
        this.avisos.exito('Solicitud enviada. El artista ya puede revisarla.');
        this.router.navigate(['/mi-espacio/solicitud', d.solicitud.id]);
      },
      error: e => {
        this.error.set(mensajeError(e));
        this.enviando.set(false);
      }
    });
  }
}
