import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/** Iconos de trazo fino dibujados a mano para el atelier (24 × 24, 1.5 px). */
const TRAZOS: Record<string, string> = {
  tablero: 'M4 4h7v7H4zM13 4h7v4h-7zM13 10h7v10h-7zM4 13h7v7H4z',
  bandeja: 'M4 13l2.5-8h11L20 13M4 13v6h16v-6M4 13h5l1 2h4l1-2h5',
  agenda: 'M5 6h14v14H5zM5 10h14M9 3v5M15 3v5',
  pincel: 'M14 4l6 6-8.5 8.5a3 3 0 01-4.2 0l-1.8-1.8a3 3 0 010-4.2zM4 20c1.5 0 3-.5 3.5-2',
  pagos: 'M3 7h18v10H3zM3 11h18M7 15h3',
  clientes: 'M9 11a3.5 3.5 0 100-7 3.5 3.5 0 000 7zM3 20c.5-3.5 3-5.5 6-5.5s5.5 2 6 5.5M16 4.5a3 3 0 010 6M18 14.5c1.8.6 3 2.4 3.2 4.5',
  tarifas: 'M4 12l8-8h8v8l-8 8zM15.5 8.5h.01',
  usuarios: 'M12 12a4 4 0 100-8 4 4 0 000 8zM4 20c.6-4 3.8-6 8-6s7.4 2 8 6',
  correo: 'M3 6h18v12H3zM3 7l9 6 9-6',
  salir: 'M10 5H5v14h5M14 8l4 4-4 4M18 12H9',
  mas: 'M12 5v14M5 12h14',
  flecha: 'M5 12h14M13 6l6 6-6 6',
  atras: 'M19 12H5M11 6l-6 6 6 6',
  imagen: 'M4 5h16v14H4zM4 16l5-5 4 4 2-2 5 5M15 9.5h.01',
  subir: 'M12 16V4M7 9l5-5 5 5M4 16v4h16v-4',
  check: 'M5 12.5l4.5 4.5L19 7',
  reloj: 'M12 21a9 9 0 100-18 9 9 0 000 18zM12 7v5l3 2',
  campana: 'M6 16V11a6 6 0 0112 0v5l1.5 2h-15zM10 20.5a2 2 0 004 0',
  documento: 'M7 3h7l4 4v14H7zM14 3v4h4M10 12h5M10 16h5',
  buscar: 'M11 18a7 7 0 100-14 7 7 0 000 14zM20 20l-4-4',
  cerrar: 'M6 6l12 12M18 6L6 18',
  aguja: 'M4 20l9-9M13 11l5-5a2 2 0 013 3l-5 5zM9 15l-2-2',
  envio: 'M3 7h11v9H3zM14 10h4l3 3v3h-7M7 19a1.5 1.5 0 100-3 1.5 1.5 0 000 3zM17 19a1.5 1.5 0 100-3 1.5 1.5 0 000 3z'
};

@Component({
  selector: 'hg-icono',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg [attr.width]="tamano()" [attr.height]="tamano()" viewBox="0 0 24 24" fill="none" stroke="currentColor"
         stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <path [attr.d]="trazo()"></path>
    </svg>
  `,
  styles: [':host { display: inline-flex; flex-shrink: 0; }']
})
export class IconoComponent {
  readonly nombre = input.required<string>();
  readonly tamano = input<number>(18);
  readonly trazo = computed(() => TRAZOS[this.nombre()] ?? TRAZOS['mas']);
}
