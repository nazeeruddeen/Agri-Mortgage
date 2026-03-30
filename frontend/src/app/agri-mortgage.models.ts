export type UserRole = 'ADMIN' | 'LOAN_OFFICER' | 'REVIEWER' | 'BORROWER';
export type AgriMortgageApplicationStatus =
  | 'DRAFT'
  | 'LAND_VERIFICATION'
  | 'ENCUMBRANCE_CHECK'
  | 'CREDIT_REVIEW'
  | 'LEGAL_REVIEW'
  | 'SANCTIONED'
  | 'DISBURSED'
  | 'CLOSED'
  | 'REJECTED';
export type ApplicantType = 'PRIMARY' | 'CO_BORROWER';
export type RelationshipType = 'SPOUSE' | 'PARENT' | 'CHILD' | 'SIBLING' | 'BUSINESS_PARTNER' | 'OTHER';
export type LandType = 'IRRIGATED' | 'DRY' | 'HORTICULTURE' | 'FOREST_ADJACENT';
export type OwnershipStatus = 'SOLE' | 'JOINT' | 'DISPUTED';
export type EncumbranceStatus = 'CLEAR' | 'ENCUMBERED' | 'PENDING';
export type EncumbranceVerificationStatus = 'NOT_RUN' | 'CLEAR' | 'ENCUMBERED' | 'PENDING_VERIFICATION' | 'GATEWAY_ERROR';
export type AgriMortgageDocumentType =
  | 'PATTADAR_PASSBOOK'
  | 'OWNERSHIP_PROOF'
  | 'ENCUMBRANCE_CERTIFICATE'
  | 'LAND_VALUATION_REPORT'
  | 'APPLICANT_KYC'
  | 'OTHER';
export type AgriMortgageDocumentStatus = 'UPLOADED' | 'VERIFIED' | 'REJECTED';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  username: string;
  role: UserRole;
}

export interface UserInfoResponse {
  username: string;
  role: UserRole;
}

export interface ApiErrorResponse {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  path?: string;
  errors?: Record<string, string>;
}

export interface CoBorrowerRequest {
  fullName: string;
  aadhaar: string;
  pan: string;
  monthlyIncome: number | string;
  relationshipType: RelationshipType;
}

export interface LandParcelRequest {
  surveyNumber: string;
  district: string;
  taluka: string;
  village: string;
  areaInAcres: number | string;
  landType: LandType;
  marketValue: number | string;
  govtCircleRate: number | string;
  ownershipStatus: OwnershipStatus;
  encumbranceStatus: EncumbranceStatus;
  remarks?: string;
}

export interface CreateAgriMortgageApplicationRequest {
  primaryApplicantName: string;
  primaryApplicantAadhaar: string;
  primaryApplicantPan: string;
  primaryMonthlyIncome: number | string;
  district: string;
  taluka: string;
  village: string;
  requestedAmount: number | string;
  requestedTenureMonths: number;
  purpose: string;
  coBorrowers: CoBorrowerRequest[];
  landParcels: LandParcelRequest[];
}

export interface CreateAgriMortgageDocumentRequest {
  documentType: AgriMortgageDocumentType;
  fileName: string;
  fileReference: string;
  remarks?: string;
}

export interface UpdateAgriMortgageDocumentStatusRequest {
  documentStatus: AgriMortgageDocumentStatus;
  remarks?: string;
}

export interface AdvanceAgriMortgageStatusRequest {
  targetStatus: AgriMortgageApplicationStatus;
  remarks?: string;
}

export interface AgriMortgageApplicantResponse {
  id: number;
  applicantType: ApplicantType;
  fullName: string;
  aadhaar: string;
  pan: string;
  monthlyIncome: number;
  relationshipType?: RelationshipType;
}

export interface AgriculturalLandParcelResponse {
  id: number;
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
  appraisalValue: number;
  encumbranceCheckDetails?: string | null;
  gatewayAvailable?: boolean | null;
  encumbranceCheckedAt?: string | null;
}

export interface AgriMortgageDocumentResponse {
  id: number;
  documentType: AgriMortgageDocumentType;
  documentStatus: AgriMortgageDocumentStatus;
  fileName: string;
  fileReference: string;
  remarks?: string | null;
  uploadedBy: string;
  uploadedAt: string;
  reviewedBy?: string | null;
  reviewedAt?: string | null;
}

export interface AgriMortgageDocumentSummaryResponse {
  documentsComplete: boolean;
  totalDocuments: number;
  verifiedDocuments: number;
  missingRequiredDocuments: string[];
}

export interface AgriMortgageApplicationStateHistoryResponse {
  fromStatus: AgriMortgageApplicationStatus | null;
  toStatus: AgriMortgageApplicationStatus;
  remarks?: string;
  changedBy?: string;
  changedAt: string;
}

export interface AgriMortgageApplicationResponse {
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
  status: AgriMortgageApplicationStatus;
  eligible: boolean;
  eligibilitySummary: string;
  encumbranceVerificationStatus: EncumbranceVerificationStatus;
  encumbranceVerificationSummary: string;
  encumbranceVerifiedAt?: string | null;
  totalLandValue: number;
  combinedIncome: number;
  ltvRatio: number;
  submittedAt?: string;
  sanctionedAt?: string;
  disbursedAt?: string;
  documentSummary: AgriMortgageDocumentSummaryResponse;
  documents: AgriMortgageDocumentResponse[];
  applicants: AgriMortgageApplicantResponse[];
  landParcels: AgriculturalLandParcelResponse[];
  stateHistory: AgriMortgageApplicationStateHistoryResponse[];
}

export interface AgriEligibilityRuleResult {
  ruleCode: string;
  passed: boolean;
  message: string;
}

export interface AgriEligibilityResponse {
  eligible: boolean;
  summary: string;
  totalLandValue: number;
  combinedIncome: number;
  ltvRatio: number;
  ruleResults: AgriEligibilityRuleResult[];
}

export interface AgriMortgageDashboardResponse {
  totalApplications: number;
  eligibleApplications: number;
  documentReadyApplications: number;
  applicationsPendingDocuments: number;
  clearEncumbranceApplications: number;
  encumberedApplications: number;
  pendingEncumbranceApplications: number;
  gatewayErrorApplications: number;
  draftApplications: number;
  landVerificationApplications: number;
  encumbranceCheckApplications: number;
  creditReviewApplications: number;
  legalReviewApplications: number;
  sanctionedApplications: number;
  disbursedApplications: number;
  rejectedApplications: number;
  closedApplications: number;
  totalRequestedAmount: number;
  averageRequestedAmount: number;
  totalLandParcels: number;
  totalAppraisedValue: number;
  statusCounts: Record<string, number>;
}

export interface AgriDistrictSummary {
  district: string;
  totalApplications: number;
  totalSanctionedAmount: number;
  averageLtvRatio: number;
}

export interface PageResponse<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
