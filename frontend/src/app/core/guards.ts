import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Rol } from './modelos';
import { AuthService } from './auth.service';

/** Exige sesión iniciada; si no, envía a la pantalla de ingreso conservando el destino. */
export const sesionGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  if (auth.autenticado()) {
    return true;
  }
  return inject(Router).createUrlTree(['/ingresar'], { queryParams: { volver: state.url } });
};

/** Exige uno de los roles indicados en data.roles (SWR-02). */
export const rolGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.autenticado()) {
    return router.createUrlTree(['/ingresar'], { queryParams: { volver: state.url } });
  }
  const roles = (route.data?.['roles'] ?? []) as Rol[];
  if (roles.length === 0 || auth.tieneRol(...roles)) {
    return true;
  }
  return router.parseUrl(auth.rutaInicio());
};

/** Las pantallas públicas de ingreso redirigen a quien ya tiene sesión. */
export const invitadoGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.autenticado() ? inject(Router).parseUrl(auth.rutaInicio()) : true;
};
