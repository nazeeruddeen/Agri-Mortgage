package com.employee.loan_system.agrimortgage.dto;

import com.employee.loan_system.agrimortgage.entity.AgriRepaymentMode;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record AgriRepaymentTransactionResponse(
        Long id,
        String transactionReference,
        BigDecimal amount,
        BigDecimal appliedPrincipalAmount,
        BigDecimal prepaymentPrincipalAmount,
        BigDecimal appliedInterestAmount,
        AgriRepaymentMode paymentMode,
        LocalDate paymentDate,
        String notes,
        String recordedBy,
        LocalDateTime recordedAt
) {
}
