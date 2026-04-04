import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import {
  AdvanceAgriMortgageStatusRequest,
  AgriEligibilityResponse,
  AgriMortgageApplicationResponse,
  AgriMortgageDocumentResponse,
  AgriMortgageDocumentStatus,
  AgriMortgageDocumentType,
  AgriMortgageLoanAccountResponse,
  AgriMortgageApplicationStatus
} from '../agri-mortgage.models';

@Component({
  selector: 'app-agri-operations',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <article class="panel animated-panel">
      <header class="panel__header">
        <h2>Selected application operations</h2>
        <div class="chip" *ngIf="selectedApplication">#{{ selectedApplication.id }}</div>
      </header>

      <ng-container *ngIf="selectedApplication; else noSelection">
        <div class="selected">
          <div class="selected__header">
            <div>
              <strong>{{ selectedApplication.applicationNumber }}</strong>
              <p>{{ selectedApplication.primaryApplicantName }}</p>
            </div>
            <span>{{ selectedApplication.status }}</span>
          </div>

          <div class="detail-grid">
            <div>
              <span>Requested amount</span>
              <strong>&#8377;{{ formatAmount(selectedApplication.requestedAmount) }}</strong>
            </div>
            <div>
              <span>Total land value</span>
              <strong>&#8377;{{ formatAmount(selectedApplication.totalLandValue) }}</strong>
            </div>
            <div>
              <span>Combined income</span>
              <strong>&#8377;{{ formatAmount(selectedApplication.combinedIncome) }}</strong>
            </div>
            <div>
              <span>LTV ratio</span>
              <strong>{{ selectedApplication.ltvRatio | number:'1.2-2' }}</strong>
            </div>
            <div>
              <span>Encumbrance verification</span>
              <strong>{{ encumbranceStatusLabel(selectedApplication.encumbranceVerificationStatus) }}</strong>
            </div>
            <div>
              <span>Document readiness</span>
              <strong>{{ documentReadinessLabel(selectedApplication) }}</strong>
            </div>
            <div *ngIf="selectedApplication.loanAccountNumber">
              <span>Loan account</span>
              <strong>{{ selectedApplication.loanAccountNumber }}</strong>
            </div>
            <div *ngIf="selectedApplication.loanAccountNumber">
              <span>Outstanding principal</span>
              <strong>&#8377;{{ formatAmount(selectedApplication.outstandingPrincipal || 0) }}</strong>
            </div>
          </div>
        </div>

        <div class="workflow-alert workflow-alert--warning" *ngIf="selectedApplication.encumbranceVerificationStatus === 'GATEWAY_ERROR'">
          <strong>Retryable encumbrance fallback</strong>
          <p>{{ selectedApplication.encumbranceVerificationSummary || 'External registry checks exhausted the retry wrapper and returned a fallback result.' }}</p>
          <small>{{ encumbranceRetryLabel(selectedApplication) }}</small>
        </div>

        <div class="workflow-alert workflow-alert--info" *ngIf="!selectedApplication.documentSummary.documentsComplete">
          <strong>Documents block sanction</strong>
          <p>Missing required documents: {{ selectedApplication.documentSummary.missingRequiredDocuments.join(', ') }}</p>
          <small>Sanction is intentionally blocked until every required document is verified.</small>
        </div>

        <div class="split-grid">
          <div>
            <div class="result-box" *ngIf="eligibility">
              <strong [class.pass]="eligibility.eligible" [class.fail]="!eligibility.eligible">
                {{ eligibility.eligible ? 'Eligible' : 'Not eligible' }}
              </strong>
              <p>{{ eligibility.summary }}</p>
              <ul>
                <li *ngFor="let rule of eligibility.ruleResults">
                  <span>{{ rule.ruleCode }}</span>
                  <small>{{ rule.message }}</small>
                </li>
              </ul>
            </div>

            <button type="button" class="primary" (click)="evaluateSelected.emit()" [disabled]="actionBusy === 'evaluate'">
              {{ actionBusy === 'evaluate' ? 'Evaluating...' : 'Run eligibility evaluation' }}
            </button>
            <button type="button" class="secondary" (click)="runEncumbranceCheck.emit()" [disabled]="actionBusy === 'encumbranceCheck'">
              {{ actionBusy === 'encumbranceCheck'
                ? 'Checking...'
                : (selectedApplication.encumbranceVerificationStatus === 'GATEWAY_ERROR'
                    ? 'Retry encumbrance verification'
                    : 'Run encumbrance verification') }}
            </button>
            <small class="action-hint" [attr.data-tone]="encumbranceRetryTone(selectedApplication)">
              {{ encumbranceRetryLabel(selectedApplication) }}
            </small>
          </div>

          <form class="form" [formGroup]="statusForm">
            <label>
              Next status
              <select formControlName="targetStatus">
                <option *ngFor="let status of allowedTransitions(selectedApplication)" [value]="status">{{ status }}</option>
              </select>
            </label>
            <label>
              Remarks
              <input type="text" formControlName="remarks">
            </label>
            <button type="button" class="secondary" (click)="advanceStatus.emit()" [disabled]="actionBusy === 'advanceStatus' || !allowedTransitions(selectedApplication).length">
              {{ actionBusy === 'advanceStatus' ? 'Updating...' : 'Advance status' }}
            </button>
          </form>
        </div>

        <div class="subcard">
          <h3>Encumbrance result</h3>
          <div class="result-box">
            <strong [class.pass]="selectedApplication.encumbranceVerificationStatus === 'CLEAR'" [class.fail]="selectedApplication.encumbranceVerificationStatus !== 'CLEAR'">
              {{ encumbranceStatusLabel(selectedApplication.encumbranceVerificationStatus) }}
            </strong>
            <p>{{ selectedApplication.encumbranceVerificationSummary || 'Verification has not been executed yet.' }}</p>
            <small *ngIf="selectedApplication.encumbranceVerifiedAt">Checked at {{ selectedApplication.encumbranceVerifiedAt | date:'medium' }}</small>
          </div>
        </div>

        <div class="subcard">
          <h3>Land and legal documents</h3>
          <div class="result-box">
            <strong [class.pass]="selectedApplication.documentSummary.documentsComplete" [class.fail]="!selectedApplication.documentSummary.documentsComplete">
              {{ documentReadinessLabel(selectedApplication) }}
            </strong>
            <p>
              Verified {{ selectedApplication.documentSummary.verifiedDocuments }} of {{ selectedApplication.documentSummary.totalDocuments }} uploaded documents.
            </p>
            <small *ngIf="selectedApplication.documentSummary.missingRequiredDocuments.length">
              Missing: {{ selectedApplication.documentSummary.missingRequiredDocuments.join(', ') }}
            </small>
          </div>

          <form class="form compact-form" [formGroup]="documentForm">
            <div class="row">
              <label>
                Document type
                <select formControlName="documentType">
                  <option *ngFor="let type of documentTypes" [value]="type">{{ formatLabel(type) }}</option>
                </select>
              </label>
              <label>
                File name
                <input type="text" formControlName="fileName" placeholder="passbook.pdf">
              </label>
            </div>
            <div class="row">
              <label>
                File reference
                <input type="text" formControlName="fileReference" placeholder="docs/passbook.pdf">
              </label>
              <label>
                Remarks
                <input type="text" formControlName="remarks" placeholder="Uploaded by field officer">
              </label>
            </div>
            <button type="button" class="secondary" (click)="addDocument.emit()" [disabled]="actionBusy === 'addDocument'">
              {{ actionBusy === 'addDocument' ? 'Adding...' : 'Add document metadata' }}
            </button>
          </form>

          <div class="list list--tight" *ngIf="selectedApplication.documents.length; else noDocuments">
            <article class="list-card" *ngFor="let document of selectedApplication.documents">
              <strong>{{ formatLabel(document.documentType) }} - {{ document.fileName }}</strong>
              <span>{{ document.fileReference }}</span>
              <small>{{ document.uploadedBy }} - {{ document.uploadedAt | date:'medium' }}</small>
              <div class="inline-actions">
                <span class="chip" [attr.data-kind]="badgeTone(document.documentStatus)">{{ document.documentStatus }}</span>
                <button type="button" class="tiny" (click)="verifyDocument.emit({ document, status: 'VERIFIED' })" [disabled]="actionBusy === 'documentStatus'">Verify</button>
                <button type="button" class="tiny danger" (click)="verifyDocument.emit({ document, status: 'REJECTED' })" [disabled]="actionBusy === 'documentStatus'">Reject</button>
              </div>
            </article>
          </div>
          <ng-template #noDocuments>
            <p>No land/legal documents are attached yet.</p>
          </ng-template>
        </div>

        <div class="subcard">
          <h3>Mortgage servicing</h3>
          <ng-container *ngIf="canShowServicing(selectedApplication) && selectedLoanAccount; else noLoanAccount">
            <div class="detail-grid">
              <div>
                <span>Account number</span>
                <strong>{{ selectedLoanAccount.accountNumber }}</strong>
              </div>
              <div>
                <span>Status</span>
                <strong>{{ formatLabel(selectedLoanAccount.status) }}</strong>
              </div>
              <div>
                <span>EMI</span>
                <strong>&#8377;{{ formatAmount(selectedLoanAccount.monthlyInstallmentAmount) }}</strong>
              </div>
              <div>
                <span>Outstanding principal</span>
                <strong>&#8377;{{ formatAmount(selectedLoanAccount.outstandingPrincipal) }}</strong>
              </div>
              <div>
                <span>Next due date</span>
                <strong>{{ selectedLoanAccount.nextDueDate ? (selectedLoanAccount.nextDueDate | date:'mediumDate') : 'Closed' }}</strong>
              </div>
              <div>
                <span>Interest rate</span>
                <strong>{{ selectedLoanAccount.annualInterestRate | number:'1.2-2' }}%</strong>
              </div>
            </div>

            <form class="form compact-form" [formGroup]="repaymentForm" *ngIf="selectedLoanAccount.status === 'ACTIVE'">
              <div class="row">
                <label>
                  Repayment amount
                  <input type="number" formControlName="amount" min="0.01" step="0.01">
                </label>
                <label>
                  Payment mode
                  <select formControlName="paymentMode">
                    <option *ngFor="let mode of paymentModes" [value]="mode">{{ formatLabel(mode) }}</option>
                  </select>
                </label>
              </div>
              <div class="row">
                <label>
                  Transaction reference
                  <input type="text" formControlName="transactionReference" placeholder="UTR or teller reference">
                </label>
                <label>
                  Payment date
                  <input type="date" formControlName="paymentDate">
                </label>
              </div>
              <div class="row">
                <label>
                  Notes
                  <input type="text" formControlName="notes" placeholder="Optional servicing note">
                </label>
                <div class="inline-actions align-end">
                  <button type="button" class="secondary" (click)="recordRepayment.emit()" [disabled]="actionBusy === 'recordRepayment'">
                    {{ actionBusy === 'recordRepayment' ? 'Posting...' : 'Record repayment' }}
                  </button>
                </div>
              </div>
            </form>

            <div class="split-grid servicing-grid">
              <div class="subcard">
                <h3>Upcoming installments</h3>
                <div class="list list--tight" *ngIf="selectedLoanAccount.installments.length; else noInstallments">
                  <article class="list-card" *ngFor="let installment of selectedLoanAccount.installments | slice:0:6">
                    <strong>Installment {{ installment.installmentNumber }} - {{ installment.dueDate | date:'mediumDate' }}</strong>
                    <span>Principal &#8377;{{ formatAmount(installment.principalDue) }} | Interest &#8377;{{ formatAmount(installment.interestDue) }}</span>
                    <small>Remaining due &#8377;{{ formatAmount(installment.remainingDue) }}</small>
                    <span class="tag" [attr.data-tone]="servicingStatusTone(installment.status)">{{ formatLabel(installment.status) }}</span>
                  </article>
                </div>
              </div>

              <div class="subcard">
                <h3>Recent repayments</h3>
                <div class="list list--tight" *ngIf="selectedLoanAccount.transactions.length; else noTransactions">
                  <article class="list-card" *ngFor="let transaction of selectedLoanAccount.transactions | slice:0:5">
                    <strong>{{ transaction.transactionReference }} - &#8377;{{ formatAmount(transaction.amount) }}</strong>
                    <span>{{ formatLabel(transaction.paymentMode) }} on {{ transaction.paymentDate | date:'mediumDate' }}</span>
                    <small>
                      Principal &#8377;{{ formatAmount(transaction.appliedPrincipalAmount) }},
                      Interest &#8377;{{ formatAmount(transaction.appliedInterestAmount) }},
                      Prepayment &#8377;{{ formatAmount(transaction.prepaymentPrincipalAmount) }}
                    </small>
                  </article>
                </div>
              </div>
            </div>
          </ng-container>

          <ng-template #noLoanAccount>
            <p>Servicing activates after disbursement. Disburse the case to materialize the mortgage loan account and repayment schedule.</p>
          </ng-template>

          <ng-template #noInstallments>
            <p>No repayment schedule is available yet for this mortgage account.</p>
          </ng-template>

          <ng-template #noTransactions>
            <p>No repayments have been posted against this mortgage account yet.</p>
          </ng-template>
        </div>

        <div class="subcard">
          <h3>Land parcels</h3>
          <div class="list list--tight">
            <article class="list-card" *ngFor="let parcel of selectedApplication.landParcels">
              <strong>{{ parcel.surveyNumber }} - {{ parcel.village }}</strong>
              <span>{{ parcel.landType }} - &#8377;{{ formatAmount(parcel.appraisalValue || parcel.marketValue) }}</span>
              <small>{{ parcel.ownershipStatus }} - {{ parcel.encumbranceStatus }}</small>
              <small *ngIf="parcel.encumbranceCheckDetails">{{ parcel.encumbranceCheckDetails }}</small>
            </article>
          </div>
        </div>

        <div class="history">
          <h3>Workflow history</h3>
          <article *ngFor="let item of selectedApplication.stateHistory">
            <strong>{{ item.fromStatus || 'START' }} -> {{ item.toStatus }}</strong>
            <span>{{ item.remarks || 'Status updated' }}</span>
            <small>{{ item.changedBy }} - {{ item.changedAt | date:'medium' }}</small>
          </article>
        </div>
      </ng-container>

      <ng-template #noSelection>
        <p>Select an application to evaluate eligibility and advance the mortgage workflow.</p>
      </ng-template>
    </article>
  `
})
export class AgriOperationsComponent {
  @Input() eligibility: AgriEligibilityResponse | null = null;
  @Input() selectedApplication: AgriMortgageApplicationResponse | null = null;
  @Input() selectedLoanAccount: AgriMortgageLoanAccountResponse | null = null;
  @Input({ required: true }) statusForm!: FormGroup;
  @Input({ required: true }) documentForm!: FormGroup;
  @Input({ required: true }) repaymentForm!: FormGroup;
  @Input({ required: true }) documentTypes!: AgriMortgageDocumentType[];
  @Input({ required: true }) paymentModes!: string[];
  @Input({ required: true }) allowedTransitions!: (application: AgriMortgageApplicationResponse | null) => AgriMortgageApplicationStatus[];
  @Input({ required: true }) formatAmount!: (value: number | null | undefined) => string;
  @Input({ required: true }) formatLabel!: (value: string) => string;
  @Input({ required: true }) servicingStatusTone!: (status: string | null | undefined) => string;
  @Input({ required: true }) badgeTone!: (status: string) => string;
  @Input({ required: true }) documentReadinessLabel!: (application: AgriMortgageApplicationResponse) => string;
  @Input({ required: true }) canShowServicing!: (application: AgriMortgageApplicationResponse | null) => boolean;
  @Input({ required: true }) encumbranceStatusLabel!: (status: AgriMortgageApplicationResponse['encumbranceVerificationStatus']) => string;
  @Input({ required: true }) encumbranceRetryLabel!: (application: AgriMortgageApplicationResponse) => string;
  @Input({ required: true }) encumbranceRetryTone!: (application: AgriMortgageApplicationResponse) => string;
  @Input() actionBusy: string | null = null;

  @Output() evaluateSelected = new EventEmitter<void>();
  @Output() runEncumbranceCheck = new EventEmitter<void>();
  @Output() advanceStatus = new EventEmitter<void>();
  @Output() addDocument = new EventEmitter<void>();
  @Output() verifyDocument = new EventEmitter<{ document: AgriMortgageDocumentResponse; status: AgriMortgageDocumentStatus }>();
  @Output() recordRepayment = new EventEmitter<void>();
}
