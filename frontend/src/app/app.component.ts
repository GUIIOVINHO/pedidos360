import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `
    <h2 style="text-align:center">Pedidos360</h2>
    <router-outlet></router-outlet>
  `
})
export class AppComponent {}
