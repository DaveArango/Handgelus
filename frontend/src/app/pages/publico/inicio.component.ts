import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Tarifa, TipoServicio } from '../../core/modelos';
import { IlustracionBotanicaComponent } from '../../shared/ilustracion-botanica.component';
import { CopPipe } from '../../shared/pipes';

@Component({
  selector: 'hg-inicio',
  standalone: true,
  imports: [RouterLink, IlustracionBotanicaComponent, CopPipe],
  template: `
    <section class="portada contenedor">
      <div class="portada-texto">
        <p class="cursiva lugar">Atelier en Armenia, Quindío</p>
        <h1>Arte por comisión<br>y tatuaje de autor.</h1>
        <p class="entrada">
          Cada pieza nace como un boceto hecho a mano para una sola persona. Cuéntanos tu idea,
          recibe una cotización clara y sigue cada etapa —del primer trazo a la última sesión— desde aquí.
        </p>
        <div class="fila acciones">
          <a [routerLink]="destinoSolicitud()" class="btn btn-primario">Iniciar solicitud</a>
          @if (!auth.autenticado()) {
            <a routerLink="/ingresar" class="btn btn-secundario">Ya tengo cuenta</a>
          }
        </div>
      </div>
      <figure class="portada-arte">
        <div class="arco">
          <hg-ilustracion-botanica />
        </div>
        <figcaption class="cursiva suave">Helecho, orquídea y café: boceto para una manga botánica.</figcaption>
      </figure>
    </section>

    <section id="proceso" class="proceso">
      <div class="contenedor">
        <header class="seccion-cabeza">
          <h2>El proceso</h2>
          <p class="suave">Cuatro pasos, sin mensajes perdidos entre redes sociales.</p>
        </header>
        <ol class="pasos">
          <li>
            <span class="numero">1</span>
            <h3>Solicitud</h3>
            <p>Describe tu idea, el tamaño y la zona del cuerpo o el destino de la obra. Adjunta tus referencias.</p>
          </li>
          <li>
            <span class="numero">2</span>
            <h3>Cotización</h3>
            <p>Ves un valor de referencia al instante; el artista lo revisa y te envía la cotización definitiva por correo.</p>
          </li>
          <li>
            <span class="numero">3</span>
            <h3>Abono y agenda</h3>
            <p>Con el 30 % de abono reservamos tu jornada; las comisiones inician con el 50 % de anticipo.</p>
          </li>
          <li>
            <span class="numero">4</span>
            <h3>Seguimiento</h3>
            <p>Consulta el avance de tu pieza, tus pagos y comprobantes en cualquier momento.</p>
          </li>
        </ol>
      </div>
    </section>

    <section id="servicios" class="contenedor servicios">
      <article class="servicio">
        <h2>Tatuaje de autor</h2>
        <p class="suave">Línea fina, blackwork, microrrealismo y botánica. Diseños únicos que no se repiten.</p>
        <dl class="datos">
          @for (t of tarifasDe('TATUAJE'); track t.id) {
            <dt>{{ nombresTatuaje[t.categoria] }}</dt>
            <dd class="cifra">{{ t.tarifaFija ? '' : 'desde ' }}{{ t.valorBase | cop }}</dd>
          }
        </dl>
      </article>
      <article class="servicio">
        <h2>Obra por comisión</h2>
        <p class="suave">Ilustración, acuarela y óleo en formatos estándar, enviados a cualquier ciudad del país.</p>
        <dl class="datos">
          @for (t of tarifasDe('COMISION'); track t.id) {
            <dt>{{ nombresComision[t.categoria] }}</dt>
            <dd class="cifra">desde {{ t.valorBase | cop }}</dd>
          }
        </dl>
      </article>
    </section>
  `,
  styles: [`
    .portada {
      display: grid; grid-template-columns: minmax(0, 1.05fr) minmax(0, .95fr);
      gap: var(--e-7); align-items: center; padding-top: var(--e-7); padding-bottom: var(--e-8);
    }
    .lugar { font-size: var(--t-lg); color: var(--sepia); margin-bottom: var(--e-3); }
    h1 { font-size: var(--t-4xl); line-height: 1.02; margin-bottom: var(--e-5); font-weight: 500; }
    .entrada { font-size: 1.08rem; color: var(--grafito); max-width: 46ch; }
    .acciones { margin-top: var(--e-5); }
    .portada-arte { margin: 0; }
    .arco {
      border-radius: var(--r-arco);
      background: var(--pergamino);
      border: 1px solid var(--arena);
      padding: var(--e-7) var(--e-6) var(--e-5);
      box-shadow: var(--sombra-papel);
      max-width: 440px; margin-left: auto;
    }
    figcaption { text-align: right; margin-top: var(--e-3); font-size: var(--t-md); }
    .proceso { background: var(--pergamino); border-top: 1px solid var(--arena); border-bottom: 1px solid var(--arena); padding: var(--e-8) 0; }
    .seccion-cabeza { margin-bottom: var(--e-6); }
    .seccion-cabeza p { margin: 0; }
    .pasos { list-style: none; margin: 0; padding: 0; display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: var(--e-6); }
    .pasos li { border-top: 1px solid var(--tinta); padding-top: var(--e-4); }
    .numero { font-family: var(--f-display); font-size: var(--t-3xl); line-height: 1; color: var(--laton); }
    .pasos h3 { margin: var(--e-3) 0 var(--e-2); }
    .pasos p { font-size: var(--t-sm); color: var(--grafito); margin: 0; }
    .servicios { display: grid; grid-template-columns: 1fr 1fr; gap: var(--e-7); padding-top: var(--e-8); }
    .servicio .datos { margin-top: var(--e-5); border-top: 1px solid var(--arena); padding-top: var(--e-4); }
    @media (max-width: 900px) {
      .portada { grid-template-columns: minmax(0, 1fr); padding-top: var(--e-6); }
      .arco { margin: 0 auto; }
      .pasos { grid-template-columns: 1fr 1fr; }
      .servicios { grid-template-columns: minmax(0, 1fr); }
    }
    @media (max-width: 560px) { .pasos { grid-template-columns: minmax(0, 1fr); } }
  `]
})
export class InicioComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);
  readonly tarifas = signal<Tarifa[]>([]);
  readonly destinoSolicitud = computed(() =>
    this.auth.esCliente() ? '/mi-espacio/nueva' : this.auth.esPersonal() ? '/estudio' : '/registro');

  readonly nombresTatuaje: Record<string, string> = { PEQUENO: 'Pequeño', MEDIANO: 'Mediano', GRANDE: 'Grande', EXTRA_GRANDE: 'Extra grande' };
  readonly nombresComision: Record<string, string> = { PEQUENO: 'Formato A5', MEDIANO: 'Formato A4', GRANDE: 'Formato A3', EXTRA_GRANDE: 'Formato A2' };

  ngOnInit(): void {
    this.api.tarifas().subscribe({ next: t => this.tarifas.set(t), error: () => this.tarifas.set([]) });
  }

  tarifasDe(tipo: TipoServicio): Tarifa[] {
    return this.tarifas().filter(t => t.tipoServicio === tipo);
  }
}
