import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';
import {
  AdvanceAgriMortgageStatusRequest,
  AgriDistrictSummary,
  AgriEligibilityResponse,
  AgriMortgageLoanAccountResponse,
  AgriMortgageApplicationResponse,
  AgriMortgageApplicationStatus,
  AgriMortgageDashboardResponse,
  AgriMortgageDocumentResponse,
  AuthResponse,
  CreateAgriMortgageApplicationRequest,
  CreateAgriMortgageDocumentRequest,
  LoginRequest,
  PageResponse,
  RecordAgriRepaymentRequest,
  AgriRepaymentTransactionResponse,
  UpdateAgriMortgageDocumentStatusRequest,
  UserInfoResponse
} from './agri-mortgage.models';

type QueryValue = string | number | boolean | null | undefined;

@Injectable({ providedIn: 'root' })
export class AgriMortgageApiService {
  private readonly baseUrl = environment.apiBaseUrl.replace(/\/$/, '');
  private readonly serverBaseUrl = this.baseUrl.replace(/\/api\/v1$/, '');

  constructor(private readonly http: HttpClient) {}

  login(payload: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.serverBaseUrl}/auth/login`, payload);
  }

  me(): Observable<UserInfoResponse> {
    return this.http.get<UserInfoResponse>(`${this.serverBaseUrl}/auth/me`);
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.serverBaseUrl}/auth/logout`, {});
  }

  createApplication(payload: CreateAgriMortgageApplicationRequest): Observable<AgriMortgageApplicationResponse> {
    return this.http.post<AgriMortgageApplicationResponse>(this.url('/agri-mortgage-applications'), payload);
  }

  getApplication(applicationId: number): Observable<AgriMortgageApplicationResponse> {
    return this.http.get<AgriMortgageApplicationResponse>(this.url(`/agri-mortgage-applications/${applicationId}`));
  }

  documents(applicationId: number): Observable<AgriMortgageDocumentResponse[]> {
    return this.http.get<AgriMortgageDocumentResponse[]>(this.url(`/agri-mortgage-applications/${applicationId}/documents`));
  }

  addDocument(applicationId: number, payload: CreateAgriMortgageDocumentRequest): Observable<AgriMortgageApplicationResponse> {
    return this.http.post<AgriMortgageApplicationResponse>(this.url(`/agri-mortgage-applications/${applicationId}/documents`), payload);
  }

  updateDocumentStatus(
    applicationId: number,
    documentId: number,
    payload: UpdateAgriMortgageDocumentStatusRequest
  ): Observable<AgriMortgageApplicationResponse> {
    return this.http.patch<AgriMortgageApplicationResponse>(
      this.url(`/agri-mortgage-applications/${applicationId}/documents/${documentId}/status`),
      payload
    );
  }

  runEncumbranceCheck(applicationId: number): Observable<AgriMortgageApplicationResponse> {
    return this.http.post<AgriMortgageApplicationResponse>(this.url(`/agri-mortgage-applications/${applicationId}/encumbrance-check`), {});
  }

  evaluate(applicationId: number): Observable<AgriEligibilityResponse> {
    return this.http.post<AgriEligibilityResponse>(this.url(`/agri-mortgage-applications/${applicationId}/evaluate`), {});
  }

  advanceStatus(applicationId: number, payload: AdvanceAgriMortgageStatusRequest): Observable<AgriMortgageApplicationResponse> {
    return this.http.post<AgriMortgageApplicationResponse>(this.url(`/agri-mortgage-applications/${applicationId}/status`), payload);
  }

  search(filters: {
    district?: string;
    taluka?: string;
    status?: AgriMortgageApplicationStatus | null;
    minAmount?: number;
    page?: number;
    size?: number;
  }): Observable<PageResponse<AgriMortgageApplicationResponse>> {
    return this.http.get<PageResponse<AgriMortgageApplicationResponse>>(this.url('/agri-mortgage-applications'), {
      params: this.params(filters)
    });
  }

  getLoanAccount(applicationId: number): Observable<AgriMortgageLoanAccountResponse> {
    return this.http.get<AgriMortgageLoanAccountResponse>(this.url(`/agri-mortgage-applications/${applicationId}/loan-account`));
  }

  listLoanAccounts(filters: { page?: number; size?: number } = {}): Observable<PageResponse<AgriMortgageLoanAccountResponse>> {
    return this.http.get<PageResponse<AgriMortgageLoanAccountResponse>>(this.url('/agri-mortgage-applications/loan-accounts'), {
      params: this.params(filters)
    });
  }

  recordRepayment(accountId: number, payload: RecordAgriRepaymentRequest): Observable<AgriRepaymentTransactionResponse> {
    return this.http.post<AgriRepaymentTransactionResponse>(this.url(`/agri-mortgage-applications/loan-accounts/${accountId}/repayments`), payload);
  }

  summary(): Observable<AgriMortgageDashboardResponse> {
    return this.http.get<AgriMortgageDashboardResponse>(this.url('/agri-mortgage-applications/summary'));
  }

  exportApplications(): Observable<Blob> {
    return this.http.get(this.url('/agri-mortgage-applications/export'), { responseType: 'blob' });
  }

  districtSummary(): Observable<AgriDistrictSummary[]> {
    return this.http.get<AgriDistrictSummary[]>(this.url('/agri-mortgage-applications/reports/district-summary'));
  }

  private url(path: string): string {
    return `${this.baseUrl}${path}`;
  }

  private params(filters: Record<string, QueryValue>): HttpParams {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filters)) {
      if (value === null || value === undefined || value === '') {
        continue;
      }
      params = params.set(key, String(value));
    }
    return params;
  }
}
