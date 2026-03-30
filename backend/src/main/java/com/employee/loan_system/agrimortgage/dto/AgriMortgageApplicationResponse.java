package com.employee.loan_system.agrimortgage.dto;

import com.employee.loan_system.agrimortgage.entity.AgriMortgageApplicationStatus;
import com.employee.loan_system.agrimortgage.entity.EncumbranceVerificationStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record AgriMortgageApplicationResponse(
        Long id,
        String applicationNumber,
        String primaryApplicantName,
        String primaryApplicantAadhaar,
        String primaryApplicantPan,
        BigDecimal primaryMonthlyIncome,
        String district,
        String taluka,
        String village,
        BigDecimal requestedAmount,
        Integer requestedTenureMonths,
        String purpose,
        AgriMortgageApplicationStatus status,
        boolean eligible,
        String eligibilitySummary,
        EncumbranceVerificationStatus encumbranceVerificationStatus,
        String encumbranceVerificationSummary,
        LocalDateTime encumbranceVerifiedAt,
        BigDecimal totalLandValue,
        BigDecimal combinedIncome,
        BigDecimal ltvRatio,
        LocalDateTime submittedAt,
        LocalDateTime sanctionedAt,
        LocalDateTime disbursedAt,
        AgriMortgageDocumentSummaryResponse documentSummary,
        List<AgriMortgageDocumentResponse> documents,
        List<AgriMortgageApplicantResponse> applicants,
        List<AgriculturalLandParcelResponse> landParcels,
        List<AgriMortgageApplicationStateHistoryResponse> stateHistory
) {
}
