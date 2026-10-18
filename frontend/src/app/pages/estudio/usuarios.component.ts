import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { AvisosService } from '../../core/avisos.service';
import { ROLES } from '../../core/etiquetas';
import { mensajeError } from '../../core/interceptor';
import { Rol, Usuario } from '../../core/modelos';
import { EtiquetaPipe, FechaPipe, InicialesPipe } from '../../shared/pipes';

interface Edicion {
  id: number | null;
  nombre: string;
  correo: string;
  telefono: string;
  rol: Rol;
  password: string;
}

/** F-01 · Gestionar usuarios del sistema (solo administrador · SWR-01). */
@Component({
  selector: 'hg-usuarios',
  standalone: true,
  imports: [FormsModule, EtiquetaPipe, FechaPipe, InicialesPipe],
  template: `
    <header class="encabezado-pagina">
      <div>
        <h1>Usuarios</h1>
        <p>Cuentas del equipo y de clientes. Cada cuenta tiene un único rol que define a qué puede acceder.</p>
      </div>
      <button type="button" class="btn btn-primario" (click)="nuevo()">Crear cuenta</button>
    </header>

    <nav class="pestanas" aria-label="Rol">
      @for (r of filtros; track r.texto) {
        <button type="button" class="pestana" [class.activa]="filtro() === r.valor" (click)="filtrar(r.valor)">{{ r.texto }}</button>
      }
    </nav>

    <div class="tabla-marco">
      <table class="tabla">
        <thead><tr><th>Nombre</th><th>Correo</th><th>Rol</th><th>Registro</th><th>Estado</th><th></th></tr></thead>
        <tbody>
          @for (u of usuarios(); track u.id) {
            <tr [class.inactivo]="!u.activo">
              <td><span class="monograma">{{ u.nombre | iniciales }}</span> <strong>{{ u.nombre }}</strong></td>
              <td class="pequeno">{{ u.correo }}<div class="mini suave">{{ u.telefono }}</div></td>
              <td>{{ u.rol | etiqueta: roles }}</td>
              <td class="suave">{{ u.fechaRegistro | fecha }}</td>
              <td><span class="sello" [class.sello-exito]="u.activo" [class.sello-neutro]="!u.activo">{{ u.activo ? 'Activa' : 'Inactiva' }}</span></td>
              <td class="derecha">
                <button type="button" class="btn btn-fantasma btn-sm" (click)="editar(u)">Editar</button>
                @if (u.id !== auth.usuario()?.id) {
                  <button type="button" class="btn btn-fantasma btn-sm" (click)="alternar(u)">{{ u.activo ? 'Desactivar' : 'Activar' }}</button>
                }
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>

    @if (edicion(); as e) {
      <div class="velo" (click)="edicion.set(null)">
        <form class="dialogo" (click)="$event.stopPropagation()" (ngSubmit)="guardar(e)" role="dialog" aria-modal="true">
          <h3>{{ e.id ? 'Editar cuenta' : 'Crear cuenta' }}</h3>
          <p class="suave pequeno">{{ e.id ? 'El correo no se puede cambiar.' : 'La contraseña se guarda cifrada con bcrypt.' }}</p>
          <div class="campo">
            <label for="nombre">Nombre</label>
            <input id="nombre" class="control" name="nombre" [(ngModel)]="e.nombre" required>
          </div>
          <div class="rejilla rejilla-2">
            <div class="campo">
              <label for="correo">Correo</label>
              <input id="correo" class="control" type="email" name="correo" [(ngModel)]="e.correo" [disabled]="!!e.id" required>
            </div>
            <div class="campo">
              <label for="telefono">Teléfono</label>
              <input id="telefono" class="control" name="telefono" [(ngModel)]="e.telefono" required>
            </div>
          </div>
          <div class="campo">
            <span class="etiqueta">Rol</span>
            <div class="opciones">
              @for (r of rolesDisponibles; track r) {
                <button type="button" class="opcion" [class.activa]="e.rol === r" (click)="e.rol = r">{{ r | etiqueta: roles }}</button>
              }
            </div>
          </div>
          <div class="campo">
            <label for="password">{{ e.id ? 'Nueva contraseña (opcional)' : 'Contraseña' }}</label>
            <input id="password" class="control" type="password" name="password" [(ngModel)]="e.password" autocomplete="new-password">
            <span class="ayuda">Mínimo 8 caracteres, con letras y números.</span>
          </div>
          <div class="acciones">
            <button type="button" class="btn btn-fantasma" (click)="edicion.set(null)">Cancelar</button>
            <button type="submit" class="btn btn-primario" [disabled]="guardando()">{{ e.id ? 'Guardar cambios' : 'Crear cuenta' }}</button>
          </div>
        </form>
      </div>
    }
  `,
  styles: [`
    .monograma { display: inline-grid; place-items: center; width: 30px; height: 30px; margin-right: 6px; border-radius: 50%; background: var(--tinta); color: var(--lino); font-size: .72rem; }
    tr.inactivo td { opacity: .55; }
  `]
})
export class UsuariosComponent implements OnInit {
  private readonly api = inject(ApiService);
  readonly auth = inject(AuthService);
  private readonly avisos = inject(AvisosService);

  readonly usuarios = signal<Usuario[]>([]);
  readonly filtro = signal<Rol | null>(null);
  readonly edicion = signal<Edicion | null>(null);
  readonly guardando = signal(false);
  readonly roles = ROLES;
  readonly rolesDisponibles: Rol[] = ['ASISTENTE', 'TATUADOR', 'ADMINISTRADOR', 'CLIENTE'];
  readonly filtros: { valor: Rol | null; texto: string }[] = [
    { valor: null, texto: 'Todas' },
    { valor: 'ASISTENTE', texto: 'Asistentes' },
    { valor: 'TATUADOR', texto: 'Tatuadores' },
    { valor: 'ADMINISTRADOR', texto: 'Administradores' },
    { valor: 'CLIENTE', texto: 'Clientes' }
  ];

  ngOnInit(): void {
    this.cargar();
  }

  filtrar(rol: Rol | null): void {
    this.filtro.set(rol);
    this.cargar();
  }

  private cargar(): void {
    this.api.usuarios(this.filtro()).subscribe({
      next: u => this.usuarios.set(u),
      error: e => this.avisos.error(mensajeError(e))
    });
  }

  nuevo(): void {
    this.edicion.set({ id: null, nombre: '', correo: '', telefono: '', rol: 'ASISTENTE', password: '' });
  }

  editar(u: Usuario): void {
    this.edicion.set({ id: u.id, nombre: u.nombre, correo: u.correo, telefono: u.telefono, rol: u.rol, password: '' });
  }

  guardar(e: Edicion): void {
    this.guardando.set(true);
    const peticion = e.id
      ? this.api.actualizarUsuario(e.id, { nombre: e.nombre, telefono: e.telefono, rol: e.rol, nuevaPassword: e.password || null })
      : this.api.crearUsuario({ nombre: e.nombre, correo: e.correo, telefono: e.telefono, password: e.password, rol: e.rol });
    peticion.subscribe({
      next: () => {
        this.guardando.set(false);
        this.edicion.set(null);
        this.avisos.exito(e.id ? 'Cuenta actualizada.' : 'Cuenta creada.');
        this.cargar();
      },
      error: err => { this.guardando.set(false); this.avisos.error(mensajeError(err)); }
    });
  }

  alternar(u: Usuario): void {
    this.api.cambiarEstadoUsuario(u.id, !u.activo).subscribe({
      next: () => { this.avisos.info(u.activo ? 'Cuenta desactivada.' : 'Cuenta activada.'); this.cargar(); },
      error: e => this.avisos.error(mensajeError(e))
    });
  }
}
