package com.employee.loan_system.agrimortgage.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "agri_mortgage_applications")
public class AgriMortgageApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_number", nullable = false, length = 40, unique = true)
    private String applicationNumber;

    @Column(name = "primary_applicant_name", nullable = false, length = 150)
    private String primaryApplicantName;

    @Column(name = "primary_applicant_aadhaar", nullable = false, length = 12)
    private String primaryApplicantAadhaar;

    @Column(name = "primary_applicant_pan", nullable = false, length = 10)
    private String primaryApplicantPan;

    @Column(name = "primary_monthly_income", nullable = false, precision = 15, scale = 2)
    private BigDecimal primaryMonthlyIncome;

    @Column(name = "district", nullable = false, length = 80)
    private String district;

    @Column(name = "taluka", nullable = false, length = 80)
    private String taluka;

    @Column(name = "village", nullable = false, length = 80)
    private String village;

    @Column(name = "requested_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "requested_tenure_months", nullable = false)
    private Integer requestedTenureMonths;

    @Column(name = "purpose", nullable = false, length = 200)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 30)
    private AgriMortgageApplicationStatus status = AgriMortgageApplicationStatus.DRAFT;

    @Column(name = "eligible", nullable = false)
    private boolean eligible;

    @Column(name = "eligibility_summary", length = 1000)
    private String eligibilitySummary;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "encumbrance_verification_status", nullable = false, length = 30)
    private EncumbranceVerificationStatus encumbranceVerificationStatus = EncumbranceVerificationStatus.NOT_RUN;

    @Column(name = "encumbrance_verification_summary", length = 1000)
    private String encumbranceVerificationSummary;

    @Column(name = "encumbrance_verified_at")
    private LocalDateTime encumbranceVerifiedAt;

    /**
     * JSON snapshot of the eligibility rules evaluated at submission time.
     * Interview answer: "We snapshot the rules at submission so that if the LTV cap
     * or income thresholds change mid-review, approval still evaluates the submitted rules
     * — not the current config. This gives the borrower a deterministic outcome."
     *
     * Example: {"maxLtvRatio":0.65,"minCombinedIncome":50000,"maxEncumbranceTolerance":"NONE"}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "eligibility_rules_snapshot", columnDefinition = "JSON")
    private String eligibilityRulesSnapshot;

    @Column(name = "total_land_value", precision = 15, scale = 2)
    private BigDecimal totalLandValue;

    @Column(name = "combined_income", precision = 15, scale = 2)
    private BigDecimal combinedIncome;

    @Column(name = "ltv_ratio", precision = 5, scale = 4)
    private BigDecimal ltvRatio;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "sanctioned_at")
    private LocalDateTime sanctionedAt;

    @Column(name = "disbursed_at")
    private LocalDateTime disbursedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AgriMortgageApplicant> applicants = new ArrayList<>();

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AgriculturalLandParcel> landParcels = new ArrayList<>();

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AgriMortgageApplicationStateHistory> stateHistory = new ArrayList<>();

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AgriMortgageDocument> documents = new ArrayList<>();

    public void addApplicant(AgriMortgageApplicant applicant) {
        applicant.setApplication(this);
        applicants.add(applicant);
    }

    public void addLandParcel(AgriculturalLandParcel parcel) {
        parcel.setApplication(this);
        landParcels.add(parcel);
    }

    public void addStateHistory(AgriMortgageApplicationStateHistory history) {
        history.setApplication(this);
        stateHistory.add(history);
    }

    public void addDocument(AgriMortgageDocument document) {
        document.setApplication(this);
        documents.add(document);
    }
}
