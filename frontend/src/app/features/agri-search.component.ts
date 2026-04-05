import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { AgriMortgageApplicationResponse, AgriMortgageApplicationStatus } from '../agri-mortgage.models';

@Component({
  selector: 'app-agri-search',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <article class="panel animated-panel">
      <header class="panel__header">
        <h2>Search applications</h2>
        <div class="chip">{{ applications.length }} visible</div>
      </header>
      <form class="form" [formGroup]="searchForm" data-testid="agri-search-form">
        <div class="row">
          <label>
            District
            <input type="text" formControlName="district">
          </label>
          <label>
            Taluka
            <input type="text" formControlName="taluka">
          </label>
        </div>
        <div class="row">
          <label>
            Status
            <select formControlName="status">
              <option value="">Any</option>
              <option *ngFor="let status of statuses" [value]="status">{{ status }}</option>
            </select>
          </label>
          <label>
            Minimum amount
            <input type="number" formControlName="minAmount" min="1">
          </label>
        </div>
        <div class="row">
          <label>
            Page size
            <input type="number" formControlName="size" min="1">
          </label>
          <div class="inline-actions align-end">
            <button type="button" class="secondary" (click)="applySearch.emit()" data-testid="agri-apply-search">Apply</button>
            <button type="button" class="ghost" (click)="clearSearch.emit()">Clear</button>
            <button type="button" class="ghost" (click)="previousPage.emit()" [disabled]="reportPage === 0">Prev</button>
            <button type="button" class="ghost" (click)="nextPage.emit()">Next</button>
          </div>
        </div>
      </form>

      <div class="list" *ngIf="applications.length; else noApplications">
        <article
          class="list-card list-card--selectable"
          *ngFor="let application of applications"
          (click)="selectApplication.emit(application)"
          [class.is-selected]="selectedApplication?.id === application.id"
          [attr.data-testid]="'agri-application-card-' + application.id">
          <strong>{{ application.applicationNumber }} - {{ application.primaryApplicantName }}</strong>
          <span>{{ application.district }} - {{ application.taluka }} - ₹{{ formatAmount(application.requestedAmount) }}</span>
          <div class="meta-row">
            <span class="tag" [attr.data-tone]="badgeTone(application.status)">{{ formatLabel(application.status) }}</span>
            <span class="tag" [attr.data-tone]="documentReadinessTone(application)">{{ documentReadinessLabel(application) }}</span>
            <span class="tag" [attr.data-tone]="badgeTone(application.encumbranceVerificationStatus)">{{ formatLabel(application.encumbranceVerificationStatus) }}</span>
            <span class="tag" [attr.data-tone]="servicingStatusTone(application.loanAccountStatus ?? null)" *ngIf="application.loanAccountNumber">
              {{ application.loanAccountNumber }} - {{ formatLabel(application.loanAccountStatus || 'ACTIVE') }}
            </span>
          </div>
          <small>{{ application.eligibilitySummary || 'Eligibility evaluation pending.' }}</small>
        </article>
      </div>
      <ng-template #noApplications>
        <div class="empty-state">
          <strong>No applications matched the current filters</strong>
          <p>Adjust district, taluka, status, or amount filters and try again.</p>
        </div>
      </ng-template>
    </article>
  `
})
export class AgriSearchComponent {
  @Input({ required: true }) searchForm!: FormGroup;
  @Input({ required: true }) applications!: AgriMortgageApplicationResponse[];
  @Input() selectedApplication: AgriMortgageApplicationResponse | null = null;
  @Input({ required: true }) statuses!: AgriMortgageApplicationStatus[];
  @Input() reportPage = 0;

  @Output() applySearch = new EventEmitter<void>();
  @Output() clearSearch = new EventEmitter<void>();
  @Output() previousPage = new EventEmitter<void>();
  @Output() nextPage = new EventEmitter<void>();
  @Output() selectApplication = new EventEmitter<AgriMortgageApplicationResponse>();

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
}




