package com.employee.loan_system.agrimortgage.repository;

import com.employee.loan_system.agrimortgage.entity.AgriculturalLandParcel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

public interface AgriculturalLandParcelRepository extends JpaRepository<AgriculturalLandParcel, Long> {

    @Query("select count(p) from AgriculturalLandParcel p")
    long countAllParcels();

    @Query(value = "select coalesce(sum(least(market_value, govt_circle_rate)), 0) from agricultural_land_parcels", nativeQuery = true)
    BigDecimal sumTotalAppraisedValue();
}
