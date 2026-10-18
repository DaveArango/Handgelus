import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/** Añade el token a cada petición al API y cierra la sesión si el servidor la rechaza. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token;
  const peticion = token && req.url.startsWith('/api')
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(peticion).pipe(
    catchError((error: HttpErrorResponse) => {
      const esLogin = req.url.includes('/api/auth/login');
      if (error.status === 401 && token && !esLogin) {
        auth.cerrarSesion();
      }
      return throwError(() => error);
    })
  );
};

/** Extrae un mensaje legible de un error HTTP. */
export function mensajeError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'No hay conexión con el servidor. Verifique que el backend esté en ejecución.';
    }
    const cuerpo = error.error;
    if (cuerpo && typeof cuerpo === 'object' && 'mensaje' in cuerpo) {
      const campos = (cuerpo as { campos?: Record<string, string> }).campos;
      const detalle = campos ? ' ' + Object.values(campos).join('. ') + '.' : '';
      return String((cuerpo as { mensaje: string }).mensaje) + detalle;
    }
  }
  return 'Ocurrió un error inesperado. Intente nuevamente.';
}
