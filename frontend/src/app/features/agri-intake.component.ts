import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormArray, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { LandType, OwnershipStatus, RelationshipType, EncumbranceStatus } from '../agri-mortgage.models';

@Component({
  selector: 'app-agri-intake',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <article class="panel animated-panel">
      <header class="panel__header">
        <h2>Create agri mortgage draft</h2>
        <div class="chip">Backend-driven intake</div>
      </header>
      <form class="form" [formGroup]="applicationForm" data-testid="agri-application-form">
        <div class="row">
          <label>
            Primary applicant
            <input type="text" formControlName="primaryApplicantName" data-testid="agri-primary-applicant-name">
          </label>
          <label>
            Aadhaar
            <input type="text" formControlName="primaryApplicantAadhaar" maxlength="12" data-testid="agri-primary-applicant-aadhaar">
          </label>
        </div>
        <div class="row">
          <label>
            PAN
            <input type="text" formControlName="primaryApplicantPan" data-testid="agri-primary-applicant-pan">
          </label>
          <label>
            Monthly income
            <input type="number" formControlName="primaryMonthlyIncome" min="1" data-testid="agri-primary-income">
          </label>
        </div>
        <div class="row">
          <label>
            District
            <input type="text" formControlName="district" data-testid="agri-district">
          </label>
          <label>
            Taluka
            <input type="text" formControlName="taluka" data-testid="agri-taluka">
          </label>
        </div>
        <div class="row">
          <label>
            Village
            <input type="text" formControlName="village" data-testid="agri-village">
          </label>
          <label>
            Requested amount
            <input type="number" formControlName="requestedAmount" min="1" data-testid="agri-requested-amount">
          </label>
        </div>
        <div class="row">
          <label>
            Tenure months
            <input type="number" formControlName="requestedTenureMonths" min="1" data-testid="agri-requested-tenure">
          </label>
          <label>
            Purpose
            <textarea rows="3" formControlName="purpose" data-testid="agri-purpose"></textarea>
          </label>
        </div>

        <div class="section-subtitle">
          <span>Co-borrowers</span>
          <button type="button" class="tiny" (click)="addCoBorrower.emit()">Add co-borrower</button>
        </div>
        <div formArrayName="coBorrowers" class="stack">
          <div class="subcard" *ngFor="let control of coBorrowersArray.controls; let i = index" [formGroupName]="i">
            <div class="row">
              <label>
                Full name
                <input type="text" formControlName="fullName" data-testid="agri-coborrower-name">
              </label>
              <label>
                Aadhaar
                <input type="text" formControlName="aadhaar" maxlength="12">
              </label>
            </div>
            <div class="row">
              <label>
                PAN
                <input type="text" formControlName="pan">
              </label>
              <label>
                Monthly income
                <input type="number" formControlName="monthlyIncome" min="1">
              </label>
            </div>
            <div class="row">
              <label>
                Relationship
                <select formControlName="relationshipType">
                  <option *ngFor="let type of relationshipTypes" [value]="type">{{ type }}</option>
                </select>
              </label>
              <div class="inline-actions align-end">
                <button type="button" class="tiny danger" (click)="removeCoBorrower.emit(i)" [disabled]="coBorrowersArray.length === 1">Remove</button>
              </div>
            </div>
          </div>
        </div>

        <div class="section-subtitle">
          <span>Land parcels</span>
          <button type="button" class="tiny" (click)="addLandParcel.emit()">Add parcel</button>
        </div>
        <div formArrayName="landParcels" class="stack">
          <div class="subcard" *ngFor="let control of landParcelsArray.controls; let i = index" [formGroupName]="i">
            <div class="row">
              <label>
                Survey number
                <input type="text" formControlName="surveyNumber" data-testid="agri-land-survey-number">
              </label>
              <label>
                Area in acres
                <input type="number" formControlName="areaInAcres" min="0.01" step="0.01" data-testid="agri-land-area">
              </label>
            </div>
            <div class="row">
              <label>
                District
                <input type="text" formControlName="district" data-testid="agri-district">
              </label>
              <label>
                Taluka
                <input type="text" formControlName="taluka" data-testid="agri-taluka">
              </label>
            </div>
            <div class="row">
              <label>
                Village
                <input type="text" formControlName="village" data-testid="agri-village">
              </label>
              <label>
                Land type
                <select formControlName="landType" data-testid="agri-land-type">
                  <option *ngFor="let type of landTypes" [value]="type">{{ type }}</option>
                </select>
              </label>
            </div>
            <div class="row">
              <label>
                Market value
                <input type="number" formControlName="marketValue" min="1" data-testid="agri-land-market-value">
              </label>
              <label>
                Govt circle rate
                <input type="number" formControlName="govtCircleRate" min="1" data-testid="agri-land-circle-rate">
              </label>
            </div>
            <div class="row">
              <label>
                Ownership status
                <select formControlName="ownershipStatus" data-testid="agri-land-ownership-status">
                  <option *ngFor="let type of ownershipStatuses" [value]="type">{{ type }}</option>
                </select>
              </label>
              <label>
                Encumbrance status
                <select formControlName="encumbranceStatus" data-testid="agri-land-encumbrance-status">
                  <option *ngFor="let type of encumbranceStatuses" [value]="type">{{ type }}</option>
                </select>
              </label>
            </div>
            <div class="row">
              <label>
                Remarks
                <textarea rows="2" formControlName="remarks"></textarea>
              </label>
              <div class="inline-actions align-end">
                <button type="button" class="tiny danger" (click)="removeLandParcel.emit(i)" [disabled]="landParcelsArray.length === 1">Remove</button>
              </div>
            </div>
          </div>
        </div>

        <button type="button" class="primary" (click)="createApplication.emit()" [disabled]="actionBusy === 'createApplication'" data-testid="agri-create-application">
          {{ actionBusy === 'createApplication' ? 'Creating...' : 'Create draft application' }}
        </button>
      </form>
    </article>
  `
})
export class AgriIntakeComponent {
  @Input({ required: true }) applicationForm!: FormGroup;
  @Input({ required: true }) coBorrowersArray!: FormArray;
  @Input({ required: true }) landParcelsArray!: FormArray;
  @Input({ required: true }) relationshipTypes!: RelationshipType[];
  @Input({ required: true }) landTypes!: LandType[];
  @Input({ required: true }) ownershipStatuses!: OwnershipStatus[];
  @Input({ required: true }) encumbranceStatuses!: EncumbranceStatus[];
  @Input() actionBusy: string | null = null;

  @Output() addCoBorrower = new EventEmitter<void>();
  @Output() removeCoBorrower = new EventEmitter<number>();
  @Output() addLandParcel = new EventEmitter<void>();
  @Output() removeLandParcel = new EventEmitter<number>();
  @Output() createApplication = new EventEmitter<void>();
}




















