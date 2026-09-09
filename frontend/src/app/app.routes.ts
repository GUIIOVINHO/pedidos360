import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { PedidosListComponent } from './pedidos/pedidos-list.component';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'pedidos', component: PedidosListComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: 'login' }
];
