import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="auth-shell">
      <div class="card auth-card">
        <div class="auth-header">
          <h2>Pedidos360</h2>
          <p>Accede con correo, Google o teléfono</p>
        </div>

        <div class="field-group">
          <input type="email" placeholder="Correo electrónico" [(ngModel)]="email" name="email" autocomplete="email">
        </div>

        <div class="field-group">
          <input type="password" placeholder="Contraseña" [(ngModel)]="password" name="password" autocomplete="current-password">
        </div>

        <div class="button-row">
          <button class="primary-btn" type="button" (click)="login()">Ingresar</button>
          <button class="secondary-btn" type="button" (click)="registrar()">Crear cuenta</button>
        </div>

        <div class="divider"><span>o</span></div>

        <button class="google-btn" type="button" (click)="loginGoogle()">
          Continuar con Google
        </button>

        <div class="phone-block" *ngIf="showSmsStep">
          <p class="helper-text">Verificación de dos factores</p>

          <div class="field-group">
            <input type="tel" placeholder="Número de teléfono +56912345678" [(ngModel)]="phoneNumber" name="phoneNumber">
          </div>

          <div class="button-row sms-row">
            <button class="secondary-btn" type="button" (click)="sendSmsCode()">Enviar código SMS</button>
          </div>

          <div class="field-group" *ngIf="smsCodeSent">
            <input type="text" placeholder="Código recibido" [(ngModel)]="smsCode" name="smsCode">
          </div>

          <div class="button-row sms-row" *ngIf="smsCodeSent">
            <button class="primary-btn" type="button" (click)="verifySmsCode()">Verificar código</button>
          </div>
        </div>

        <div id="recaptcha-container" *ngIf="showSmsStep"></div>

        <p class="error" *ngIf="error">{{ error }}</p>
      </div>
    </div>
  `
})
export class LoginComponent {
  email = '';
  password = '';
  phoneNumber = '';
  smsCode = '';
  smsCodeSent = false;
  showSmsStep = false;
  error = '';

  constructor(private authService: AuthService, private router: Router) {}

  login() {
    this.error = '';
    this.authService.login(this.email, this.password).subscribe({
      next: () => {
        this.showSmsStep = true;
        this.smsCodeSent = false;
        this.smsCode = '';
      },
      error: (err) => this.error = 'No se pudo iniciar sesion: ' + err.message
    });
  }

  registrar() {
    this.error = '';
    this.authService.registrar(this.email, this.password).subscribe({
      next: () => this.router.navigate(['/pedidos']),
      error: (err) => this.error = 'No se pudo registrar: ' + err.message
    });
  }

  loginGoogle() {
    this.error = '';
    this.authService.loginWithGoogle().subscribe({
      next: () => this.router.navigate(['/pedidos']),
      error: (err) => this.error = 'No se pudo iniciar con Google: ' + err.message
    });
  }

  async sendSmsCode() {
    this.error = '';
    try {
      await this.authService.sendSmsCode(this.phoneNumber);
      this.smsCodeSent = true;
    } catch (err: any) {
      this.error = err?.message || 'No se pudo enviar el código SMS.';
    }
  }

  async verifySmsCode() {
    this.error = '';
    try {
      await this.authService.verifySmsCode(this.smsCode);
      this.router.navigate(['/pedidos']);
    } catch (err: any) {
      this.error = err?.message || 'Código SMS incorrecto o inválido.';
    }
  }
}