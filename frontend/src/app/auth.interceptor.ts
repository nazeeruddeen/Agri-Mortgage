import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthSessionService } from './auth-session.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const session = inject(AuthSessionService);
  const isAuthRequest = request.url.includes('/auth/login') || request.url.includes('/auth/refresh');
  const credentialedRequest = request.clone({ withCredentials: true });

  return next(credentialedRequest).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401 || isAuthRequest || !session.isAuthenticated) {
        return throwError(() => error);
      }

      return session.refreshAccessToken().pipe(
        switchMap(() => next(request.clone({ withCredentials: true }))),
        catchError((refreshError) => {
          session.clear();
          return throwError(() => refreshError);
        })
      );
    })
  );
};
