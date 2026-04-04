package com.employee.loan_system.agrimortgage.repository;

import com.employee.loan_system.agrimortgage.entity.AgriMortgageApplication;
import com.employee.loan_system.agrimortgage.entity.AgriMortgageApplicationStatus;
import com.employee.loan_system.agrimortgage.entity.EncumbranceVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AgriMortgageApplicationRepository extends JpaRepository<AgriMortgageApplication, Long>, JpaSpecificationExecutor<AgriMortgageApplication> {

    interface StatusCountRow {
        AgriMortgageApplicationStatus getStatus();

        long getTotal();
    }

    Optional<AgriMortgageApplication> findByApplicationNumber(String applicationNumber);

    long countByStatus(AgriMortgageApplicationStatus status);

    long countByEligibleTrue();

    long countByEncumbranceVerificationStatus(EncumbranceVerificationStatus status);

    @Query("select coalesce(sum(a.requestedAmount), 0) from AgriMortgageApplication a")
    BigDecimal sumRequestedAmount();

    @Query("select a.status as status, count(a) as total from AgriMortgageApplication a group by a.status")
    List<StatusCountRow> findStatusCounts();

    @Query("select count(a) from AgriMortgageApplication a where a.encumbranceVerificationStatus in :statuses")
    long countByEncumbranceVerificationStatusIn(@Param("statuses") List<EncumbranceVerificationStatus> statuses);

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
