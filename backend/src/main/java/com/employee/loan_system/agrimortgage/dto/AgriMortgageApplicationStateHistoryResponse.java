package com.employee.loan_system.agrimortgage.dto;

import com.employee.loan_system.agrimortgage.entity.AgriMortgageApplicationStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AgriMortgageApplicationStateHistoryResponse(
        AgriMortgageApplicationStatus fromStatus,
        AgriMortgageApplicationStatus toStatus,
        String remarks,
        String changedBy,
        LocalDateTime changedAt
) {
}
