import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

export interface Pedido {
  id?: number;
  cliente: string;
  producto: string;
  cantidad: number;
  estado?: string;
}

@Injectable({ providedIn: 'root' })
export class PedidosService {
  private baseUrl = `${environment.apiUrl}/pedidos`;

  constructor(private http: HttpClient) {}

  listar() {
    return this.http.get<Pedido[]>(this.baseUrl);
  }

  crear(pedido: Pedido) {
    return this.http.post<Pedido>(this.baseUrl, pedido);
  }

  eliminar(id: number) {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
