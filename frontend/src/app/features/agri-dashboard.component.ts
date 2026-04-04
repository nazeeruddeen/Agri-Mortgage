import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { AgriDistrictSummary, AgriMortgageDashboardResponse } from '../agri-mortgage.models';

@Component({
  selector: 'app-agri-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="hero animated-panel">
      <div class="hero__copy">
        <div class="eyebrow">Agricultural mortgage operations</div>
        <h2>Workflow snapshot</h2>
        <p>
          This view reflects live agri mortgage volume, eligibility outcomes, document readiness, and encumbrance verification state directly from the backend.
        </p>
      </div>
      <div class="hero__metrics" *ngIf="summary">
        <article class="metric">
          <span>Total applications</span>
          <strong>{{ summary.totalApplications }}</strong>
        </article>
        <article class="metric">
          <span>Eligible</span>
          <strong>{{ summary.eligibleApplications }}</strong>
        </article>
        <article class="metric">
          <span>Document ready</span>
          <strong>{{ summary.documentReadyApplications }}</strong>
        </article>
        <article class="metric">
          <span>Pending documents</span>
          <strong>{{ summary.applicationsPendingDocuments }}</strong>
        </article>
        <article class="metric">
          <span>Encumbrance clear</span>
          <strong>{{ summary.clearEncumbranceApplications }}</strong>
        </article>
        <article class="metric">
          <span>Gateway issues</span>
          <strong>{{ summary.gatewayErrorApplications }}</strong>
        </article>
        <article class="metric">
          <span>Sanctioned</span>
          <strong>{{ summary.sanctionedApplications }}</strong>
        </article>
        <article class="metric">
          <span>Total appraised value</span>
          <strong>₹{{ formatAmount(summary.totalAppraisedValue) }}</strong>
        </article>
      </div>
    </section>

    <article class="panel animated-panel">
      <header class="panel__header">
        <h2>District concentration</h2>
        <button type="button" class="ghost" (click)="exportApplications.emit()" [disabled]="actionBusy === 'export'">
          {{ actionBusy === 'export' ? 'Exporting...' : 'Export Excel' }}
        </button>
      </header>
      <div class="dashboard-kpis" *ngIf="summary">
        <article class="kpi-card">
          <span>Document backlog</span>
          <strong>{{ summary.applicationsPendingDocuments }}</strong>
          <small>Cases still waiting on required land or legal proofs.</small>
        </article>
        <article class="kpi-card">
          <span>Encumbrance pending</span>
          <strong>{{ summary.pendingEncumbranceApplications }}</strong>
          <small>Includes not-run or pending verification cases.</small>
        </article>
        <article class="kpi-card">
          <span>Encumbered cases</span>
          <strong>{{ summary.encumberedApplications }}</strong>
          <small>Applications currently blocked by encumbrance findings.</small>
        </article>
        <article class="kpi-card">
          <span>Retryable fallback</span>
          <strong>{{ summary.gatewayErrorApplications }}</strong>
          <small>Encumbrance checks that fell back after external retries.</small>
        </article>
      </div>
      <div class="list" *ngIf="districtSummary.length; else noDistrictSummary">
        <article class="list-card" *ngFor="let district of districtSummary">
          <strong>{{ district.district }}</strong>
          <span>{{ district.totalApplications }} applications - ₹{{ formatAmount(district.totalSanctionedAmount) }} sanctioned</span>
          <small>Average LTV {{ district.averageLtvRatio | number:'1.0-2' }}</small>
        </article>
      </div>
      <ng-template #noDistrictSummary>
        <div class="empty-state">
          <strong>No district concentration data yet</strong>
          <p>Create or load mortgage cases to populate the district summary view.</p>
        </div>
      </ng-template>
    </article>
  `
})
export class AgriDashboardComponent {
  @Input() summary: AgriMortgageDashboardResponse | null = null;
  @Input({ required: true }) districtSummary!: AgriDistrictSummary[];
  @Input() actionBusy: string | null = null;

  @Output() exportApplications = new EventEmitter<void>();

  formatAmount(value: number | null | undefined): string {
    if (value === null || value === undefined) {
      return '0';
    }
    return value.toLocaleString('en-IN', { maximumFractionDigits: 2 });
  }
}
