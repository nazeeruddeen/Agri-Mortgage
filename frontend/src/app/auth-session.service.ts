import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, finalize, map, shareReplay, tap, throwError } from 'rxjs';
import { environment } from '../environments/environment';
import { AuthResponse, UserInfoResponse } from './agri-mortgage.models';

const STORAGE_KEY = 'agri-mortgage-auth-session';

interface StoredSession {
  accessToken: string;
  refreshToken: string;
  username: string;
  role: string;
}

@Injectable({ providedIn: 'root' })
export class AuthSessionService {
  private readonly sessionSubject = new BehaviorSubject<StoredSession | null>(this.readSession());
  private readonly serverBaseUrl = environment.apiBaseUrl.replace(/\/$/, '').replace(/\/api\/v1$/, '');
  private refreshRequest$: Observable<string> | null = null;
  readonly session$ = this.sessionSubject.asObservable();

  constructor(private readonly http: HttpClient) {}

  get session(): StoredSession | null {
    return this.sessionSubject.value;
  }

  get accessToken(): string | null {
    return this.session?.accessToken ?? null;
  }

  get isAuthenticated(): boolean {
    return !!this.accessToken;
  }

  get refreshToken(): string | null {
    return this.session?.refreshToken ?? null;
  }

  setSession(response: AuthResponse): void {
    const session: StoredSession = {
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      username: response.username,
      role: response.role
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    this.sessionSubject.next(session);
  }

  setUserInfo(userInfo: UserInfoResponse): void {
    if (!this.session) {
      return;
    }
    const session: StoredSession = {
      ...this.session,
      username: userInfo.username,
      role: userInfo.role
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    this.sessionSubject.next(session);
  }

  clear(): void {
    localStorage.removeItem(STORAGE_KEY);
    this.sessionSubject.next(null);
  }

  refreshAccessToken(): Observable<string> {
    const refreshToken = this.refreshToken;
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token available'));
    }

    if (this.refreshRequest$) {
      return this.refreshRequest$;
    }

    this.refreshRequest$ = this.http
      .post<AuthResponse>(`${this.serverBaseUrl}/auth/refresh`, { refreshToken })
      .pipe(
        tap((response) => this.setSession(response)),
        map((response) => response.accessToken),
        shareReplay(1),
        finalize(() => {
          this.refreshRequest$ = null;
        })
      );

    return this.refreshRequest$;
  }

  private readSession(): StoredSession | null {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as StoredSession;
    } catch {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
  }
}
