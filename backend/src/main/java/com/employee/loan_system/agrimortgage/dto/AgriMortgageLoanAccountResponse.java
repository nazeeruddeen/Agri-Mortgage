package com.employee.loan_system.agrimortgage.dto;

import com.employee.loan_system.agrimortgage.entity.AgriMortgageLoanAccountStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record AgriMortgageLoanAccountResponse(
        Long id,
        Long applicationId,
        String applicationNumber,
        String accountNumber,
        String primaryApplicantName,
        BigDecimal principalAmount,
        BigDecimal annualInterestRate,
        Integer tenureMonths,
        BigDecimal monthlyInstallmentAmount,
        BigDecimal outstandingPrincipal,
        String disbursementReference,
        AgriMortgageLoanAccountStatus status,
        LocalDateTime disbursedAt,
        LocalDate nextDueDate,
        List<AgriRepaymentInstallmentResponse> installments,
        List<AgriRepaymentTransactionResponse> transactions
) {
}
