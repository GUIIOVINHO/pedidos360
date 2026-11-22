import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

export interface EventoResumen {
  tipo: string;
  pedidoId: number;
  cliente: string;
  producto: string;
  cantidad: number;
  recibidoEn: string;
}

export interface Metricas {
  totalPedidos: number;
  totalActualizaciones: number;
  totalUnidades: number;
  pedidosUltimoMinuto: number;
  unidadesPorProducto: { [producto: string]: number };
  pedidosPorCliente: { [cliente: string]: number };
  ultimosEventos: EventoResumen[];
  actualizadoEn: string;
}

/**
 * Consulta las metricas en tiempo real que calcula analytics-service a partir del flujo de eventos de Kafka.
 * El interceptor de autenticacion adjunta el JWT de Firebase en cada llamada.
 */
@Injectable({ providedIn: 'root' })
export class MetricasService {
  private baseUrl = `${environment.analyticsUrl}/metricas`;

  constructor(private http: HttpClient) {}

  obtener() {
    return this.http.get<Metricas>(this.baseUrl);
  }
}
