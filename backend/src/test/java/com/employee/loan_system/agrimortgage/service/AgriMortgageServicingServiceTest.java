package com.employee.loan_system.agrimortgage.service;

import com.employee.loan_system.agrimortgage.entity.AgriMortgageApplication;
import com.employee.loan_system.agrimortgage.entity.AgriMortgageLoanAccount;
import com.employee.loan_system.agrimortgage.entity.AgriMortgageLoanAccountStatus;
import com.employee.loan_system.agrimortgage.entity.AgriRepaymentInstallment;
import com.employee.loan_system.agrimortgage.entity.AgriRepaymentInstallmentStatus;
import com.employee.loan_system.agrimortgage.repository.AgriMortgageLoanAccountRepository;
import com.employee.loan_system.agrimortgage.repository.AgriRepaymentInstallmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgriMortgageServicingServiceTest {

    @Mock
    private AgriMortgageLoanAccountRepository loanAccountRepository;

    @Mock
    private AgriRepaymentInstallmentRepository installmentRepository;

    @Test
    void ageOverdueInstallmentsShouldMarkPastDueInstallmentsAndRefreshAccount() {
        AgriMortgageServicingService service = new AgriMortgageServicingService(
                loanAccountRepository,
                installmentRepository,
                new BigDecimal("9.50"));

        LocalDate asOfDate = LocalDate.of(2026, 4, 4);
        AgriMortgageLoanAccount account = buildAccount();
        AgriRepaymentInstallment overdue = buildInstallment(account, 1, asOfDate.minusDays(5));
        overdue.setPrincipalPaid(new BigDecimal("5000.00"));
        overdue.setStatus(AgriRepaymentInstallmentStatus.PARTIAL);

        AgriRepaymentInstallment next = buildInstallment(account, 2, asOfDate.plusDays(10));
        account.getInstallments().addAll(List.of(overdue, next));

        when(installmentRepository.findPastDueInstallments(asOfDate)).thenReturn(List.of(overdue));

        int aged = service.ageOverdueInstallments(asOfDate);

        assertThat(aged).isEqualTo(1);
        assertThat(overdue.getStatus()).isEqualTo(AgriRepaymentInstallmentStatus.OVERDUE);
        assertThat(account.getNextDueDate()).isEqualTo(asOfDate.minusDays(5));
        verify(loanAccountRepository).saveAll(anyList());
    }

    @Test
    void ageOverdueInstallmentsShouldNoopWhenNothingIsPastDue() {
        AgriMortgageServicingService service = new AgriMortgageServicingService(
                loanAccountRepository,
                installmentRepository,
                new BigDecimal("9.50"));

        LocalDate asOfDate = LocalDate.of(2026, 4, 4);
        when(installmentRepository.findPastDueInstallments(asOfDate)).thenReturn(List.of());

        int aged = service.ageOverdueInstallments(asOfDate);

        assertThat(aged).isZero();
    }

    private AgriMortgageLoanAccount buildAccount() {
        AgriMortgageApplication application = new AgriMortgageApplication();
        application.setId(42L);
        application.setApplicationNumber("AGRI-42");
        application.setPrimaryApplicantName("Savita Deshmukh");

        AgriMortgageLoanAccount account = new AgriMortgageLoanAccount();
        account.setId(99L);
        account.setApplication(application);
        account.setAccountNumber("AGL-000042");
        account.setDisbursementReference("DISB-AGRI-42");
        account.setPrincipalAmount(new BigDecimal("1000000.00"));
        account.setAnnualInterestRate(new BigDecimal("9.50"));
        account.setTenureMonths(24);
        account.setMonthlyInstallmentAmount(new BigDecimal("45934.00"));
        account.setOutstandingPrincipal(new BigDecimal("850000.00"));
        account.setDisbursedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        account.setNextDueDate(LocalDate.of(2026, 2, 1));
        account.setStatus(AgriMortgageLoanAccountStatus.ACTIVE);
        return account;
    }

    private AgriRepaymentInstallment buildInstallment(AgriMortgageLoanAccount account, int number, LocalDate dueDate) {
        AgriRepaymentInstallment installment = new AgriRepaymentInstallment();
        installment.setLoanAccount(account);
        installment.setInstallmentNumber(number);
        installment.setDueDate(dueDate);
        installment.setOpeningPrincipal(new BigDecimal("850000.00"));
        installment.setPrincipalDue(new BigDecimal("35000.00"));
        installment.setInterestDue(new BigDecimal("10934.00"));
        installment.setPrincipalPaid(BigDecimal.ZERO.setScale(2));
        installment.setInterestPaid(BigDecimal.ZERO.setScale(2));
        installment.setStatus(AgriRepaymentInstallmentStatus.PENDING);
        return installment;
    }
}
