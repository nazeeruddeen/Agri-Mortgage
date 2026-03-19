package com.employee.loan_system.agrimortgage.service;

import com.employee.loan_system.agrimortgage.dto.AdvanceAgriMortgageStatusRequest;
import com.employee.loan_system.agrimortgage.dto.CreateAgriMortgageApplicationRequest;
import com.employee.loan_system.agrimortgage.dto.AgriMortgageDashboardResponse;
import com.employee.loan_system.agrimortgage.entity.AgriMortgageApplication;
import com.employee.loan_system.agrimortgage.entity.AgriMortgageApplicationStatus;
import com.employee.loan_system.agrimortgage.entity.AgriMortgageApplicant;
import com.employee.loan_system.agrimortgage.entity.AgriculturalLandParcel;
import com.employee.loan_system.agrimortgage.entity.ApplicantType;
import com.employee.loan_system.agrimortgage.entity.EncumbranceStatus;
import com.employee.loan_system.agrimortgage.entity.LandType;
import com.employee.loan_system.agrimortgage.entity.OwnershipStatus;
import com.employee.loan_system.agrimortgage.entity.RelationshipType;
import com.employee.loan_system.agrimortgage.repository.AgriMortgageApplicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgriMortgageApplicationServiceTest {

    @Mock
    private AgriMortgageApplicationRepository applicationRepository;

    @Test
    void createDraftShouldStoreApplicantsAndLand() {
        AgriMortgageApplicationService service = new AgriMortgageApplicationService(applicationRepository, new AgriEligibilityService());
        when(applicationRepository.save(any(AgriMortgageApplication.class))).thenAnswer(invocation -> {
            AgriMortgageApplication application = invocation.getArgument(0);
            application.setId(1L);
            return application;
        });

        CreateAgriMortgageApplicationRequest request = buildRequest();

        var response = service.createDraft(request);

        assertThat(response.applicants()).hasSize(2);
        assertThat(response.landParcels()).hasSize(1);
        assertThat(response.status()).isEqualTo(AgriMortgageApplicationStatus.DRAFT);
    }

    @Test
    void evaluateShouldFailWhenLandIsEncumbered() {
        AgriMortgageApplicationService service = new AgriMortgageApplicationService(applicationRepository, new AgriEligibilityService());
        AgriMortgageApplication application = buildApplication();
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(AgriMortgageApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.evaluate(1L);

        assertThat(response.eligible()).isFalse();
        assertThat(response.summary()).contains("NO_ENCUMBRANCE");
    }

    @Test
    void advanceStatusShouldRejectInvalidTransition() {
        AgriMortgageApplicationService service = new AgriMortgageApplicationService(applicationRepository, new AgriEligibilityService());
        AgriMortgageApplication application = buildApplication();
        application.setStatus(AgriMortgageApplicationStatus.DRAFT);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        AdvanceAgriMortgageStatusRequest request = new AdvanceAgriMortgageStatusRequest();
        request.setTargetStatus(AgriMortgageApplicationStatus.DISBURSED);
        request.setRemarks("Skip to disbursement");

        assertThatThrownBy(() -> service.advanceStatus(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid transition");
    }

    @Test
    void dashboardShouldAggregateCountsAndAmounts() {
        AgriMortgageApplicationService service = new AgriMortgageApplicationService(applicationRepository, new AgriEligibilityService());
        AgriMortgageApplication draft = buildApplication();
        draft.setStatus(AgriMortgageApplicationStatus.DRAFT);
        AgriMortgageApplication sanctioned = buildApplication();
        sanctioned.setId(2L);
        sanctioned.setStatus(AgriMortgageApplicationStatus.SANCTIONED);
        sanctioned.setEligible(true);
        sanctioned.setRequestedAmount(new BigDecimal("2500000"));
        sanctioned.setLandParcels(new ArrayList<>(sanctioned.getLandParcels()));
        when(applicationRepository.findAll()).thenReturn(List.of(draft, sanctioned));
        when(applicationRepository.countByStatus(AgriMortgageApplicationStatus.DRAFT)).thenReturn(1L);
        when(applicationRepository.countByStatus(AgriMortgageApplicationStatus.LAND_VERIFICATION)).thenReturn(0L);
        when(applicationRepository.countByStatus(AgriMortgageApplicationStatus.ENCUMBRANCE_CHECK)).thenReturn(0L);
        when(applicationRepository.countByStatus(AgriMortgageApplicationStatus.CREDIT_REVIEW)).thenReturn(0L);
        when(applicationRepository.countByStatus(AgriMortgageApplicationStatus.LEGAL_REVIEW)).thenReturn(0L);
        when(applicationRepository.countByStatus(AgriMortgageApplicationStatus.SANCTIONED)).thenReturn(1L);
        when(applicationRepository.countByStatus(AgriMortgageApplicationStatus.DISBURSED)).thenReturn(0L);
        when(applicationRepository.countByStatus(AgriMortgageApplicationStatus.REJECTED)).thenReturn(0L);
        when(applicationRepository.countByStatus(AgriMortgageApplicationStatus.CLOSED)).thenReturn(0L);

        AgriMortgageDashboardResponse response = service.getDashboard();

        assertThat(response.totalApplications()).isEqualTo(2);
        assertThat(response.eligibleApplications()).isEqualTo(1);
        assertThat(response.sanctionedApplications()).isEqualTo(1);
        assertThat(response.totalRequestedAmount()).isEqualByComparingTo("5500000.00");
    }

    private CreateAgriMortgageApplicationRequest buildRequest() {
        CreateAgriMortgageApplicationRequest request = new CreateAgriMortgageApplicationRequest();
        request.setPrimaryApplicantName("Ramesh Patil");
        request.setPrimaryApplicantAadhaar("123412341234");
        request.setPrimaryApplicantPan("ABCDE1234F");
        request.setPrimaryMonthlyIncome(new BigDecimal("75000"));
        request.setDistrict("Pune");
        request.setTaluka("Baramati");
        request.setVillage("Morgaon");
        request.setRequestedAmount(new BigDecimal("3000000"));
        request.setRequestedTenureMonths(48);
        request.setPurpose("Irrigation equipment purchase");

        CreateAgriMortgageApplicationRequest.CoBorrowerRequest coBorrower = new CreateAgriMortgageApplicationRequest.CoBorrowerRequest();
        coBorrower.setFullName("Suresh Patil");
        coBorrower.setAadhaar("432143214321");
        coBorrower.setPan("PQRSX6789K");
        coBorrower.setMonthlyIncome(new BigDecimal("40000"));
        coBorrower.setRelationshipType(RelationshipType.SPOUSE);
        request.setCoBorrowers(List.of(coBorrower));

        CreateAgriMortgageApplicationRequest.LandParcelRequest parcel = new CreateAgriMortgageApplicationRequest.LandParcelRequest();
        parcel.setSurveyNumber("S-123");
        parcel.setDistrict("Pune");
        parcel.setTaluka("Baramati");
        parcel.setVillage("Morgaon");
        parcel.setAreaInAcres(new BigDecimal("3.50"));
        parcel.setLandType(LandType.IRRIGATED);
        parcel.setMarketValue(new BigDecimal("6000000"));
        parcel.setGovtCircleRate(new BigDecimal("5500000"));
        parcel.setOwnershipStatus(OwnershipStatus.SOLE);
        parcel.setEncumbranceStatus(EncumbranceStatus.CLEAR);
        request.setLandParcels(List.of(parcel));

        return request;
    }

    private AgriMortgageApplication buildApplication() {
        AgriMortgageApplication application = new AgriMortgageApplication();
        application.setId(1L);
        application.setApplicationNumber("AGRI-TEST");
        application.setPrimaryApplicantName("Ramesh Patil");
        application.setPrimaryApplicantAadhaar("123412341234");
        application.setPrimaryApplicantPan("ABCDE1234F");
        application.setPrimaryMonthlyIncome(new BigDecimal("75000"));
        application.setDistrict("Pune");
        application.setTaluka("Baramati");
        application.setVillage("Morgaon");
        application.setRequestedAmount(new BigDecimal("3000000"));
        application.setRequestedTenureMonths(48);
        application.setPurpose("Irrigation equipment purchase");

        AgriMortgageApplicant primary = new AgriMortgageApplicant();
        primary.setApplicantType(ApplicantType.PRIMARY);
        primary.setFullName("Ramesh Patil");
        primary.setAadhaar("123412341234");
        primary.setPan("ABCDE1234F");
        primary.setMonthlyIncome(new BigDecimal("75000"));
        application.addApplicant(primary);

        AgriculturalLandParcel parcel = new AgriculturalLandParcel();
        parcel.setSurveyNumber("S-123");
        parcel.setDistrict("Pune");
        parcel.setTaluka("Baramati");
        parcel.setVillage("Morgaon");
        parcel.setAreaInAcres(new BigDecimal("3.50"));
        parcel.setLandType(LandType.IRRIGATED);
        parcel.setMarketValue(new BigDecimal("6000000"));
        parcel.setGovtCircleRate(new BigDecimal("5500000"));
        parcel.setOwnershipStatus(OwnershipStatus.SOLE);
        parcel.setEncumbranceStatus(EncumbranceStatus.ENCUMBERED);
        application.addLandParcel(parcel);

        return application;
    }
}
