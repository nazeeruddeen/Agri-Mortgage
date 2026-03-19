package com.employee.loan_system.agrimortgage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "agricultural_land_parcels")
public class AgriculturalLandParcel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private AgriMortgageApplication application;

    @Column(name = "survey_number", nullable = false, length = 50)
    private String surveyNumber;

    @Column(name = "district", nullable = false, length = 80)
    private String district;

    @Column(name = "taluka", nullable = false, length = 80)
    private String taluka;

    @Column(name = "village", nullable = false, length = 80)
    private String village;

    @Column(name = "area_in_acres", nullable = false, precision = 10, scale = 2)
    private BigDecimal areaInAcres;

    @Enumerated(EnumType.STRING)
    @Column(name = "land_type", nullable = false, length = 30)
    private LandType landType;

    @Column(name = "market_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal marketValue;

    @Column(name = "govt_circle_rate", nullable = false, precision = 15, scale = 2)
    private BigDecimal govtCircleRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "ownership_status", nullable = false, length = 30)
    private OwnershipStatus ownershipStatus = OwnershipStatus.SOLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "encumbrance_status", nullable = false, length = 30)
    private EncumbranceStatus encumbranceStatus = EncumbranceStatus.PENDING;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public BigDecimal appraisalValue() {
        return marketValue.min(govtCircleRate);
    }
}
