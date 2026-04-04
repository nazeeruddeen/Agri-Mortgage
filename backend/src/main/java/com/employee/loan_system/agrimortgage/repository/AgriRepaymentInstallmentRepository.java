package com.employee.loan_system.agrimortgage.repository;

import com.employee.loan_system.agrimortgage.entity.AgriRepaymentInstallment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AgriRepaymentInstallmentRepository extends JpaRepository<AgriRepaymentInstallment, Long> {

    @EntityGraph(attributePaths = {"loanAccount", "loanAccount.application"})
    @Query("""
            select installment
            from AgriRepaymentInstallment installment
            where installment.status <> com.employee.loan_system.agrimortgage.entity.AgriRepaymentInstallmentStatus.PAID
              and installment.dueDate < :asOfDate
              and (installment.principalDue + installment.interestDue) > (installment.principalPaid + installment.interestPaid)
            order by installment.dueDate asc
            """)
    List<AgriRepaymentInstallment> findPastDueInstallments(LocalDate asOfDate);
}
