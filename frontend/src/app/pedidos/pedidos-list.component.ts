import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Pedido, PedidosService } from '../services/pedidos.service';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-pedidos-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="dashboard-shell">
      <div class="card dashboard-card">
        <div class="dashboard-topbar">
          <div>
            <h3 class="panel-title">Gestión de pedidos</h3>
            <p class="helper-text">Administra tus pedidos de forma segura</p>
          </div>
          <button class="secondary-btn" type="button" (click)="logout()">Cerrar sesión</button>
        </div>

        <div class="new-order">
          <h3 class="section-title">Nuevo pedido</h3>
          <div class="form-grid">
            <input placeholder="Cliente" [(ngModel)]="nuevo.cliente" name="cliente">
            <input placeholder="Producto" [(ngModel)]="nuevo.producto" name="producto">
            <input type="number" placeholder="Cantidad" [(ngModel)]="nuevo.cantidad" name="cantidad">
          </div>
          <div class="form-actions">
            <button class="primary-btn" type="button" (click)="crear()">Crear pedido</button>
          </div>
          <p class="error" *ngIf="error">{{ error }}</p>
        </div>

        <div>
          <h3 class="section-title">Pedidos</h3>
          <table>
            <thead>
              <tr><th>ID</th><th>Cliente</th><th>Producto</th><th>Cantidad</th><th>Estado</th><th></th></tr>
            </thead>
            <tbody>
              <tr *ngFor="let p of pedidos">
                <td>{{ p.id }}</td>
                <td>{{ p.cliente }}</td>
                <td>{{ p.producto }}</td>
                <td>{{ p.cantidad }}</td>
                <td><span class="status-pill">{{ p.estado || 'Activo' }}</span></td>
                <td><button class="danger-btn" type="button" (click)="eliminar(p.id!)">Eliminar</button></td>
              </tr>
            </tbody>
          </table>
          <p class="empty-state" *ngIf="!pedidos.length">No hay pedidos registrados.</p>
        </div>
      </div>
    </div>
  `
})
export class PedidosListComponent implements OnInit {
  pedidos: Pedido[] = [];
  nuevo: Pedido = { cliente: '', producto: '', cantidad: 1 };
  error = '';

  constructor(
    private pedidosService: PedidosService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    this.cargar();
  }

  cargar() {
    this.pedidosService.listar().subscribe({
      next: (data) => this.pedidos = data,
      error: (err) => this.error = 'Error al cargar pedidos: ' + err.message
    });
  }

  crear() {
    this.error = '';
    this.pedidosService.crear(this.nuevo).subscribe({
      next: () => {
        this.nuevo = { cliente: '', producto: '', cantidad: 1 };
        this.cargar();
      },
      error: (err) => this.error = 'Error al crear pedido: ' + err.message
    });
  }

  eliminar(id: number) {
    this.pedidosService.eliminar(id).subscribe({ next: () => this.cargar() });
  }

  logout() {
    this.authService.logout().subscribe(() => this.router.navigate(['/login']));
  }
}
