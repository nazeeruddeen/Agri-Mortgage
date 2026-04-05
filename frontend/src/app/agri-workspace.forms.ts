import { FormBuilder, Validators } from '@angular/forms';

export function createAgriCoBorrowerGroup(fb: FormBuilder) {
  return fb.group<any>({
    fullName: ['', [Validators.required, Validators.maxLength(150)]],
    aadhaar: ['', [Validators.required, Validators.minLength(12), Validators.maxLength(12)]],
    pan: ['', [Validators.required, Validators.maxLength(10)]],
    monthlyIncome: [null, [Validators.required, Validators.min(1)]],
    relationshipType: ['SPOUSE', [Validators.required]]
  });
}

export function createAgriLandParcelGroup(fb: FormBuilder) {
  return fb.group<any>({
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

export function buildAgriAuthForm(fb: FormBuilder) {
  return fb.group<any>({
    username: ['', [Validators.required]],
    password: ['', [Validators.required]]
  });
}

export function buildAgriApplicationForm(fb: FormBuilder) {
  return fb.group<any>({
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
    coBorrowers: fb.array([createAgriCoBorrowerGroup(fb)]),
    landParcels: fb.array([createAgriLandParcelGroup(fb)])
  });
}

export function buildAgriSearchForm(fb: FormBuilder) {
  return fb.group<any>({
    district: [''],
    taluka: [''],
    status: [''],
    minAmount: [null],
    size: [10]
  });
}

export function buildAgriStatusForm(fb: FormBuilder) {
  return fb.group<any>({
    targetStatus: ['LAND_VERIFICATION', [Validators.required]],
    remarks: ['Proceed to next stage', [Validators.maxLength(500)]]
  });
}

export function buildAgriDocumentForm(fb: FormBuilder) {
  return fb.group<any>({
    documentType: ['PATTADAR_PASSBOOK', [Validators.required]],
    fileName: ['', [Validators.required, Validators.maxLength(180)]],
    fileReference: ['', [Validators.required, Validators.maxLength(255)]],
    remarks: ['']
  });
}

export function buildAgriRepaymentForm(fb: FormBuilder, today: string) {
  return fb.group<any>({
    amount: [null, [Validators.required, Validators.min(0.01)]],
    paymentMode: ['UPI', [Validators.required]],
    transactionReference: ['', [Validators.required, Validators.maxLength(120)]],
    paymentDate: [today, [Validators.required]],
    notes: ['', [Validators.maxLength(500)]]
  });
}
