package com.employee.loan_system.agrimortgage.repository;

import com.employee.loan_system.agrimortgage.entity.AgriMortgageLoanAccount;
import com.employee.loan_system.agrimortgage.entity.AgriMortgageLoanAccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgriMortgageLoanAccountRepository extends JpaRepository<AgriMortgageLoanAccount, Long> {

    @EntityGraph(attributePaths = {"application"})
    Optional<AgriMortgageLoanAccount> findByApplication_Id(Long applicationId);

    @EntityGraph(attributePaths = {"application"})
    Optional<AgriMortgageLoanAccount> findByAccountNumber(String accountNumber);

    @EntityGraph(attributePaths = {"application"})
    Page<AgriMortgageLoanAccount> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(AgriMortgageLoanAccountStatus status);
}
