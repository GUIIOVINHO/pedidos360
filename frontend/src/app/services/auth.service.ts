import { Injectable, inject } from '@angular/core';
import {
  Auth,
  GoogleAuthProvider,
  RecaptchaVerifier,
  createUserWithEmailAndPassword,
  signInWithEmailAndPassword,
  signInWithPhoneNumber,
  signInWithPopup,
  signOut,
  user
} from '@angular/fire/auth';
import { Observable, from } from 'rxjs';

/**
 * Encapsula la integracion con el IDaaS (Firebase Authentication).
 * El flujo de login queda 100% delegado a Firebase: nuestro frontend
 * nunca maneja contraseñas directamente, solo pide a Firebase que
 * autentique y luego usa el JWT (ID Token) que Firebase entrega.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private auth: Auth = inject(Auth);
  private recaptchaVerifier: any = null;
  private confirmationResult: any = null;

  /** Observable con el usuario actual (o null si no hay sesion) */
  readonly user$ = user(this.auth);

  login(email: string, password: string) {
    return from(signInWithEmailAndPassword(this.auth, email, password));
  }

  registrar(email: string, password: string) {
    return from(createUserWithEmailAndPassword(this.auth, email, password));
  }

  loginWithGoogle() {
    const provider = new GoogleAuthProvider();
    provider.setCustomParameters({
      prompt: 'select_account'
    });
    return from(signInWithPopup(this.auth, provider));
  }

  async sendSmsCode(phoneNumber: string): Promise<void> {
    if (!phoneNumber || !phoneNumber.trim()) {
      throw new Error('Debes ingresar un número de teléfono válido.');
    }

    if (this.recaptchaVerifier) {
      this.recaptchaVerifier.clear();
    }

    this.recaptchaVerifier = new RecaptchaVerifier(this.auth, 'recaptcha-container', {
      size: 'invisible',
      callback: () => {}
    });

    this.confirmationResult = await signInWithPhoneNumber(this.auth, phoneNumber, this.recaptchaVerifier);
  }

  async verifySmsCode(code: string): Promise<void> {
    if (!this.confirmationResult) {
      throw new Error('Primero debes enviar el código SMS.');
    }

    if (!code || !code.trim()) {
      throw new Error('Debes ingresar el código recibido por SMS.');
    }

    await this.confirmationResult.confirm(code);
    this.clearRecaptcha();
  }

  clearRecaptcha() {
    if (this.recaptchaVerifier) {
      this.recaptchaVerifier.clear();
      this.recaptchaVerifier = null;
    }
    this.confirmationResult = null;
  }

  logout(): Observable<void> {
    return from(signOut(this.auth));
  }

  /** Obtiene el JWT (ID Token) vigente del usuario autenticado */
  async getIdToken(): Promise<string | null> {
    const current = this.auth.currentUser;
    if (!current) return null;
    return current.getIdToken();
  }
}
