package com.employee.loan_system.agrimortgage.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record AgriEligibilityResponse(
        boolean eligible,
        String summary,
        BigDecimal totalLandValue,
        BigDecimal combinedIncome,
        BigDecimal ltvRatio,
        List<AgriEligibilityRuleResult> ruleResults
) {
}
