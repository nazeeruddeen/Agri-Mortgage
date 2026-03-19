package com.employee.loan_system.agrimortgage.dto;

import com.employee.loan_system.agrimortgage.entity.EncumbranceStatus;
import com.employee.loan_system.agrimortgage.entity.LandType;
import com.employee.loan_system.agrimortgage.entity.OwnershipStatus;
import com.employee.loan_system.agrimortgage.entity.RelationshipType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class CreateAgriMortgageApplicationRequest {

    @NotBlank
    private String primaryApplicantName;

    @NotBlank
    private String primaryApplicantAadhaar;

    @NotBlank
    private String primaryApplicantPan;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal primaryMonthlyIncome;

    @NotBlank
    private String district;

    @NotBlank
    private String taluka;

    @NotBlank
    private String village;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal requestedAmount;

    @NotNull
    @Positive
    private Integer requestedTenureMonths;

    @NotBlank
    @Size(max = 200)
    private String purpose;

    @Valid
    private List<CoBorrowerRequest> coBorrowers = new ArrayList<>();

    @Valid
    private List<LandParcelRequest> landParcels = new ArrayList<>();

    @Data
    public static class CoBorrowerRequest {
        @NotBlank
        private String fullName;

        @NotBlank
        private String aadhaar;

        @NotBlank
        private String pan;

        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        private BigDecimal monthlyIncome;

        @NotNull
        private RelationshipType relationshipType;
    }

    @Data
    public static class LandParcelRequest {
        @NotBlank
        private String surveyNumber;

        @NotBlank
        private String district;

        @NotBlank
        private String taluka;

        @NotBlank
        private String village;

        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        private BigDecimal areaInAcres;

        @NotNull
        private LandType landType;

        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        private BigDecimal marketValue;

        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        private BigDecimal govtCircleRate;

        @NotNull
        private OwnershipStatus ownershipStatus;

        @NotNull
        private EncumbranceStatus encumbranceStatus;

        @Size(max = 500)
        private String remarks;
    }
}
