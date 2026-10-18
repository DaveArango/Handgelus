import { Pipe, PipeTransform, inject } from '@angular/core';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { Observable, catchError, map, of, shareReplay } from 'rxjs';
import { ApiService } from '../core/api.service';
import { deIso, fechaLarga } from '../core/fechas';

const FORMATO_COP = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });

/** Valor en pesos colombianos: $ 1.300.000 */
@Pipe({ name: 'cop', standalone: true })
export class CopPipe implements PipeTransform {
  transform(valor: number | null | undefined): string {
    return valor === null || valor === undefined ? '—' : FORMATO_COP.format(valor);
  }
}

/** Busca la etiqueta en español de un valor: {{ estado | etiqueta: ESTADOS }} */
@Pipe({ name: 'etiqueta', standalone: true })
export class EtiquetaPipe implements PipeTransform {
  transform(valor: string | null | undefined, mapa: Record<string, string>): string {
    return valor ? (mapa[valor] ?? valor) : '—';
  }
}

/** Fecha corta "19 oct 2026" o larga "lunes 19 de octubre". */
@Pipe({ name: 'fecha', standalone: true })
export class FechaPipe implements PipeTransform {
  private static readonly corta = new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short', year: 'numeric' });
  private static readonly conHora = new Intl.DateTimeFormat('es-CO', {
    day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit'
  });

  transform(valor: string | null | undefined, formato: 'corta' | 'larga' | 'hora' = 'corta'): string {
    if (!valor) {
      return '—';
    }
    if (formato === 'larga') {
      return fechaLarga(valor);
    }
    const fecha = valor.length <= 10 ? deIso(valor) : new Date(valor);
    return formato === 'hora' ? FechaPipe.conHora.format(fecha) : FechaPipe.corta.format(fecha);
  }
}

/** Iniciales para los monogramas: "Valeria Méndez" → "VM". */
@Pipe({ name: 'iniciales', standalone: true })
export class InicialesPipe implements PipeTransform {
  transform(nombre: string | null | undefined): string {
    if (!nombre) {
      return '·';
    }
    return nombre.split(/\s+/).filter(Boolean).slice(0, 2).map(p => p[0].toUpperCase()).join('');
  }
}

/**
 * Carga una imagen protegida del API con el token de la sesión y devuelve una URL local.
 * Uso: <img [src]="nombre | imagenSegura | async">
 */
@Pipe({ name: 'imagenSegura', standalone: true })
export class ImagenSeguraPipe implements PipeTransform {
  private readonly api = inject(ApiService);
  private readonly sanitizer = inject(DomSanitizer);
  private static readonly cache = new Map<string, Observable<SafeUrl | null>>();

  transform(nombre: string | null | undefined): Observable<SafeUrl | null> {
    if (!nombre) {
      return of(null);
    }
    const existente = ImagenSeguraPipe.cache.get(nombre);
    if (existente) {
      return existente;
    }
    const flujo = this.api.archivo(nombre).pipe(
      map(blob => this.sanitizer.bypassSecurityTrustUrl(URL.createObjectURL(blob))),
      catchError(() => of(null)),
      shareReplay(1)
    );
    ImagenSeguraPipe.cache.set(nombre, flujo);
    return flujo;
  }
}

export const PIPES = [CopPipe, EtiquetaPipe, FechaPipe, InicialesPipe, ImagenSeguraPipe] as const;
