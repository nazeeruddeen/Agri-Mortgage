import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { AbstractControl, FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, forkJoin, of } from 'rxjs';
import {
  buildAgriApplicationForm,
  buildAgriAuthForm,
  buildAgriDocumentForm,
  buildAgriRepaymentForm,
  buildAgriSearchForm,
  buildAgriStatusForm,
  createAgriCoBorrowerGroup,
  createAgriLandParcelGroup
} from './agri-workspace.forms';
import { AgriDashboardComponent } from './features/agri-dashboard.component';
import { AgriIntakeComponent } from './features/agri-intake.component';
import { AgriOperationsComponent } from './features/agri-operations.component';
import { AgriSearchComponent } from './features/agri-search.component';
import { AgriMortgageApiService } from './agri-mortgage-api.service';
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
  RecordAgriRepaymentRequest,
  RelationshipType,
  UpdateAgriMortgageDocumentStatusRequest,
  UserInfoResponse
} from './agri-mortgage.models';
import { AuthSessionService } from './auth-session.service';

type NoticeKind = 'info' | 'success' | 'warning' | 'danger';
type AppTab = 'dashboard' | 'intake' | 'search' | 'operations';

@Component({
  selector: 'app-agri-mortgage-workspace',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, AgriDashboardComponent, AgriIntakeComponent, AgriOperationsComponent, AgriSearchComponent],
  templateUrl: './workspace.component.html',
  styleUrl: './workspace.component.scss'
})
export class AgriMortgageWorkspaceComponent implements OnInit {
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
  readonly paymentModes = ['UPI', 'NEFT', 'IMPS', 'AUTO_DEBIT', 'CHEQUE', 'CASH'];

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
  loanAccounts: AgriMortgageLoanAccountResponse[] = [];
  selectedLoanAccount: AgriMortgageLoanAccountResponse | null = null;
  private routeSelectedApplicationId: number | null = null;
  private refreshSuccessNotice: { kind: NoticeKind; text: string } | null = null;
  reportPage = 0;

  authForm = buildAgriAuthForm(this.fb);
  applicationForm = buildAgriApplicationForm(this.fb);
  searchForm = buildAgriSearchForm(this.fb);
  statusForm = buildAgriStatusForm(this.fb);
  documentForm = buildAgriDocumentForm(this.fb);
  repaymentForm = buildAgriRepaymentForm(this.fb, this.today());

  constructor(
    private readonly fb: FormBuilder,
    private readonly api: AgriMortgageApiService,
    private readonly authSession: AuthSessionService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.route.data.subscribe((data) => {
      this.activeTab = (data['tab'] as AppTab | undefined) ?? 'dashboard';
    });
    this.route.queryParamMap.subscribe((params) => {
      const selectedApplicationId = Number(params.get('selectedApplicationId'));
      this.routeSelectedApplicationId = Number.isFinite(selectedApplicationId) && selectedApplicationId > 0
        ? selectedApplicationId
        : null;
    });
    this.restoreSession();
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
    const path: Record<AppTab, string> = {
      dashboard: '/overview',
      intake: '/intake',
      search: '/applications',
      operations: '/operations'
    };
    void this.router.navigate([path[tab]], { queryParams: this.selectionQueryParams() });
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
    this.actionBusy = 'logout';
    const request = this.authSession.isAuthenticated ? this.api.logout() : of(void 0);
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
      search: this.api.search(this.searchFilters()),
      loanAccounts: this.api.listLoanAccounts({ page: 0, size: 10 })
    }).subscribe({
      next: ({ me, summary, districtSummary, search, loanAccounts }) => {
        this.currentUser = me;
        this.authSession.setUserInfo(me);
        this.summary = summary;
        this.districtSummary = districtSummary;
        this.applications = search.content;
        this.loanAccounts = loanAccounts.content;
        this.selectedApplication = this.routeSelectedApplicationId
          ? search.content.find((item) => item.id === this.routeSelectedApplicationId) ?? search.content[0] ?? null
          : resetSelection
            ? search.content[0] ?? null
            : search.content.find((item) => item.id === this.selectedApplication?.id) ?? search.content[0] ?? null;
        if (this.selectedApplication) {
          this.patchStatusForm(this.selectedApplication);
          this.syncSelectedLoanAccount(this.selectedApplication);
        } else if (this.routeSelectedApplicationId) {
          this.api.getApplication(this.routeSelectedApplicationId).subscribe({
            next: (application) => this.selectApplication(application),
            error: () => {
              this.routeSelectedApplicationId = null;
              this.syncSelectionQueryParams();
            }
          });
        } else {
          this.selectedLoanAccount = null;
        }
        this.pageBusy = false;
        this.notice = this.refreshSuccessNotice ?? { kind: 'success', text: 'Agri mortgage data refreshed successfully.' };
        this.refreshSuccessNotice = null;
      },
      error: (error) => {
        this.pageBusy = false;
        this.refreshSuccessNotice = null;
        this.handleError(error, 'Unable to refresh the agri mortgage workspace');
      }
    });
  }

  addCoBorrower(): void {
    this.coBorrowersArray.push(createAgriCoBorrowerGroup(this.fb));
  }

  removeCoBorrower(index: number): void {
    if (this.coBorrowersArray.length > 1) {
      this.coBorrowersArray.removeAt(index);
    }
  }

  addLandParcel(): void {
    this.landParcelsArray.push(createAgriLandParcelGroup(this.fb));
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
        this.routeSelectedApplicationId = application.id;
        this.setTab('search');
        this.refreshSuccessNotice = { kind: 'success', text: `Draft ${application.applicationNumber} created successfully.` };
        this.refreshAll(true);
        this.selectedApplication = application;
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
        this.selectedApplication = page.content.find((item) => item.id === (this.routeSelectedApplicationId ?? this.selectedApplication?.id))
          ?? page.content[0]
          ?? null;
        if (this.selectedApplication) {
          this.patchStatusForm(this.selectedApplication);
          this.syncSelectedLoanAccount(this.selectedApplication);
        } else if (this.routeSelectedApplicationId) {
          this.api.getApplication(this.routeSelectedApplicationId).subscribe({
            next: (application) => this.selectApplication(application),
            error: () => {
              this.routeSelectedApplicationId = null;
              this.syncSelectionQueryParams();
              this.selectedLoanAccount = null;
            }
          });
        } else {
          this.selectedLoanAccount = null;
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
    this.routeSelectedApplicationId = application.id;
    this.syncSelectionQueryParams();
    this.patchStatusForm(application);
    this.syncSelectedLoanAccount(application);
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

  recordRepayment(): void {
    if (!this.selectedLoanAccount) {
      this.notice = { kind: 'warning', text: 'Select a disbursed mortgage application with a servicing account first.' };
      return;
    }
    if (this.repaymentForm.invalid) {
      this.touch(this.repaymentForm);
      this.notice = { kind: 'warning', text: 'Complete the repayment form before posting the servicing transaction.' };
      return;
    }

    const payload = this.repaymentForm.getRawValue() as unknown as RecordAgriRepaymentRequest;
    this.runAction(
      'recordRepayment',
      () => this.api.recordRepayment(this.selectedLoanAccount!.id, payload),
      (transaction) => {
        const accountNumber = this.selectedLoanAccount?.accountNumber;
        this.repaymentForm.patchValue({
          amount: null,
          paymentMode: 'UPI',
          transactionReference: '',
          paymentDate: this.today(),
          notes: ''
        });
        if (this.selectedApplication) {
          this.syncSelectedLoanAccount(this.selectedApplication);
          this.refreshSelectedApplication(this.selectedApplication.id, `Repayment ${transaction.transactionReference} recorded successfully.`);
        }
        this.refreshLoanAccounts();
        this.notice = {
          kind: 'success',
          text: `Repayment ${transaction.transactionReference} posted against ${accountNumber ?? 'the servicing account'}.`
        };
      },
      'Unable to record agri mortgage repayment'
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

  servicingStatusTone(status: string | null | undefined): string {
    if (!status) {
      return 'warning';
    }
    if (status === 'CLOSED' || status === 'PAID') {
      return 'success';
    }
    if (status === 'OVERDUE' || status === 'PARTIAL') {
      return 'warning';
    }
    return 'warning';
  }

  canShowServicing(application: AgriMortgageApplicationResponse | null): boolean {
    return !!application && (application.status === 'DISBURSED' || application.status === 'CLOSED' || !!application.loanAccountNumber);
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
        this.clearSession('Sign in to load the live agri mortgage workspace.');
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
    this.loanAccounts = [];
    this.selectedApplication = null;
    this.selectedLoanAccount = null;
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
    this.routeSelectedApplicationId = application.id;
    this.applications = [application, ...this.applications.filter((item) => item.id !== application.id)];
    this.syncSelectedLoanAccount(application);
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
    this.coBorrowersArray.push(createAgriCoBorrowerGroup(this.fb));
    this.landParcelsArray.push(createAgriLandParcelGroup(this.fb));
  }

  private refreshLoanAccounts(): void {
    this.api.listLoanAccounts({ page: 0, size: 10 }).subscribe({
      next: (page) => {
        this.loanAccounts = page.content;
      },
      error: (error) => this.handleError(error, 'Unable to refresh mortgage servicing accounts')
    });
  }

  private syncSelectedLoanAccount(application: AgriMortgageApplicationResponse | null): void {
    if (!this.canShowServicing(application) || !application) {
      this.selectedLoanAccount = null;
      return;
    }

    this.api.getLoanAccount(application.id).subscribe({
      next: (account) => {
        this.selectedLoanAccount = account;
        this.loanAccounts = [account, ...this.loanAccounts.filter((item) => item.id !== account.id)].slice(0, 10);
      },
      error: (error) => {
        this.selectedLoanAccount = null;
        this.handleError(error, 'Unable to load the mortgage servicing account');
      }
    });
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private syncSelectionQueryParams(): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: this.selectionQueryParams(),
      replaceUrl: true
    });
  }

  private selectionQueryParams(): Record<string, number> | {} {
    return this.routeSelectedApplicationId ? { selectedApplicationId: this.routeSelectedApplicationId } : {};
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

