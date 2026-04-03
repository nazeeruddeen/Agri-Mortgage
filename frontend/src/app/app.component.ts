import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { AbstractControl, FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable, forkJoin, of } from 'rxjs';
import { AgriMortgageApiService } from './agri-mortgage-api.service';
import { environment } from '../environments/environment';
import {
  AdvanceAgriMortgageStatusRequest,
  AgriDistrictSummary,
  AgriEligibilityResponse,
  AgriMortgageApplicationResponse,
  AgriMortgageApplicationStatus,
  AgriMortgageDashboardResponse,
  AgriMortgageDocumentResponse,
  AgriMortgageDocumentStatus,
  AgriMortgageDocumentType,
  ApiErrorResponse,
  AuthResponse,
  CoBorrowerRequest,
  CreateAgriMortgageApplicationRequest,
  CreateAgriMortgageDocumentRequest,
  LandParcelRequest,
  LandType,
  LoginRequest,
  OwnershipStatus,
  EncumbranceStatus,
  EncumbranceVerificationStatus,
  RelationshipType,
  UpdateAgriMortgageDocumentStatusRequest,
  UserInfoResponse
} from './agri-mortgage.models';
import { AuthSessionService } from './auth-session.service';

type NoticeKind = 'info' | 'success' | 'warning' | 'danger';
type AppTab = 'dashboard' | 'intake' | 'search' | 'operations';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit {
  readonly title = 'Agri Mortgage Loan System';
  readonly apiBaseUrl = `${environment.apiBaseUrl}/agri-mortgage-applications`;
  readonly tabs: AppTab[] = ['dashboard', 'intake', 'search', 'operations'];
  readonly statuses: AgriMortgageApplicationStatus[] = [
    'DRAFT',
    'LAND_VERIFICATION',
    'ENCUMBRANCE_CHECK',
    'CREDIT_REVIEW',
    'LEGAL_REVIEW',
    'SANCTIONED',
    'DISBURSED',
    'CLOSED',
    'REJECTED'
  ];
  readonly relationshipTypes: RelationshipType[] = ['SPOUSE', 'PARENT', 'CHILD', 'SIBLING', 'BUSINESS_PARTNER', 'OTHER'];
  readonly landTypes: LandType[] = ['IRRIGATED', 'DRY', 'HORTICULTURE', 'FOREST_ADJACENT'];
  readonly ownershipStatuses: OwnershipStatus[] = ['SOLE', 'JOINT', 'DISPUTED'];
  readonly encumbranceStatuses: EncumbranceStatus[] = ['CLEAR', 'ENCUMBERED', 'PENDING'];
  readonly documentTypes: AgriMortgageDocumentType[] = [
    'PATTADAR_PASSBOOK',
    'OWNERSHIP_PROOF',
    'ENCUMBRANCE_CERTIFICATE',
    'LAND_VALUATION_REPORT',
    'APPLICANT_KYC',
    'OTHER'
  ];

  activeTab: AppTab = 'dashboard';
  bootstrapping = false;
  pageBusy = false;
  actionBusy: string | null = null;
  notice: { kind: NoticeKind; text: string } = { kind: 'info', text: 'Sign in to load the live agri mortgage workspace.' };

  currentUser: UserInfoResponse | null = null;
  authResponse: AuthResponse | null = null;
  summary: AgriMortgageDashboardResponse | null = null;
  eligibility: AgriEligibilityResponse | null = null;
  districtSummary: AgriDistrictSummary[] = [];
  applications: AgriMortgageApplicationResponse[] = [];
  selectedApplication: AgriMortgageApplicationResponse | null = null;
  reportPage = 0;

  authForm = this.fb.group<any>({
    username: ['', [Validators.required]],
    password: ['', [Validators.required]]
  });

  applicationForm = this.fb.group<any>({
    primaryApplicantName: ['', [Validators.required, Validators.maxLength(150)]],
    primaryApplicantAadhaar: ['', [Validators.required, Validators.minLength(12), Validators.maxLength(12)]],
    primaryApplicantPan: ['', [Validators.required, Validators.maxLength(10)]],
    primaryMonthlyIncome: [null, [Validators.required, Validators.min(1)]],
    district: ['', [Validators.required, Validators.maxLength(80)]],
    taluka: ['', [Validators.required, Validators.maxLength(80)]],
    village: ['', [Validators.required, Validators.maxLength(80)]],
    requestedAmount: [null, [Validators.required, Validators.min(1)]],
    requestedTenureMonths: [36, [Validators.required, Validators.min(1)]],
    purpose: ['', [Validators.required, Validators.maxLength(200)]],
    coBorrowers: this.fb.array([this.createCoBorrowerGroup()]),
    landParcels: this.fb.array([this.createLandParcelGroup()])
  });

  searchForm = this.fb.group<any>({
    district: [''],
    taluka: [''],
    status: [''],
    minAmount: [null],
    size: [10]
  });

  statusForm = this.fb.group<any>({
    targetStatus: ['LAND_VERIFICATION', [Validators.required]],
    remarks: ['Proceed to next stage', [Validators.maxLength(500)]]
  });

  documentForm = this.fb.group<any>({
    documentType: ['PATTADAR_PASSBOOK', [Validators.required]],
    fileName: ['', [Validators.required, Validators.maxLength(180)]],
    fileReference: ['', [Validators.required, Validators.maxLength(255)]],
    remarks: ['']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly api: AgriMortgageApiService,
    private readonly authSession: AuthSessionService
  ) {}

  ngOnInit(): void {
    if (this.authSession.isAuthenticated) {
      this.restoreSession();
    }
  }

  get isAuthenticated(): boolean {
    return this.authSession.isAuthenticated;
  }

  get coBorrowersArray(): FormArray {
    return this.applicationForm.get('coBorrowers') as FormArray;
  }

  get landParcelsArray(): FormArray {
    return this.applicationForm.get('landParcels') as FormArray;
  }

  setTab(tab: AppTab): void {
    this.activeTab = tab;
  }

  login(): void {
    if (this.authForm.invalid) {
      this.touch(this.authForm);
      this.notice = { kind: 'warning', text: 'Enter username and password to sign in.' };
      return;
    }

    this.actionBusy = 'login';
    const payload = this.authForm.getRawValue() as unknown as LoginRequest;
    this.api.login(payload).subscribe({
      next: (response) => {
        this.authSession.setSession(response);
        this.authResponse = response;
        this.currentUser = { username: response.username, role: response.role };
        this.actionBusy = null;
        this.notice = { kind: 'success', text: `Signed in as ${response.username}. Loading agri mortgage data.` };
        this.refreshAll(true);
      },
      error: (error) => {
        this.actionBusy = null;
        this.handleError(error, 'Unable to sign in');
      }
    });
  }

  logout(): void {
    const refreshToken = this.authSession.session?.refreshToken;
    this.actionBusy = 'logout';
    const request = refreshToken ? this.api.logout(refreshToken) : of(void 0);
    request.subscribe({
      next: () => this.clearSession('Signed out from the agri mortgage workspace.'),
      error: () => this.clearSession('Session cleared locally after logout attempt.')
    });
  }

  refreshAll(resetSelection = false): void {
    if (!this.isAuthenticated) {
      return;
    }

    this.pageBusy = true;
    this.notice = { kind: 'info', text: 'Refreshing dashboard, district summary, and mortgage applications.' };
    forkJoin({
      me: this.api.me(),
      summary: this.api.summary(),
      districtSummary: this.api.districtSummary(),
      search: this.api.search(this.searchFilters())
    }).subscribe({
      next: ({ me, summary, districtSummary, search }) => {
        this.currentUser = me;
        this.authSession.setUserInfo(me);
        this.summary = summary;
        this.districtSummary = districtSummary;
        this.applications = search.content;
        this.selectedApplication = resetSelection
          ? search.content[0] ?? null
          : search.content.find((item) => item.id === this.selectedApplication?.id) ?? search.content[0] ?? null;
        if (this.selectedApplication) {
          this.patchStatusForm(this.selectedApplication);
        }
        this.pageBusy = false;
        this.notice = { kind: 'success', text: 'Agri mortgage data refreshed successfully.' };
      },
      error: (error) => {
        this.pageBusy = false;
        this.handleError(error, 'Unable to refresh the agri mortgage workspace');
      }
    });
  }

  addCoBorrower(): void {
    this.coBorrowersArray.push(this.createCoBorrowerGroup());
  }

  removeCoBorrower(index: number): void {
    if (this.coBorrowersArray.length > 1) {
      this.coBorrowersArray.removeAt(index);
    }
  }

  addLandParcel(): void {
    this.landParcelsArray.push(this.createLandParcelGroup());
  }

  removeLandParcel(index: number): void {
    if (this.landParcelsArray.length > 1) {
      this.landParcelsArray.removeAt(index);
    }
  }

  createApplication(): void {
    if (this.applicationForm.invalid) {
      this.touch(this.applicationForm);
      this.notice = { kind: 'warning', text: 'Complete the mortgage intake form before creating the draft.' };
      return;
    }

    this.runAction(
      'createApplication',
      () => this.api.createApplication(this.applicationForm.getRawValue() as unknown as CreateAgriMortgageApplicationRequest),
      (application) => {
        this.resetApplicationForm();
        this.activeTab = 'search';
        this.refreshAll(true);
        this.selectedApplication = application;
        this.notice = { kind: 'success', text: `Draft ${application.applicationNumber} created successfully.` };
      },
      'Unable to create agri mortgage application'
    );
  }

  applySearch(): void {
    this.pageBusy = true;
    this.api.search(this.searchFilters()).subscribe({
      next: (page) => {
        this.pageBusy = false;
        this.applications = page.content;
        this.selectedApplication = page.content.find((item) => item.id === this.selectedApplication?.id) ?? page.content[0] ?? null;
        if (this.selectedApplication) {
          this.patchStatusForm(this.selectedApplication);
        }
        this.notice = { kind: 'info', text: `Loaded ${page.totalElements} application(s) matching the current filters.` };
      },
      error: (error) => {
        this.pageBusy = false;
        this.handleError(error, 'Unable to search agri mortgage applications');
      }
    });
  }

  clearSearch(): void {
    this.searchForm.reset({
      district: '',
      taluka: '',
      status: '',
      minAmount: null,
      size: 10
    });
    this.reportPage = 0;
    this.applySearch();
  }

  selectApplication(application: AgriMortgageApplicationResponse): void {
    this.selectedApplication = application;
    this.patchStatusForm(application);
    this.notice = { kind: 'info', text: `Application ${application.applicationNumber} selected.` };
  }

  evaluateSelected(): void {
    if (!this.selectedApplication) {
      this.notice = { kind: 'warning', text: 'Select an application first.' };
      return;
    }

    this.runAction(
      'evaluate',
      () => this.api.evaluate(this.selectedApplication!.id),
      (result) => {
        this.eligibility = result;
        this.refreshSelectedApplication(this.selectedApplication!.id, result.summary);
      },
      'Unable to evaluate agri mortgage eligibility'
    );
  }

  runEncumbranceCheck(): void {
    if (!this.selectedApplication) {
      this.notice = { kind: 'warning', text: 'Select an application first.' };
      return;
    }

    this.runAction(
      'encumbranceCheck',
      () => this.api.runEncumbranceCheck(this.selectedApplication!.id),
      (application) => {
        this.upsertApplication(application);
        this.patchStatusForm(application);
        if (application.encumbranceVerificationStatus === 'GATEWAY_ERROR') {
          this.notice = {
            kind: 'warning',
            text: `Encumbrance verification fell back after retries. ${application.encumbranceVerificationSummary}. Retry when the external registry is healthy.`
          };
          return;
        }
        if (application.encumbranceVerificationStatus === 'PENDING_VERIFICATION') {
          this.notice = {
            kind: 'warning',
            text: `Encumbrance verification is still pending confirmation. ${application.encumbranceVerificationSummary}.`
          };
          return;
        }
        this.notice = { kind: 'success', text: `Encumbrance verification completed: ${application.encumbranceVerificationSummary}` };
      },
      'Unable to run encumbrance verification'
    );
  }

  addDocument(): void {
    if (!this.selectedApplication) {
      this.notice = { kind: 'warning', text: 'Select an application first.' };
      return;
    }
    if (this.documentForm.invalid) {
      this.touch(this.documentForm);
      this.notice = { kind: 'warning', text: 'Complete the document metadata form before adding a document.' };
      return;
    }

    this.runAction(
      'addDocument',
      () => this.api.addDocument(this.selectedApplication!.id, this.documentForm.getRawValue() as unknown as CreateAgriMortgageDocumentRequest),
      (application) => {
        this.documentForm.patchValue({
          documentType: 'PATTADAR_PASSBOOK',
          fileName: '',
          fileReference: '',
          remarks: ''
        });
        this.upsertApplication(application);
        this.patchStatusForm(application);
        this.notice = { kind: 'success', text: 'Document metadata added to the selected application.' };
      },
      'Unable to add document metadata'
    );
  }

  verifyDocument(document: AgriMortgageDocumentResponse, documentStatus: AgriMortgageDocumentStatus): void {
    if (!this.selectedApplication) {
      return;
    }

    const payload: UpdateAgriMortgageDocumentStatusRequest = {
      documentStatus,
      remarks: documentStatus === 'VERIFIED' ? 'Verified during legal review' : 'Rejected during legal review'
    };

    this.runAction(
      'documentStatus',
      () => this.api.updateDocumentStatus(this.selectedApplication!.id, document.id, payload),
      (application) => {
        this.upsertApplication(application);
        this.patchStatusForm(application);
        this.notice = { kind: 'success', text: `${this.formatLabel(document.documentType)} marked as ${documentStatus}.` };
      },
      'Unable to update document status'
    );
  }

  advanceStatus(): void {
    if (!this.selectedApplication) {
      this.notice = { kind: 'warning', text: 'Select an application first.' };
      return;
    }
    if (this.statusForm.invalid) {
      this.touch(this.statusForm);
      this.notice = { kind: 'warning', text: 'Choose a valid next workflow status.' };
      return;
    }

    this.runAction(
      'advanceStatus',
      () => this.api.advanceStatus(this.selectedApplication!.id, this.statusForm.getRawValue() as unknown as AdvanceAgriMortgageStatusRequest),
      (application) => {
        this.upsertApplication(application);
        this.patchStatusForm(application);
        this.refreshSummaryOnly();
        this.notice = { kind: 'success', text: `Application moved to ${application.status}.` };
      },
      'Unable to advance application status'
    );
  }

  exportApplications(): void {
    this.runAction(
      'export',
      () => this.api.exportApplications(),
      (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'agri-mortgage-applications.xlsx';
        link.click();
        URL.revokeObjectURL(url);
        this.notice = { kind: 'success', text: 'Excel export download started.' };
      },
      'Unable to export agri mortgage applications'
    );
  }

  nextPage(): void {
    this.reportPage += 1;
    this.applySearch();
  }

  previousPage(): void {
    if (this.reportPage > 0) {
      this.reportPage -= 1;
      this.applySearch();
    }
  }

  allowedTransitions(application: AgriMortgageApplicationResponse | null): AgriMortgageApplicationStatus[] {
    if (!application) {
      return [];
    }
    const transitions: Record<AgriMortgageApplicationStatus, AgriMortgageApplicationStatus[]> = {
      DRAFT: ['LAND_VERIFICATION', 'REJECTED'],
      LAND_VERIFICATION: ['ENCUMBRANCE_CHECK', 'REJECTED'],
      ENCUMBRANCE_CHECK: ['CREDIT_REVIEW', 'REJECTED'],
      CREDIT_REVIEW: ['LEGAL_REVIEW', 'REJECTED'],
      LEGAL_REVIEW: ['SANCTIONED', 'REJECTED'],
      SANCTIONED: ['DISBURSED', 'REJECTED'],
      DISBURSED: ['CLOSED'],
      CLOSED: [],
      REJECTED: []
    };
    return transitions[application.status];
  }

  formatAmount(value: number | null | undefined): string {
    if (value === null || value === undefined) {
      return '0';
    }
    return value.toLocaleString('en-IN', { maximumFractionDigits: 2 });
  }

  formatLabel(value: string): string {
    return value.replace(/_/g, ' ');
  }

  badgeTone(status: string): string {
    if (status === 'REJECTED' || status === 'ENCUMBERED') {
      return 'danger';
    }
    if (status === 'GATEWAY_ERROR' || status === 'PENDING_VERIFICATION' || status === 'NOT_RUN') {
      return 'warning';
    }
    if (status === 'VERIFIED' || status === 'CLEAR' || status === 'SANCTIONED' || status === 'DISBURSED') {
      return 'success';
    }
    return 'warning';
  }

  documentReadinessTone(application: AgriMortgageApplicationResponse): string {
    return application.documentSummary.documentsComplete ? 'success' : 'warning';
  }

  documentReadinessLabel(application: AgriMortgageApplicationResponse): string {
    if (application.documentSummary.documentsComplete) {
      return 'Documents ready';
    }
    return `Documents pending (${application.documentSummary.missingRequiredDocuments.length} missing)`;
  }

  encumbranceStatusLabel(status: EncumbranceVerificationStatus): string {
    switch (status) {
      case 'NOT_RUN':
        return 'Not run';
      case 'CLEAR':
        return 'Encumbrance clear';
      case 'ENCUMBERED':
        return 'Encumbered';
      case 'PENDING_VERIFICATION':
        return 'Pending confirmation';
      case 'GATEWAY_ERROR':
        return 'Gateway fallback';
    }
  }

  encumbranceRetryLabel(application: AgriMortgageApplicationResponse): string {
    switch (application.encumbranceVerificationStatus) {
      case 'GATEWAY_ERROR':
        return 'Retry available after gateway recovery';
      case 'PENDING_VERIFICATION':
        return 'Waiting for external confirmation';
      case 'NOT_RUN':
        return 'Not run yet';
      default:
        return 'No retry needed';
    }
  }

  encumbranceRetryTone(application: AgriMortgageApplicationResponse): string {
    switch (application.encumbranceVerificationStatus) {
      case 'GATEWAY_ERROR':
      case 'PENDING_VERIFICATION':
      case 'NOT_RUN':
        return 'warning';
      default:
        return 'success';
    }
  }

  private restoreSession(): void {
    this.bootstrapping = true;
    this.api.me().subscribe({
      next: (user) => {
        this.currentUser = user;
        this.authSession.setUserInfo(user);
        this.bootstrapping = false;
        this.refreshAll(true);
      },
      error: () => {
        this.bootstrapping = false;
        this.clearSession('Stored session expired. Please sign in again.');
      }
    });
  }

  private clearSession(message: string): void {
    this.authSession.clear();
    this.currentUser = null;
    this.authResponse = null;
    this.summary = null;
    this.eligibility = null;
    this.districtSummary = [];
    this.applications = [];
    this.selectedApplication = null;
    this.actionBusy = null;
    this.pageBusy = false;
    this.notice = { kind: 'info', text: message };
  }

  private refreshSelectedApplication(applicationId: number, successMessage: string): void {
    this.api.getApplication(applicationId).subscribe({
      next: (application) => {
        this.upsertApplication(application);
        this.patchStatusForm(application);
        this.refreshSummaryOnly();
        this.notice = {
          kind: this.eligibility?.eligible ? 'success' : 'warning',
          text: successMessage
        };
      },
      error: (error) => this.handleError(error, 'Unable to refresh the selected application')
    });
  }

  private refreshSummaryOnly(): void {
    forkJoin({
      summary: this.api.summary(),
      districtSummary: this.api.districtSummary()
    }).subscribe({
      next: ({ summary, districtSummary }) => {
        this.summary = summary;
        this.districtSummary = districtSummary;
      },
      error: (error) => this.handleError(error, 'Unable to refresh dashboard summaries')
    });
  }

  private upsertApplication(application: AgriMortgageApplicationResponse): void {
    this.selectedApplication = application;
    this.applications = [application, ...this.applications.filter((item) => item.id !== application.id)];
  }

  private patchStatusForm(application: AgriMortgageApplicationResponse): void {
    const next = this.allowedTransitions(application)[0] ?? application.status;
    this.statusForm.patchValue({
      targetStatus: next,
      remarks: next === 'REJECTED' ? 'Rejected after review' : 'Proceed to next stage'
    });
  }

  private searchFilters(): {
    district?: string;
    taluka?: string;
    status?: AgriMortgageApplicationStatus | null;
    minAmount?: number;
    page?: number;
    size?: number;
  } {
    const value = this.searchForm.getRawValue() as Record<string, string | number | null>;
    return {
      district: value['district'] ? String(value['district']) : undefined,
      taluka: value['taluka'] ? String(value['taluka']) : undefined,
      status: value['status'] ? (value['status'] as AgriMortgageApplicationStatus) : undefined,
      minAmount: value['minAmount'] ? Number(value['minAmount']) : undefined,
      page: this.reportPage,
      size: value['size'] ? Number(value['size']) : 10
    };
  }

  private createCoBorrowerGroup() {
    return this.fb.group<any>({
      fullName: ['', [Validators.required, Validators.maxLength(150)]],
      aadhaar: ['', [Validators.required, Validators.minLength(12), Validators.maxLength(12)]],
      pan: ['', [Validators.required, Validators.maxLength(10)]],
      monthlyIncome: [null, [Validators.required, Validators.min(1)]],
      relationshipType: ['SPOUSE', [Validators.required]]
    });
  }

  private createLandParcelGroup() {
    return this.fb.group<any>({
      surveyNumber: ['', [Validators.required, Validators.maxLength(50)]],
      district: ['', [Validators.required, Validators.maxLength(80)]],
      taluka: ['', [Validators.required, Validators.maxLength(80)]],
      village: ['', [Validators.required, Validators.maxLength(80)]],
      areaInAcres: [null, [Validators.required, Validators.min(0.01)]],
      landType: ['IRRIGATED', [Validators.required]],
      marketValue: [null, [Validators.required, Validators.min(1)]],
      govtCircleRate: [null, [Validators.required, Validators.min(1)]],
      ownershipStatus: ['SOLE', [Validators.required]],
      encumbranceStatus: ['CLEAR', [Validators.required]],
      remarks: ['']
    });
  }

  private resetApplicationForm(): void {
    this.applicationForm.reset({
      primaryApplicantName: '',
      primaryApplicantAadhaar: '',
      primaryApplicantPan: '',
      primaryMonthlyIncome: null,
      district: '',
      taluka: '',
      village: '',
      requestedAmount: null,
      requestedTenureMonths: 36,
      purpose: ''
    });
    this.coBorrowersArray.clear();
    this.landParcelsArray.clear();
    this.coBorrowersArray.push(this.createCoBorrowerGroup());
    this.landParcelsArray.push(this.createLandParcelGroup());
  }

  private touch(control: AbstractControl): void {
    control.markAllAsTouched();
  }

  private handleError(error: unknown, fallbackText: string): void {
    const httpError = error as HttpErrorResponse;
    const apiError = httpError?.error as ApiErrorResponse | string | undefined;
    const detail = typeof apiError === 'string'
      ? apiError
      : apiError?.message || httpError?.message || fallbackText;
    if (httpError?.status === 409) {
      this.notice = {
        kind: 'warning',
        text: `Concurrent update detected (409 Conflict). ${detail} Reload the selected application and retry the operator action.`
      };
      return;
    }
    if (httpError?.status === 503 || httpError?.status === 504) {
      this.notice = {
        kind: 'warning',
        text: `${fallbackText}. ${detail} The external registry is still recovering; retry once the dependency is healthy.`
      };
      return;
    }
    this.notice = { kind: 'danger', text: `${fallbackText}. ${detail}` };
  }

  private runAction<T>(
    key: string,
    action: () => Observable<T>,
    onSuccess: (value: T) => void,
    fallbackText: string
  ): void {
    this.actionBusy = key;
    action().subscribe({
      next: (value) => {
        this.actionBusy = null;
        onSuccess(value);
      },
      error: (error) => {
        this.actionBusy = null;
        this.handleError(error, fallbackText);
      }
    });
  }
}
