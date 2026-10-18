import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { Rol, Sesion, Usuario } from './modelos';

const CLAVE = 'handgelus.sesion';

/** Sesión del usuario: token JWT y datos básicos, guardados en el navegador. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly sesion = signal<Sesion | null>(this.leer());

  readonly usuario = computed<Usuario | null>(() => this.sesion()?.usuario ?? null);
  readonly autenticado = computed(() => this.sesion() !== null);
  readonly esCliente = computed(() => this.usuario()?.rol === 'CLIENTE');
  readonly esPersonal = computed(() => !!this.usuario() && this.usuario()!.rol !== 'CLIENTE');

  get token(): string | null {
    return this.sesion()?.token ?? null;
  }

  tieneRol(...roles: Rol[]): boolean {
    const u = this.usuario();
    return !!u && roles.includes(u.rol);
  }

  login(correo: string, password: string): Observable<Sesion> {
    return this.http.post<Sesion>('/api/auth/login', { correo, password }).pipe(tap(s => this.guardar(s)));
  }

  registrar(datos: { nombre: string; correo: string; telefono: string; password: string; aceptaTratamientoDatos: boolean }): Observable<Sesion> {
    return this.http.post<Sesion>('/api/auth/registro', datos).pipe(tap(s => this.guardar(s)));
  }

  actualizarPerfil(nombre: string, telefono: string): Observable<Usuario> {
    return this.http.put<Usuario>('/api/auth/perfil', { nombre, telefono }).pipe(tap(u => {
      const actual = this.sesion();
      if (actual) {
        this.guardar({ ...actual, usuario: u });
      }
    }));
  }

  /** Ruta de inicio según el rol. */
  rutaInicio(): string {
    const u = this.usuario();
    if (!u) {
      return '/';
    }
    return u.rol === 'CLIENTE' ? '/mi-espacio' : '/estudio';
  }

  cerrarSesion(redirigir = true): void {
    this.sesion.set(null);
    try {
      localStorage.removeItem(CLAVE);
    } catch { /* almacenamiento no disponible */ }
    if (redirigir) {
      this.router.navigateByUrl('/ingresar');
    }
  }

  private guardar(s: Sesion): void {
    this.sesion.set(s);
    try {
      localStorage.setItem(CLAVE, JSON.stringify(s));
    } catch { /* almacenamiento no disponible */ }
  }

  private leer(): Sesion | null {
    try {
      const crudo = localStorage.getItem(CLAVE);
      return crudo ? JSON.parse(crudo) as Sesion : null;
    } catch {
      return null;
    }
  }
}
