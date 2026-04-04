import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, FormGroup } from '@angular/forms';
import { AgriOperationsComponent } from './agri-operations.component';

describe('AgriOperationsComponent', () => {
  let fixture: ComponentFixture<AgriOperationsComponent>;
  let component: AgriOperationsComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AgriOperationsComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(AgriOperationsComponent);
    component = fixture.componentInstance;
    component.statusForm = new FormGroup({
      targetStatus: new FormControl('LEGAL_REVIEW'),
      remarks: new FormControl('Proceed')
    });
    component.documentForm = new FormGroup({
      documentType: new FormControl('PATTADAR_PASSBOOK'),
      fileName: new FormControl('passbook.pdf'),
      fileReference: new FormControl('docs/passbook.pdf'),
      remarks: new FormControl('')
    });
    component.repaymentForm = new FormGroup({
      amount: new FormControl(15000),
      paymentMode: new FormControl('UPI'),
      transactionReference: new FormControl('AGR-TXN-1'),
      paymentDate: new FormControl('2026-04-04'),
      notes: new FormControl('')
    });
    component.documentTypes = ['PATTADAR_PASSBOOK', 'OWNERSHIP_PROOF'] as any;
    component.paymentModes = ['UPI', 'NEFT'];
    component.allowedTransitions = () => ['LEGAL_REVIEW', 'REJECTED'] as any;
    component.formatAmount = (value) => `${value ?? 0}`;
    component.formatLabel = (value) => value.replace(/_/g, ' ');
    component.servicingStatusTone = () => 'warning';
    component.badgeTone = () => 'warning';
    component.documentReadinessLabel = () => 'Documents pending (1 missing)';
    component.canShowServicing = () => true;
    component.encumbranceStatusLabel = () => 'Gateway fallback';
    component.encumbranceRetryLabel = () => 'Retry available after gateway recovery';
    component.encumbranceRetryTone = () => 'warning';
    component.selectedApplication = {
      id: 9,
      applicationNumber: 'AG-1009',
      primaryApplicantName: 'Ramesh Patel',
      status: 'ENCUMBRANCE_CHECK',
      requestedAmount: 600000,
      totalLandValue: 900000,
      combinedIncome: 70000,
      ltvRatio: 0.67,
      encumbranceVerificationStatus: 'GATEWAY_ERROR',
      encumbranceVerificationSummary: 'Registry timeout after retries',
      documentSummary: {
        documentsComplete: false,
        verifiedDocuments: 1,
        totalDocuments: 2,
        missingRequiredDocuments: ['LAND_VALUATION_REPORT']
      },
      documents: [
        {
          id: 1,
          documentType: 'PATTADAR_PASSBOOK',
          fileName: 'passbook.pdf',
          fileReference: 'docs/passbook.pdf',
          uploadedBy: 'operator',
          uploadedAt: '2026-04-04T09:00:00',
          documentStatus: 'PENDING'
        }
      ],
      landParcels: [],
      stateHistory: [],
      loanAccountNumber: 'AGL-1',
      outstandingPrincipal: 450000
    } as any;
    component.selectedLoanAccount = {
      accountNumber: 'AGL-1',
      status: 'ACTIVE',
      monthlyInstallmentAmount: 12000,
      outstandingPrincipal: 450000,
      nextDueDate: '2026-05-01',
      annualInterestRate: 9.5,
      installments: [],
      transactions: []
    } as any;
    fixture.detectChanges();
  });

  it('renders the retryable encumbrance fallback state', () => {
    expect(fixture.nativeElement.textContent).toContain('Retryable encumbrance fallback');
    expect(fixture.nativeElement.textContent).toContain('Registry timeout after retries');
  });

  it('emits verifyDocument with the selected status', () => {
    spyOn(component.verifyDocument, 'emit');

    const verifyButton = Array.from(fixture.nativeElement.querySelectorAll('button'))
      .find((element) => (element as HTMLButtonElement).textContent?.includes('Verify')) as HTMLButtonElement;

    verifyButton.click();

    expect(component.verifyDocument.emit).toHaveBeenCalledWith({
      document: jasmine.objectContaining({ id: 1 }),
      status: 'VERIFIED'
    });
  });
});
