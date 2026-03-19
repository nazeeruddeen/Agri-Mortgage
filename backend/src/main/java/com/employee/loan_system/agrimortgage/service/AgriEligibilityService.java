package com.employee.loan_system.agrimortgage.service;

import com.employee.loan_system.agrimortgage.dto.AgriEligibilityResponse;
import com.employee.loan_system.agrimortgage.dto.AgriEligibilityRuleResult;
import com.employee.loan_system.agrimortgage.entity.AgriMortgageApplicant;
import com.employee.loan_system.agrimortgage.entity.AgriMortgageApplication;
import com.employee.loan_system.agrimortgage.entity.ApplicantType;
import com.employee.loan_system.agrimortgage.entity.AgriculturalLandParcel;
import com.employee.loan_system.agrimortgage.entity.EncumbranceStatus;
import com.employee.loan_system.agrimortgage.entity.OwnershipStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class AgriEligibilityService {

    private static final BigDecimal MAX_LTV_RATIO = new BigDecimal("0.70");

    public AgriEligibilityResponse evaluate(AgriMortgageApplication application) {
        BigDecimal totalLandValue = application.getLandParcels().stream()
                .map(AgriculturalLandParcel::appraisalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal combinedIncome = application.getPrimaryMonthlyIncome().setScale(2, RoundingMode.HALF_UP);
        for (AgriMortgageApplicant applicant : application.getApplicants()) {
            if (applicant.getApplicantType() != ApplicantType.CO_BORROWER) {
                continue;
            }
            combinedIncome = combinedIncome.add(applicant.getMonthlyIncome().setScale(2, RoundingMode.HALF_UP));
        }

        BigDecimal ltvRatio = totalLandValue.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ONE
                : application.getRequestedAmount().divide(totalLandValue, 4, RoundingMode.HALF_UP);

        List<AgriEligibilityRuleResult> ruleResults = new ArrayList<>();
        ruleResults.add(rule("LAND_AVAILABLE", !application.getLandParcels().isEmpty(), "At least one land parcel is required"));
        ruleResults.add(rule("NO_DISPUTED_OWNERSHIP", application.getLandParcels().stream().noneMatch(parcel -> parcel.getOwnershipStatus() == OwnershipStatus.DISPUTED),
                "No land parcel can have disputed ownership"));
        ruleResults.add(rule("NO_ENCUMBRANCE", application.getLandParcels().stream().allMatch(parcel -> parcel.getEncumbranceStatus() == EncumbranceStatus.CLEAR),
                "All land parcels must be free from encumbrance"));
        ruleResults.add(rule("LTV_THRESHOLD", ltvRatio.compareTo(MAX_LTV_RATIO) <= 0,
                "Requested amount must stay within the 70% LTV cap"));
        BigDecimal estimatedMonthlyObligation = application.getRequestedAmount()
                .divide(BigDecimal.valueOf(Math.max(1, application.getRequestedTenureMonths())), 2, RoundingMode.HALF_UP);
        ruleResults.add(rule("AFFORDABILITY", combinedIncome.multiply(new BigDecimal("0.40")).compareTo(estimatedMonthlyObligation) >= 0,
                "Combined income should cover the estimated monthly obligation"));

        boolean eligible = ruleResults.stream().allMatch(AgriEligibilityRuleResult::passed);
        String summary = eligible
                ? "Eligibility checks passed"
                : ruleResults.stream().filter(result -> !result.passed()).map(result -> result.ruleCode() + ": " + result.message()).reduce((a, b) -> a + "; " + b).orElse("Eligibility checks failed");

        return AgriEligibilityResponse.builder()
                .eligible(eligible)
                .summary(summary)
                .totalLandValue(totalLandValue)
                .combinedIncome(combinedIncome)
                .ltvRatio(ltvRatio)
                .ruleResults(ruleResults)
                .build();
    }

    private AgriEligibilityRuleResult rule(String code, boolean passed, String message) {
        return AgriEligibilityRuleResult.builder()
                .ruleCode(code)
                .passed(passed)
                .message(passed ? "Passed" : message)
                .build();
    }
}
