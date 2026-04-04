package com.employee.loan_system.agrimortgage.service;

import com.employee.loan_system.agrimortgage.dto.AgriMortgageLoanAccountResponse;
import com.employee.loan_system.agrimortgage.dto.AgriRepaymentInstallmentResponse;
import com.employee.loan_system.agrimortgage.dto.AgriRepaymentTransactionResponse;
import com.employee.loan_system.agrimortgage.dto.RecordAgriRepaymentRequest;
import com.employee.loan_system.agrimortgage.entity.AgriMortgageApplication;
import com.employee.loan_system.agrimortgage.entity.AgriMortgageLoanAccount;
import com.employee.loan_system.agrimortgage.entity.AgriMortgageLoanAccountStatus;
import com.employee.loan_system.agrimortgage.entity.AgriRepaymentInstallment;
import com.employee.loan_system.agrimortgage.entity.AgriRepaymentInstallmentStatus;
import com.employee.loan_system.agrimortgage.entity.AgriRepaymentTransaction;
import com.employee.loan_system.agrimortgage.repository.AgriMortgageLoanAccountRepository;
import com.employee.loan_system.agrimortgage.repository.AgriRepaymentInstallmentRepository;
import com.employee.loan_system.exception.BusinessRuleException;
import com.employee.loan_system.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AgriMortgageServicingService {

    private final AgriMortgageLoanAccountRepository loanAccountRepository;
    private final AgriRepaymentInstallmentRepository installmentRepository;
    private final BigDecimal defaultInterestRate;

    public AgriMortgageServicingService(
            AgriMortgageLoanAccountRepository loanAccountRepository,
            AgriRepaymentInstallmentRepository installmentRepository,
            @Value("${app.agri.servicing.default-interest-rate:9.50}") BigDecimal defaultInterestRate) {
        this.loanAccountRepository = loanAccountRepository;
        this.installmentRepository = installmentRepository;
        this.defaultInterestRate = defaultInterestRate;
    }

    @Transactional
    public AgriMortgageLoanAccount ensureLoanAccountForDisbursement(AgriMortgageApplication application, String disbursementReference) {
        return loanAccountRepository.findByApplication_Id(application.getId())
                .orElseGet(() -> loanAccountRepository.save(createLoanAccount(application, disbursementReference)));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public AgriMortgageLoanAccountResponse getByApplicationId(Long applicationId) {
        return toResponse(findByApplicationId(applicationId));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public Page<AgriMortgageLoanAccountResponse> listAccounts(Pageable pageable) {
        return loanAccountRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    public AgriRepaymentTransactionResponse recordRepayment(Long accountId, RecordAgriRepaymentRequest request) {
        AgriMortgageLoanAccount account = loanAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Agri mortgage loan account not found with id: " + accountId));
        if (account.getStatus() != AgriMortgageLoanAccountStatus.ACTIVE) {
            throw new BusinessRuleException("Repayments can only be recorded for active mortgage accounts");
        }

        BigDecimal amountRemaining = request.getAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAppliedPrincipal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAppliedInterest = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal prepaymentPrincipalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        LocalDate paymentDate = request.getPaymentDate();
        AgriRepaymentInstallment firstTouchedInstallment = null;

        List<AgriRepaymentInstallment> installments = account.getInstallments().stream()
                .sorted(Comparator.comparing(AgriRepaymentInstallment::getInstallmentNumber))
                .toList();

        boolean hasDueInstallments = installments.stream()
                .filter(this::hasRemainingDue)
                .anyMatch(installment -> !installment.getDueDate().isAfter(paymentDate));
        boolean advancedToNextInstallment = false;

        for (AgriRepaymentInstallment installment : installments) {
            if (amountRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            if (!isEligibleForScheduledPayment(installment, paymentDate, hasDueInstallments, advancedToNextInstallment)) {
                continue;
            }

            BigDecimal interestRemaining = installment.getInterestDue().subtract(installment.getInterestPaid());
            BigDecimal principalRemaining = installment.getPrincipalDue().subtract(installment.getPrincipalPaid());
            if (interestRemaining.add(principalRemaining).compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            if (firstTouchedInstallment == null) {
                firstTouchedInstallment = installment;
            }

            BigDecimal interestApplied = amountRemaining.min(interestRemaining.max(BigDecimal.ZERO));
            installment.setInterestPaid(installment.getInterestPaid().add(interestApplied));
            amountRemaining = amountRemaining.subtract(interestApplied);
            totalAppliedInterest = totalAppliedInterest.add(interestApplied);

            BigDecimal principalApplied = amountRemaining.min(principalRemaining.max(BigDecimal.ZERO));
            installment.setPrincipalPaid(installment.getPrincipalPaid().add(principalApplied));
            amountRemaining = amountRemaining.subtract(principalApplied);
            totalAppliedPrincipal = totalAppliedPrincipal.add(principalApplied);

            updateInstallmentStatus(installment, paymentDate);
            if (hasDueInstallments || !installment.getDueDate().isBefore(paymentDate)) {
                advancedToNextInstallment = true;
            }
        }

        BigDecimal remainingPrincipalCapacity = account.getOutstandingPrincipal().subtract(totalAppliedPrincipal)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        if (amountRemaining.compareTo(BigDecimal.ZERO) > 0 && remainingPrincipalCapacity.compareTo(BigDecimal.ZERO) > 0) {
            prepaymentPrincipalAmount = amountRemaining.min(remainingPrincipalCapacity);
            amountRemaining = amountRemaining.subtract(prepaymentPrincipalAmount);
            totalAppliedPrincipal = totalAppliedPrincipal.add(prepaymentPrincipalAmount);
        }

        if (amountRemaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException("Repayment amount exceeds the outstanding principal and currently due interest");
        }

        account.setOutstandingPrincipal(account.getOutstandingPrincipal().subtract(totalAppliedPrincipal)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP));
        if (prepaymentPrincipalAmount.compareTo(BigDecimal.ZERO) > 0) {
            recastFutureInstallments(account, paymentDate);
        }
        refreshNextDueDate(account, paymentDate);
        if (account.getOutstandingPrincipal().compareTo(BigDecimal.ZERO) <= 0) {
            account.setStatus(AgriMortgageLoanAccountStatus.CLOSED);
        }

        AgriRepaymentTransaction transaction = new AgriRepaymentTransaction();
        transaction.setTransactionReference(request.getTransactionReference().trim());
        transaction.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
        transaction.setAppliedPrincipalAmount(totalAppliedPrincipal);
        transaction.setPrepaymentPrincipalAmount(prepaymentPrincipalAmount);
        transaction.setAppliedInterestAmount(totalAppliedInterest);
        transaction.setPaymentMode(request.getPaymentMode());
        transaction.setPaymentDate(paymentDate);
        transaction.setNotes(request.getNotes() == null ? null : request.getNotes().trim());
        transaction.setRecordedBy(currentActor());
        transaction.setInstallment(firstTouchedInstallment);
        account.addTransaction(transaction);

        loanAccountRepository.save(account);
        return toTransactionResponse(transaction);
    }

    @Transactional
    public int ageOverdueInstallments(LocalDate asOfDate) {
        LocalDate effectiveDate = asOfDate == null ? LocalDate.now() : asOfDate;
        List<AgriRepaymentInstallment> overdueCandidates = installmentRepository.findPastDueInstallments(effectiveDate);
        if (overdueCandidates.isEmpty()) {
            return 0;
        }

        List<AgriMortgageLoanAccount> touchedAccounts = new ArrayList<>();
        for (AgriRepaymentInstallment installment : overdueCandidates) {
            if (installment.getStatus() != AgriRepaymentInstallmentStatus.OVERDUE) {
                installment.setStatus(AgriRepaymentInstallmentStatus.OVERDUE);
                installment.setPaidAt(null);
                if (installment.getRemarks() == null || installment.getRemarks().isBlank()) {
                    installment.setRemarks("Marked overdue during servicing aging sweep on " + effectiveDate);
                }
            }

            AgriMortgageLoanAccount account = installment.getLoanAccount();
            if (account != null && touchedAccounts.stream().noneMatch(existing -> existing.getId().equals(account.getId()))) {
                refreshNextDueDate(account, effectiveDate);
                touchedAccounts.add(account);
            }
        }

        loanAccountRepository.saveAll(touchedAccounts);
        return overdueCandidates.size();
    }

    private AgriMortgageLoanAccount createLoanAccount(AgriMortgageApplication application, String disbursementReference) {
        AgriMortgageLoanAccount account = new AgriMortgageLoanAccount();
        account.setApplication(application);
        account.setAccountNumber(String.format("AGL-%06d", application.getId()));
        account.setDisbursementReference(disbursementReference == null || disbursementReference.isBlank()
                ? "DISB-" + application.getApplicationNumber()
                : disbursementReference.trim());
        account.setPrincipalAmount(application.getRequestedAmount().setScale(2, RoundingMode.HALF_UP));
        account.setAnnualInterestRate(defaultInterestRate.setScale(2, RoundingMode.HALF_UP));
        account.setTenureMonths(application.getRequestedTenureMonths());
        account.setMonthlyInstallmentAmount(calculateEmi(
                application.getRequestedAmount(),
                defaultInterestRate,
                application.getRequestedTenureMonths()));
        account.setOutstandingPrincipal(application.getRequestedAmount().setScale(2, RoundingMode.HALF_UP));
        account.setDisbursedAt(application.getDisbursedAt());
        account.setNextDueDate(application.getDisbursedAt().toLocalDate().plusMonths(1));

        for (AgriRepaymentInstallment installment : buildSchedule(account)) {
            account.addInstallment(installment);
        }
        return account;
    }

    private List<AgriRepaymentInstallment> buildSchedule(AgriMortgageLoanAccount account) {
        List<AgriRepaymentInstallment> installments = new ArrayList<>();
        BigDecimal remainingPrincipal = account.getPrincipalAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal monthlyRate = account.getAnnualInterestRate().divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
        BigDecimal emi = account.getMonthlyInstallmentAmount().setScale(2, RoundingMode.HALF_UP);
        LocalDate dueDate = account.getDisbursedAt().toLocalDate().plusMonths(1);

        for (int installmentNumber = 1; installmentNumber <= account.getTenureMonths(); installmentNumber++) {
            BigDecimal interestDue = remainingPrincipal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalDue = emi.subtract(interestDue).setScale(2, RoundingMode.HALF_UP);
            if (installmentNumber == account.getTenureMonths()) {
                principalDue = remainingPrincipal;
            }
            if (principalDue.compareTo(BigDecimal.ZERO) < 0) {
                principalDue = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }

            AgriRepaymentInstallment installment = new AgriRepaymentInstallment();
            installment.setInstallmentNumber(installmentNumber);
            installment.setDueDate(dueDate);
            installment.setOpeningPrincipal(remainingPrincipal);
            installment.setPrincipalDue(principalDue);
            installment.setInterestDue(interestDue);
            installment.setPrincipalPaid(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            installment.setInterestPaid(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            installment.setStatus(AgriRepaymentInstallmentStatus.PENDING);
            installments.add(installment);

            remainingPrincipal = remainingPrincipal.subtract(principalDue).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            dueDate = dueDate.plusMonths(1);
        }
        return installments;
    }

    private boolean isEligibleForScheduledPayment(
            AgriRepaymentInstallment installment,
            LocalDate paymentDate,
            boolean hasDueInstallments,
            boolean advancedToNextInstallment) {
        if (!hasRemainingDue(installment)) {
            return false;
        }
        if (!hasDueInstallments) {
            return !advancedToNextInstallment;
        }
        return !installment.getDueDate().isAfter(paymentDate);
    }

    private boolean hasRemainingDue(AgriRepaymentInstallment installment) {
        return installment.remainingDue().compareTo(BigDecimal.ZERO) > 0;
    }

    private void updateInstallmentStatus(AgriRepaymentInstallment installment, LocalDate paymentDate) {
        if (installment.remainingDue().compareTo(BigDecimal.ZERO) <= 0) {
            installment.setStatus(AgriRepaymentInstallmentStatus.PAID);
            installment.setPaidAt(LocalDateTime.now());
            installment.setRemarks(null);
            return;
        }
        installment.setPaidAt(null);
        installment.setStatus(installment.getDueDate().isBefore(paymentDate)
                ? AgriRepaymentInstallmentStatus.OVERDUE
                : AgriRepaymentInstallmentStatus.PARTIAL);
    }

    private void recastFutureInstallments(AgriMortgageLoanAccount account, LocalDate paymentDate) {
        List<AgriRepaymentInstallment> futureInstallments = account.getInstallments().stream()
                .filter(installment -> installment.remainingDue().compareTo(BigDecimal.ZERO) > 0)
                .filter(installment -> installment.getPrincipalPaid().compareTo(BigDecimal.ZERO) == 0
                        && installment.getInterestPaid().compareTo(BigDecimal.ZERO) == 0)
                .sorted(Comparator.comparing(AgriRepaymentInstallment::getInstallmentNumber))
                .toList();

        if (futureInstallments.isEmpty()) {
            account.setMonthlyInstallmentAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            return;
        }

        BigDecimal remainingPrincipal = account.getOutstandingPrincipal().setScale(2, RoundingMode.HALF_UP);
        BigDecimal monthlyRate = account.getAnnualInterestRate().divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
        BigDecimal recalculatedEmi = calculateEmi(remainingPrincipal, account.getAnnualInterestRate(), futureInstallments.size());

        for (int index = 0; index < futureInstallments.size(); index++) {
            AgriRepaymentInstallment installment = futureInstallments.get(index);
            BigDecimal interestDue = remainingPrincipal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalDue = recalculatedEmi.subtract(interestDue).setScale(2, RoundingMode.HALF_UP);
            if (index == futureInstallments.size() - 1) {
                principalDue = remainingPrincipal;
            }
            if (principalDue.compareTo(BigDecimal.ZERO) < 0) {
                principalDue = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }

            installment.setOpeningPrincipal(remainingPrincipal);
            installment.setInterestDue(interestDue);
            installment.setPrincipalDue(principalDue);
            installment.setPrincipalPaid(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            installment.setInterestPaid(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            installment.setPaidAt(null);
            installment.setStatus(installment.getDueDate().isBefore(paymentDate)
                    ? AgriRepaymentInstallmentStatus.OVERDUE
                    : AgriRepaymentInstallmentStatus.PENDING);
            installment.setRemarks("Schedule recast after principal prepayment on " + paymentDate);

            remainingPrincipal = remainingPrincipal.subtract(principalDue).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        }

        account.setMonthlyInstallmentAmount(recalculatedEmi.setScale(2, RoundingMode.HALF_UP));
    }

    private void refreshNextDueDate(AgriMortgageLoanAccount account, LocalDate paymentDate) {
        LocalDate nextDueDate = account.getInstallments().stream()
                .filter(installment -> installment.remainingDue().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(AgriRepaymentInstallment::getDueDate))
                .map(AgriRepaymentInstallment::getDueDate)
                .findFirst()
                .orElse(paymentDate);
        account.setNextDueDate(nextDueDate);
    }

    private BigDecimal calculateEmi(BigDecimal principal, BigDecimal annualInterestRate, Integer tenureMonths) {
        if (tenureMonths == null || tenureMonths <= 0) {
            return principal.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal monthlyRate = annualInterestRate.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
        }

        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal factor = onePlusRate.pow(tenureMonths);
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(factor);
        BigDecimal denominator = factor.subtract(BigDecimal.ONE);
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    public AgriMortgageLoanAccount findByApplicationId(Long applicationId) {
        return loanAccountRepository.findByApplication_Id(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Agri mortgage loan account not found for application id: " + applicationId));
    }

    private AgriMortgageLoanAccountResponse toResponse(AgriMortgageLoanAccount account) {
        return AgriMortgageLoanAccountResponse.builder()
                .id(account.getId())
                .applicationId(account.getApplication().getId())
                .applicationNumber(account.getApplication().getApplicationNumber())
                .accountNumber(account.getAccountNumber())
                .primaryApplicantName(account.getApplication().getPrimaryApplicantName())
                .principalAmount(account.getPrincipalAmount())
                .annualInterestRate(account.getAnnualInterestRate())
                .tenureMonths(account.getTenureMonths())
                .monthlyInstallmentAmount(account.getMonthlyInstallmentAmount())
                .outstandingPrincipal(account.getOutstandingPrincipal())
                .disbursementReference(account.getDisbursementReference())
                .status(account.getStatus())
                .disbursedAt(account.getDisbursedAt())
                .nextDueDate(account.getNextDueDate())
                .installments(account.getInstallments().stream()
                        .sorted(Comparator.comparing(AgriRepaymentInstallment::getInstallmentNumber))
                        .map(this::toInstallmentResponse)
                        .toList())
                .transactions(account.getTransactions().stream()
                        .sorted(Comparator.comparing(AgriRepaymentTransaction::getRecordedAt).reversed())
                        .map(this::toTransactionResponse)
                        .toList())
                .build();
    }

    private AgriRepaymentInstallmentResponse toInstallmentResponse(AgriRepaymentInstallment installment) {
        AgriRepaymentInstallmentStatus effectiveStatus = installment.getStatus();
        if (effectiveStatus != AgriRepaymentInstallmentStatus.PAID && installment.getDueDate().isBefore(LocalDate.now())) {
            effectiveStatus = AgriRepaymentInstallmentStatus.OVERDUE;
        }
        return AgriRepaymentInstallmentResponse.builder()
                .id(installment.getId())
                .installmentNumber(installment.getInstallmentNumber())
                .dueDate(installment.getDueDate())
                .openingPrincipal(installment.getOpeningPrincipal())
                .principalDue(installment.getPrincipalDue())
                .interestDue(installment.getInterestDue())
                .principalPaid(installment.getPrincipalPaid())
                .interestPaid(installment.getInterestPaid())
                .remainingDue(installment.remainingDue())
                .status(effectiveStatus)
                .paidAt(installment.getPaidAt())
                .remarks(installment.getRemarks())
                .build();
    }

    private AgriRepaymentTransactionResponse toTransactionResponse(AgriRepaymentTransaction transaction) {
        return AgriRepaymentTransactionResponse.builder()
                .id(transaction.getId())
                .transactionReference(transaction.getTransactionReference())
                .amount(transaction.getAmount())
                .appliedPrincipalAmount(transaction.getAppliedPrincipalAmount())
                .prepaymentPrincipalAmount(transaction.getPrepaymentPrincipalAmount())
                .appliedInterestAmount(transaction.getAppliedInterestAmount())
                .paymentMode(transaction.getPaymentMode())
                .paymentDate(transaction.getPaymentDate())
                .notes(transaction.getNotes())
                .recordedBy(transaction.getRecordedBy())
                .recordedAt(transaction.getRecordedAt())
                .build();
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || !authentication.isAuthenticated()) {
            return "SYSTEM";
        }
        return authentication.getName();
    }
}
