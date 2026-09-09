import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, switchMap } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Adjunta el JWT (ID Token de Firebase) a cada peticion dirigida a nuestra
 * API. El backend (FirebaseAuthFilter) valida ese token en cada request.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  return from(authService.getIdToken()).pipe(
    switchMap(token => {
      if (!token) {
        return next(req);
      }
      const cloned = req.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
      });
      return next(cloned);
    })
  );
};
