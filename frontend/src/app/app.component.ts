import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { AbstractControl, FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

type ApplicationStatus =
  | 'DRAFT'
  | 'LAND_VERIFICATION'
  | 'ENCUMBRANCE_CHECK'
  | 'CREDIT_REVIEW'
  | 'LEGAL_REVIEW'
  | 'SANCTIONED'
  | 'DISBURSED'
  | 'CLOSED'
  | 'REJECTED';

type RelationshipType = 'SPOUSE' | 'PARENT' | 'CHILD' | 'SIBLING' | 'BUSINESS_PARTNER' | 'OTHER';
type LandType = 'IRRIGATED' | 'DRY' | 'HORTICULTURE' | 'FOREST_ADJACENT';
type OwnershipStatus = 'SOLE' | 'JOINT' | 'DISPUTED';
type EncumbranceStatus = 'CLEAR' | 'ENCUMBERED' | 'PENDING';

interface CoBorrower {
  fullName: string;
  aadhaar: string;
  pan: string;
  monthlyIncome: number;
  relationshipType: RelationshipType;
}

interface LandParcel {
  surveyNumber: string;
  district: string;
  taluka: string;
  village: string;
  areaInAcres: number;
  landType: LandType;
  marketValue: number;
  govtCircleRate: number;
  ownershipStatus: OwnershipStatus;
  encumbranceStatus: EncumbranceStatus;
  remarks?: string;
}

interface ApplicationHistoryItem {
  fromStatus: ApplicationStatus | null;
  toStatus: ApplicationStatus;
  remarks: string;
  changedAt: string;
}

interface AgriApplication {
  id: number;
  applicationNumber: string;
  primaryApplicantName: string;
  primaryApplicantAadhaar: string;
  primaryApplicantPan: string;
  primaryMonthlyIncome: number;
  district: string;
  taluka: string;
  village: string;
  requestedAmount: number;
  requestedTenureMonths: number;
  purpose: string;
  status: ApplicationStatus;
  eligible: boolean;
  eligibilitySummary: string;
  totalLandValue: number;
  combinedIncome: number;
  ltvRatio: number;
  coBorrowers: CoBorrower[];
  landParcels: LandParcel[];
  history: ApplicationHistoryItem[];
}

interface SummaryCard {
  label: string;
  value: string | number;
  accent: string;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  readonly title = 'Agri Mortgage Loan System';
  readonly subtitle = 'Interactive agricultural mortgage workflow shell';
  readonly apiBaseUrl = 'http://localhost:8011/api/v1/agri-mortgage-applications';

  readonly statuses: ApplicationStatus[] = [
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

  readonly statusTransitions: Record<ApplicationStatus, ApplicationStatus[]> = {
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

  readonly relationshipTypes: RelationshipType[] = ['SPOUSE', 'PARENT', 'CHILD', 'SIBLING', 'BUSINESS_PARTNER', 'OTHER'];
  readonly landTypes: LandType[] = ['IRRIGATED', 'DRY', 'HORTICULTURE', 'FOREST_ADJACENT'];
  readonly ownershipStatuses: OwnershipStatus[] = ['SOLE', 'JOINT', 'DISPUTED'];
  readonly encumbranceStatuses: EncumbranceStatus[] = ['CLEAR', 'ENCUMBERED', 'PENDING'];

  notice = 'Ready to create and manage agri mortgage applications';
  selectedApplication: AgriApplication | null = null;
  visibleApplications: AgriApplication[] = [];
  applications: AgriApplication[] = [];
  summaryCards: SummaryCard[] = [];
  totalRequestedAmount = 0;
  totalAppraisedValue = 0;

  activeTab: string = 'dashboard';

  setTab(tab: string): void {
    this.activeTab = tab;
  }

  applicationForm = this.fb.group({
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

  searchForm = this.fb.group({
    district: [''],
    taluka: [''],
    status: [''],
    minAmount: [null]
  });

  statusForm = this.fb.group({
    targetStatus: ['LAND_VERIFICATION' as ApplicationStatus, [Validators.required]],
    remarks: ['Ready for next stage', [Validators.maxLength(500)]]
  });

  readonly workflowStages = [
    'Borrower onboarding',
    'Land parcel capture',
    'Ownership and encumbrance review',
    'Eligibility evaluation',
    'Approval state tracking',
    'Summary reporting'
  ];

  constructor(private readonly fb: FormBuilder) {
    this.rebuildDerivedState();
    if (this.applications.length > 0) {
      this.selectApplication(this.applications[0]);
    }
  }

  get coBorrowersArray(): FormArray {
    return this.applicationForm.get('coBorrowers') as FormArray;
  }

  get landParcelsArray(): FormArray {
    return this.applicationForm.get('landParcels') as FormArray;
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
      this.notice = 'Please complete the borrower details before creating the application';
      return;
    }

    const form = this.applicationForm.getRawValue();
    const coBorrowers = this.coBorrowersArray.controls.map((control) => control.getRawValue() as CoBorrower);
    const landParcels = this.landParcelsArray.controls.map((control) => control.getRawValue() as LandParcel);

    const application: AgriApplication = {
      id: this.nextId(),
      applicationNumber: `AGRI-${String(this.applications.length + 1).padStart(4, '0')}`,
      primaryApplicantName: form.primaryApplicantName?.trim() ?? '',
      primaryApplicantAadhaar: form.primaryApplicantAadhaar?.trim() ?? '',
      primaryApplicantPan: form.primaryApplicantPan?.trim().toUpperCase() ?? '',
      primaryMonthlyIncome: Number(form.primaryMonthlyIncome ?? 0),
      district: form.district?.trim() ?? '',
      taluka: form.taluka?.trim() ?? '',
      village: form.village?.trim() ?? '',
      requestedAmount: Number(form.requestedAmount ?? 0),
      requestedTenureMonths: Number(form.requestedTenureMonths ?? 0),
      purpose: form.purpose?.trim() ?? '',
      status: 'DRAFT',
      eligible: false,
      eligibilitySummary: 'Draft created. Run evaluation to calculate score.',
      totalLandValue: 0,
      combinedIncome: Number(form.primaryMonthlyIncome ?? 0),
      ltvRatio: 0,
      coBorrowers,
      landParcels,
      history: [
        {
          fromStatus: null,
          toStatus: 'DRAFT',
          remarks: 'Draft created',
          changedAt: new Date().toISOString()
        }
      ]
    };

    this.applications = [application, ...this.applications];
    this.selectApplication(application);
    this.rebuildDerivedState();
    this.notice = `Created application ${application.applicationNumber}`;
    this.resetApplicationForm();
  }

  evaluateSelected(): void {
    if (!this.selectedApplication) {
      this.notice = 'Select an application first';
      return;
    }

    const app = this.selectedApplication;
    const totalLandValue = app.landParcels.reduce((sum, parcel) => sum + Math.min(parcel.marketValue, parcel.govtCircleRate), 0);
    const combinedIncome = app.primaryMonthlyIncome + app.coBorrowers.reduce((sum, borrower) => sum + Number(borrower.monthlyIncome || 0), 0);
    const ltvRatio = totalLandValue > 0 ? app.requestedAmount / totalLandValue : 0;
    const hasClearTitle = app.landParcels.every((parcel) => parcel.ownershipStatus !== 'DISPUTED' && parcel.encumbranceStatus === 'CLEAR');
    const eligible = hasClearTitle && totalLandValue > 0 && app.requestedAmount <= totalLandValue * 0.75 && combinedIncome >= app.requestedAmount / 24;

    this.updateApplication(app.id, {
      eligible,
      totalLandValue,
      combinedIncome,
      ltvRatio,
      eligibilitySummary: eligible
        ? 'Eligible based on land coverage, ownership status, and income.'
        : 'Not eligible because one or more policy checks failed.',
      historyEntry: {
        fromStatus: app.status,
        toStatus: app.status,
        remarks: eligible ? 'Eligibility approved' : 'Eligibility failed',
        changedAt: new Date().toISOString()
      }
    });

    this.notice = eligible ? 'Eligibility approved' : 'Eligibility not approved';
  }

  advanceStatus(): void {
    if (!this.selectedApplication) {
      this.notice = 'Select an application first';
      return;
    }

    const target = this.statusForm.get('targetStatus')?.value as ApplicationStatus;
    const remarks = this.statusForm.get('remarks')?.value?.trim() || 'Status updated';
    const current = this.selectedApplication.status;

    if (!this.statusTransitions[current].includes(target)) {
      this.notice = `Invalid transition from ${current} to ${target}`;
      return;
    }

    this.updateApplication(this.selectedApplication.id, {
      status: target,
      historyEntry: {
        fromStatus: current,
        toStatus: target,
        remarks,
        changedAt: new Date().toISOString()
      }
    });

    this.statusForm.patchValue({
      targetStatus: this.statusTransitions[target][0] ?? target,
      remarks: 'Ready for next stage'
    });
    this.notice = `Application moved to ${target}`;
  }

  applySearch(): void {
    const filters = this.searchForm.getRawValue();
    this.visibleApplications = this.applications.filter((application) => {
      const districtMatch = !filters.district || application.district.toLowerCase().includes(filters.district.toLowerCase());
      const talukaMatch = !filters.taluka || application.taluka.toLowerCase().includes(filters.taluka.toLowerCase());
      const statusMatch = !filters.status || application.status === filters.status;
      const amountMatch = !filters.minAmount || application.requestedAmount >= Number(filters.minAmount);
      return districtMatch && talukaMatch && statusMatch && amountMatch;
    });
    this.notice = `Showing ${this.visibleApplications.length} matching application(s)`;
  }

  clearSearch(): void {
    this.searchForm.reset({
      district: '',
      taluka: '',
      status: '',
      minAmount: null
    });
    this.visibleApplications = [...this.applications];
    this.notice = 'Search cleared';
  }

  selectApplication(application: AgriApplication): void {
    this.selectedApplication = application;
    this.statusForm.patchValue({
      targetStatus: this.statusTransitions[application.status][0] ?? application.status,
      remarks: application.status === 'DRAFT' ? 'Move to land verification' : 'Proceed to next stage'
    });
  }

  refreshSummary(): void {
    this.rebuildDerivedState();
    this.notice = 'Dashboard refreshed';
  }

  formatAmount(value: number): string {
    return value.toLocaleString('en-IN', { maximumFractionDigits: 2 });
  }


  eligibilityScore(application: AgriApplication): string {
    if (!application.totalLandValue) {
      return '0%';
    }
    const score = Math.max(0, Math.min(100, Math.round((1 - application.ltvRatio) * 100)));
    return `${score}%`;
  }

  private rebuildDerivedState(): void {
    this.visibleApplications = [...this.applications];
    this.totalRequestedAmount = this.applications.reduce((sum, application) => sum + application.requestedAmount, 0);
    this.totalAppraisedValue = this.applications.reduce((sum, application) => {
      return sum + application.landParcels.reduce((parcelSum, parcel) => parcelSum + Math.min(parcel.marketValue, parcel.govtCircleRate), 0);
    }, 0);

    const statusCounts = this.statuses.reduce((acc, status) => {
      acc[status] = this.applications.filter((application) => application.status === status).length;
      return acc;
    }, {} as Record<ApplicationStatus, number>);

    this.summaryCards = [
      { label: 'Total applications', value: this.applications.length, accent: 'accent-2' },
      { label: 'Eligible cases', value: this.applications.filter((application) => application.eligible).length, accent: 'accent' },
      { label: 'Draft', value: statusCounts.DRAFT, accent: 'warning' },
      { label: 'Sanctioned', value: statusCounts.SANCTIONED, accent: 'accent-2' },
      { label: 'Disbursed', value: statusCounts.DISBURSED, accent: 'accent' },
      { label: 'Rejected', value: statusCounts.REJECTED, accent: 'warning' }
    ];
  }

  private updateApplication(
    id: number,
    changes: Partial<Pick<AgriApplication, 'status' | 'eligible' | 'totalLandValue' | 'combinedIncome' | 'ltvRatio' | 'eligibilitySummary'>> & {
      historyEntry: ApplicationHistoryItem;
    }
  ): void {
    this.applications = this.applications.map((application) => {
      if (application.id !== id) {
        return application;
      }
      return {
        ...application,
        ...(changes.status !== undefined ? { status: changes.status } : {}),
        ...(changes.eligible !== undefined ? { eligible: changes.eligible } : {}),
        ...(changes.totalLandValue !== undefined ? { totalLandValue: changes.totalLandValue } : {}),
        ...(changes.combinedIncome !== undefined ? { combinedIncome: changes.combinedIncome } : {}),
        ...(changes.ltvRatio !== undefined ? { ltvRatio: changes.ltvRatio } : {}),
        ...(changes.eligibilitySummary !== undefined ? { eligibilitySummary: changes.eligibilitySummary } : {}),
        history: [changes.historyEntry, ...application.history]
      };
    });

    const nextSelected = this.applications.find((application) => application.id === id) ?? null;
    this.selectedApplication = nextSelected;
    if (nextSelected) {
      this.visibleApplications = this.visibleApplications.map((application) =>
        application.id === nextSelected.id ? nextSelected : application
      );
    }
    this.rebuildDerivedState();
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

  private createCoBorrowerGroup() {
    return this.fb.group({
      fullName: ['', [Validators.required, Validators.maxLength(150)]],
      aadhaar: ['', [Validators.required, Validators.minLength(12), Validators.maxLength(12)]],
      pan: ['', [Validators.required, Validators.maxLength(10)]],
      monthlyIncome: [null, [Validators.required, Validators.min(1)]],
      relationshipType: ['SPOUSE' as RelationshipType, [Validators.required]]
    });
  }

  private createLandParcelGroup() {
    return this.fb.group({
      surveyNumber: ['', [Validators.required, Validators.maxLength(50)]],
      district: ['', [Validators.required, Validators.maxLength(80)]],
      taluka: ['', [Validators.required, Validators.maxLength(80)]],
      village: ['', [Validators.required, Validators.maxLength(80)]],
      areaInAcres: [null, [Validators.required, Validators.min(0.01)]],
      landType: ['IRRIGATED' as LandType, [Validators.required]],
      marketValue: [null, [Validators.required, Validators.min(1)]],
      govtCircleRate: [null, [Validators.required, Validators.min(1)]],
      ownershipStatus: ['SOLE' as OwnershipStatus, [Validators.required]],
      encumbranceStatus: ['CLEAR' as EncumbranceStatus, [Validators.required]],
      remarks: ['']
    });
  }

  private nextId(): number {
    return Math.max(...this.applications.map((application) => application.id), 0) + 1;
  }

  private touch(control: AbstractControl): void {
    control.markAllAsTouched();
  }
}
