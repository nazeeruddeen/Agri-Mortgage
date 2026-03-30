package com.employee.loan_system.agrimortgage.dto;

import com.employee.loan_system.agrimortgage.entity.AgriMortgageApplicationStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Map;

@Builder
public record AgriMortgageDashboardResponse(
        long totalApplications,
        long eligibleApplications,
        long documentReadyApplications,
        long applicationsPendingDocuments,
        long clearEncumbranceApplications,
        long encumberedApplications,
        long pendingEncumbranceApplications,
        long gatewayErrorApplications,
        long draftApplications,
        long landVerificationApplications,
        long encumbranceCheckApplications,
        long creditReviewApplications,
        long legalReviewApplications,
        long sanctionedApplications,
        long disbursedApplications,
        long rejectedApplications,
        long closedApplications,
        BigDecimal totalRequestedAmount,
        BigDecimal averageRequestedAmount,
        long totalLandParcels,
        BigDecimal totalAppraisedValue,
        Map<AgriMortgageApplicationStatus, Long> statusCounts
) {
}
