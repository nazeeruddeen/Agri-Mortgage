package com.employee.loan_system.agrimortgage.dto;

import com.employee.loan_system.agrimortgage.entity.AgriRepaymentInstallmentStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record AgriRepaymentInstallmentResponse(
        Long id,
        Integer installmentNumber,
        LocalDate dueDate,
        BigDecimal openingPrincipal,
        BigDecimal principalDue,
        BigDecimal interestDue,
        BigDecimal principalPaid,
        BigDecimal interestPaid,
        BigDecimal remainingDue,
        AgriRepaymentInstallmentStatus status,
        LocalDateTime paidAt,
        String remarks
) {
}
