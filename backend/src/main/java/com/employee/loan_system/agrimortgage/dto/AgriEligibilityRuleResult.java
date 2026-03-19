package com.employee.loan_system.agrimortgage.dto;

import lombok.Builder;

@Builder
public record AgriEligibilityRuleResult(
        String ruleCode,
        boolean passed,
        String message
) {
}
