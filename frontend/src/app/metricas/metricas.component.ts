import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { EMPTY, Subscription, timer } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { Metricas, MetricasService } from '../services/metricas.service';

interface Fila {
  nombre: string;
  valor: number;
}

@Component({
  selector: 'app-metricas',
  standalone: true,
  imports: [CommonModule],
  styles: [`
    .topbar-actions { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
    .live { display: inline-flex; align-items: center; gap: 8px; color: var(--muted); font-size: 0.9rem; }
    .dot { width: 10px; height: 10px; border-radius: 50%; background: var(--success); animation: pulso 1.4s infinite; }
    .dot.off { background: var(--danger); animation: none; }
    @keyframes pulso { 0% { opacity: 1; } 50% { opacity: 0.3; } 100% { opacity: 1; } }
    .kpis { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 14px; margin: 18px 0; }
    .kpi { background: var(--panel-soft); border: 1px solid var(--border); border-radius: 14px; padding: 16px; }
    .kpi .valor { font-size: 2rem; font-weight: 700; color: var(--primary-dark); }
    .kpi .etiqueta { color: var(--muted); font-size: 0.85rem; }
    .kpi.alerta { background: #fff1f1; border-color: var(--danger); }
    .kpi.alerta .valor { color: var(--danger); }
    .columnas { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 18px; }
    .barra { height: 8px; border-radius: 6px; background: var(--primary); }
  `],
  template: `
    <div class="dashboard-shell">
      <div class="card dashboard-card">
        <div class="dashboard-topbar">
          <div>
            <h3 class="panel-title">Métricas en tiempo real</h3>
            <p class="helper-text">Eventos de pedidos procesados por Kafka (se actualiza cada 3 segundos)</p>
          </div>
          <div class="topbar-actions">
            <span class="live"><span class="dot" [class.off]="!!error"></span>{{ error ? 'Sin conexión' : 'En vivo' }}</span>
            <button class="secondary-btn" type="button" (click)="volver()">Volver a pedidos</button>
          </div>
        </div>

        <p class="error" *ngIf="error">{{ error }}</p>

        <ng-container *ngIf="metricas">
          <div class="kpis">
            <div class="kpi" [class.alerta]="metricas.pedidosUltimoMinuto >= 5">
              <div class="valor">{{ metricas.pedidosUltimoMinuto }}</div>
              <div class="etiqueta">Pedidos en el último minuto</div>
            </div>
            <div class="kpi">
              <div class="valor">{{ metricas.totalPedidos }}</div>
              <div class="etiqueta">Pedidos creados</div>
            </div>
            <div class="kpi">
              <div class="valor">{{ metricas.totalUnidades }}</div>
              <div class="etiqueta">Unidades pedidas</div>
            </div>
            <div class="kpi">
              <div class="valor">{{ metricas.totalActualizaciones }}</div>
              <div class="etiqueta">Actualizaciones</div>
            </div>
          </div>

          <div class="columnas">
            <div>
              <h3 class="section-title">Unidades por producto</h3>
              <table>
                <thead><tr><th>Producto</th><th>Unidades</th><th></th></tr></thead>
                <tbody>
                  <tr *ngFor="let fila of productos">
                    <td>{{ fila.nombre }}</td>
                    <td>{{ fila.valor }}</td>
                    <td style="width: 40%"><div class="barra" [style.width.%]="porcentaje(fila.valor, maxProducto)"></div></td>
                  </tr>
                </tbody>
              </table>
              <p class="empty-state" *ngIf="!productos.length">Aún no hay datos.</p>
            </div>

            <div>
              <h3 class="section-title">Pedidos por cliente</h3>
              <table>
                <thead><tr><th>Cliente</th><th>Pedidos</th><th></th></tr></thead>
                <tbody>
                  <tr *ngFor="let fila of clientes">
                    <td>{{ fila.nombre }}</td>
                    <td>{{ fila.valor }}</td>
                    <td style="width: 40%"><div class="barra" [style.width.%]="porcentaje(fila.valor, maxCliente)"></div></td>
                  </tr>
                </tbody>
              </table>
              <p class="empty-state" *ngIf="!clientes.length">Aún no hay datos.</p>
            </div>
          </div>

          <h3 class="section-title" style="margin-top: 22px">Últimos eventos recibidos</h3>
          <table>
            <thead><tr><th>Evento</th><th>Pedido</th><th>Cliente</th><th>Producto</th><th>Cantidad</th><th>Recibido</th></tr></thead>
            <tbody>
              <tr *ngFor="let e of metricas.ultimosEventos">
                <td><span class="status-pill">{{ e.tipo }}</span></td>
                <td>#{{ e.pedidoId }}</td>
                <td>{{ e.cliente }}</td>
                <td>{{ e.producto }}</td>
                <td>{{ e.cantidad }}</td>
                <td>{{ e.recibidoEn | date:'HH:mm:ss' }}</td>
              </tr>
            </tbody>
          </table>
          <p class="empty-state" *ngIf="!metricas.ultimosEventos.length">Crea un pedido para ver el flujo de eventos.</p>
        </ng-container>
      </div>
    </div>
  `
})
export class MetricasComponent implements OnInit, OnDestroy {
  metricas: Metricas | null = null;
  productos: Fila[] = [];
  clientes: Fila[] = [];
  maxProducto = 1;
  maxCliente = 1;
  error = '';

  private suscripcion?: Subscription;

  constructor(private metricasService: MetricasService, private router: Router) {}

  ngOnInit() {
    // Consulta cada 3 segundos. Si una consulta falla se muestra el aviso y se sigue intentando en el siguiente ciclo.
    this.suscripcion = timer(0, 3000)
      .pipe(
        switchMap(() =>
          this.metricasService.obtener().pipe(
            catchError(() => {
              this.error = 'No se pudo consultar analytics-service (puerto 8084). Verifica que esté levantado; se reintenta solo.';
              return EMPTY;
            })
          )
        )
      )
      .subscribe((datos) => this.actualizar(datos));
  }

  ngOnDestroy() {
    this.suscripcion?.unsubscribe();
  }

  porcentaje(valor: number, maximo: number): number {
    return maximo > 0 ? Math.max(4, Math.round((valor / maximo) * 100)) : 0;
  }

  volver() {
    this.router.navigate(['/pedidos']);
  }

  private actualizar(datos: Metricas) {
    this.error = '';
    this.metricas = datos;
    this.productos = Object.entries(datos.unidadesPorProducto).map(([nombre, valor]) => ({ nombre, valor }));
    this.clientes = Object.entries(datos.pedidosPorCliente).map(([nombre, valor]) => ({ nombre, valor }));
    this.maxProducto = Math.max(1, ...this.productos.map(f => f.valor));
    this.maxCliente = Math.max(1, ...this.clientes.map(f => f.valor));
  }

}
