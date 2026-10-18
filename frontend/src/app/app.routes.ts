import { Routes } from '@angular/router';
import { invitadoGuard, rolGuard, sesionGuard } from './core/guards';
import { EstudioLayoutComponent } from './layout/estudio-layout.component';
import { PublicoLayoutComponent } from './layout/publico-layout.component';

const PERSONAL = ['ASISTENTE', 'TATUADOR', 'ADMINISTRADOR'];

export const routes: Routes = [
  // Sitio público y portal del cliente (navegación superior, Imágenes 2–5)
  {
    path: '',
    component: PublicoLayoutComponent,
    children: [
      { path: '', loadComponent: () => import('./pages/publico/inicio.component').then(m => m.InicioComponent), title: 'Handgelus · Arte por comisión y tatuaje de autor' },
      { path: 'ingresar', canActivate: [invitadoGuard], loadComponent: () => import('./pages/publico/ingresar.component').then(m => m.IngresarComponent), title: 'Ingresar · Handgelus' },
      { path: 'registro', canActivate: [invitadoGuard], loadComponent: () => import('./pages/publico/registro.component').then(m => m.RegistroComponent), title: 'Crear cuenta · Handgelus' },
      {
        path: 'mi-espacio',
        canActivate: [rolGuard],
        data: { roles: ['CLIENTE'] },
        children: [
          { path: '', loadComponent: () => import('./pages/cliente/mis-solicitudes.component').then(m => m.MisSolicitudesComponent), title: 'Mis proyectos · Handgelus' },
          { path: 'nueva', loadComponent: () => import('./pages/cliente/nueva-solicitud.component').then(m => m.NuevaSolicitudComponent), title: 'Nueva solicitud · Handgelus' },
          { path: 'solicitud/:id', loadComponent: () => import('./pages/cliente/solicitud-cliente.component').then(m => m.SolicitudClienteComponent), title: 'Mi proyecto · Handgelus' },
          { path: 'perfil', loadComponent: () => import('./pages/cliente/perfil.component').then(m => m.PerfilComponent), title: 'Perfil y pagos · Handgelus' }
        ]
      },
      { path: 'pasarela/:referencia', canActivate: [sesionGuard], loadComponent: () => import('./pages/cliente/pasarela.component').then(m => m.PasarelaComponent), title: 'Pago seguro · Handgelus' }
    ]
  },
  // Panel interno del atelier (barra lateral, Imágenes 6–11)
  {
    path: 'estudio',
    component: EstudioLayoutComponent,
    canActivate: [rolGuard],
    data: { roles: PERSONAL },
    children: [
      { path: '', loadComponent: () => import('./pages/estudio/tablero.component').then(m => m.TableroComponent), title: 'Tablero · Handgelus' },
      { path: 'solicitudes', loadComponent: () => import('./pages/estudio/bandeja.component').then(m => m.BandejaComponent), title: 'Solicitudes · Handgelus' },
      { path: 'solicitudes/:id', loadComponent: () => import('./pages/estudio/solicitud-estudio.component').then(m => m.SolicitudEstudioComponent), title: 'Solicitud · Handgelus' },
      { path: 'agenda', loadComponent: () => import('./pages/estudio/agenda.component').then(m => m.AgendaComponent), title: 'Agenda · Handgelus' },
      { path: 'proyectos', loadComponent: () => import('./pages/estudio/proyectos.component').then(m => m.ProyectosComponent), title: 'Proyectos en curso · Handgelus' },
      { path: 'pagos', loadComponent: () => import('./pages/estudio/pagos.component').then(m => m.PagosComponent), title: 'Control de pagos · Handgelus' },
      { path: 'clientes', loadComponent: () => import('./pages/estudio/clientes.component').then(m => m.ClientesComponent), title: 'Clientes · Handgelus' },
      { path: 'clientes/:id', loadComponent: () => import('./pages/estudio/historial-cliente.component').then(m => m.HistorialClienteComponent), title: 'Historial · Handgelus' },
      { path: 'tarifas', canActivate: [rolGuard], data: { roles: ['TATUADOR', 'ADMINISTRADOR'] }, loadComponent: () => import('./pages/estudio/tarifas.component').then(m => m.TarifasComponent), title: 'Tarifas · Handgelus' },
      { path: 'usuarios', canActivate: [rolGuard], data: { roles: ['ADMINISTRADOR'] }, loadComponent: () => import('./pages/estudio/usuarios.component').then(m => m.UsuariosComponent), title: 'Usuarios · Handgelus' },
      { path: 'correos', loadComponent: () => import('./pages/estudio/correos.component').then(m => m.CorreosComponent), title: 'Notificaciones · Handgelus' }
    ]
  },
  { path: '**', redirectTo: '' }
];
