import { Injectable, signal } from '@angular/core';

export interface Aviso {
  id: number;
  texto: string;
  tipo: 'info' | 'exito' | 'error';
}

/** Avisos flotantes breves que confirman acciones o explican errores. */
@Injectable({ providedIn: 'root' })
export class AvisosService {
  private secuencia = 0;
  readonly lista = signal<Aviso[]>([]);

  exito(texto: string): void {
    this.mostrar(texto, 'exito');
  }

  error(texto: string): void {
    this.mostrar(texto, 'error', 7000);
  }

  info(texto: string): void {
    this.mostrar(texto, 'info');
  }

  cerrar(id: number): void {
    this.lista.update(l => l.filter(a => a.id !== id));
  }

  private mostrar(texto: string, tipo: Aviso['tipo'], duracion = 4500): void {
    const id = ++this.secuencia;
    this.lista.update(l => [...l, { id, texto, tipo }]);
    setTimeout(() => this.cerrar(id), duracion);
  }
}
