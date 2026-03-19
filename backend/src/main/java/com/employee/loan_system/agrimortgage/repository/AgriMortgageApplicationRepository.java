package com.employee.loan_system.agrimortgage.repository;

import com.employee.loan_system.agrimortgage.entity.AgriMortgageApplication;
import com.employee.loan_system.agrimortgage.entity.AgriMortgageApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Optional;

public interface AgriMortgageApplicationRepository extends JpaRepository<AgriMortgageApplication, Long>, JpaSpecificationExecutor<AgriMortgageApplication> {

    Optional<AgriMortgageApplication> findByApplicationNumber(String applicationNumber);

    long countByStatus(AgriMortgageApplicationStatus status);

    @Query("select coalesce(sum(a.requestedAmount), 0) from AgriMortgageApplication a")
    BigDecimal sumRequestedAmount();
}
