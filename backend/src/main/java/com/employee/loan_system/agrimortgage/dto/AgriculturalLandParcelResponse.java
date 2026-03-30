package com.employee.loan_system.agrimortgage.dto;

import com.employee.loan_system.agrimortgage.entity.EncumbranceStatus;
import com.employee.loan_system.agrimortgage.entity.LandType;
import com.employee.loan_system.agrimortgage.entity.OwnershipStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record AgriculturalLandParcelResponse(
        Long id,
        String surveyNumber,
        String district,
        String taluka,
        String village,
        BigDecimal areaInAcres,
        LandType landType,
        BigDecimal marketValue,
        BigDecimal govtCircleRate,
        OwnershipStatus ownershipStatus,
        EncumbranceStatus encumbranceStatus,
        String remarks,
        BigDecimal appraisalValue,
        String encumbranceCheckDetails,
        Boolean gatewayAvailable,
        LocalDateTime encumbranceCheckedAt
) {
}
