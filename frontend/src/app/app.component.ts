import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AvisosComponent } from './shared/avisos.component';

@Component({
  selector: 'hg-root',
  standalone: true,
  imports: [RouterOutlet, AvisosComponent],
  template: `
    <router-outlet />
    <hg-avisos />
  `
})
export class AppComponent {}
