package com.employee.loan_system.agrimortgage.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(
        name = "agri_mortgage_loan_accounts",
        uniqueConstraints = @UniqueConstraint(name = "uk_agri_loan_accounts_application", columnNames = "application_id")
)
public class AgriMortgageLoanAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private AgriMortgageApplication application;

    @Column(name = "account_number", nullable = false, length = 40, unique = true)
    private String accountNumber;

    @Column(name = "disbursement_reference", nullable = false, length = 80)
    private String disbursementReference;

    @Column(name = "principal_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "annual_interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal annualInterestRate;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Column(name = "monthly_installment_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyInstallmentAmount;

    @Column(name = "outstanding_principal", nullable = false, precision = 15, scale = 2)
    private BigDecimal outstandingPrincipal;

    @Column(name = "disbursed_at", nullable = false)
    private LocalDateTime disbursedAt;

    @Column(name = "next_due_date", nullable = false)
    private LocalDate nextDueDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 30)
    private AgriMortgageLoanAccountStatus status = AgriMortgageLoanAccountStatus.ACTIVE;

    @OneToMany(mappedBy = "loanAccount", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("installmentNumber ASC")
    private List<AgriRepaymentInstallment> installments = new ArrayList<>();

    @OneToMany(mappedBy = "loanAccount", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("recordedAt ASC")
    private List<AgriRepaymentTransaction> transactions = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void addInstallment(AgriRepaymentInstallment installment) {
        installment.setLoanAccount(this);
        installments.add(installment);
    }

    public void addTransaction(AgriRepaymentTransaction transaction) {
        transaction.setLoanAccount(this);
        transactions.add(transaction);
    }
}
