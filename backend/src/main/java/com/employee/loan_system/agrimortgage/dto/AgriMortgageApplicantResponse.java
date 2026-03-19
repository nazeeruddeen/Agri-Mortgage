package com.employee.loan_system.agrimortgage.dto;

import com.employee.loan_system.agrimortgage.entity.ApplicantType;
import com.employee.loan_system.agrimortgage.entity.RelationshipType;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record AgriMortgageApplicantResponse(
        Long id,
        ApplicantType applicantType,
        String fullName,
        String aadhaar,
        String pan,
        BigDecimal monthlyIncome,
        RelationshipType relationshipType
) {
}
