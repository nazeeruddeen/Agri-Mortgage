package com.employee.loan_system.agrimortgage.repository;

import com.employee.loan_system.agrimortgage.entity.AgriMortgageApplication;
import com.employee.loan_system.agrimortgage.entity.AgriMortgageApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AgriMortgageApplicationRepository extends JpaRepository<AgriMortgageApplication, Long>, JpaSpecificationExecutor<AgriMortgageApplication> {

    Optional<AgriMortgageApplication> findByApplicationNumber(String applicationNumber);

    long countByStatus(AgriMortgageApplicationStatus status);

    @Query("select coalesce(sum(a.requestedAmount), 0) from AgriMortgageApplication a")
    BigDecimal sumRequestedAmount();

    @Query(value = """
            select
                district,
                count(*) as total_applications,
                sum(case when status in ('SANCTIONED', 'DISBURSED', 'CLOSED') then 1 else 0 end) as sanctioned_applications,
                coalesce(sum(case when status in ('SANCTIONED', 'DISBURSED', 'CLOSED') then requested_amount else 0 end), 0) as total_sanctioned_amount,
                coalesce(avg(ltv_ratio), 0) as average_ltv_ratio
            from agri_mortgage_applications
            group by district
            order by district
            """, nativeQuery = true)
    List<Object[]> findDistrictSummaryRows();
}
