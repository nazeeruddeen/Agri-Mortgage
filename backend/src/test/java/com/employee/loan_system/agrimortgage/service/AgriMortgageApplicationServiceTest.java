package com.employee.loan_system.agrimortgage.service;

import com.employee.loan_system.agrimortgage.dto.AdvanceAgriMortgageStatusRequest;
import com.employee.loan_system.agrimortgage.dto.CreateAgriMortgageApplicationRequest;
import com.employee.loan_system.agrimortgage.dto.CreateAgriMortgageDocumentRequest;
import com.employee.loan_system.agrimortgage.dto.AgriMortgageDashboardResponse;
import com.employee.loan_system.agrimortgage.dto.UpdateAgriMortgageDocumentStatusRequest;
import com.employee.loan_system.agrimortgage.entity.AgriMortgageApplication;
import com.employee.loan_system.agrimortgage.entity.AgriMortgageApplicationStatus;
import com.employee.loan_system.agrimortgage.entity.AgriMortgageApplicant;
import com.employee.loan_system.agrimortgage.entity.AgriMortgageDocument;
import com.employee.loan_system.agrimortgage.entity.AgriMortgageDocumentStatus;
import com.employee.loan_system.agrimortgage.entity.AgriMortgageDocumentType;
import com.employee.loan_system.agrimortgage.entity.AgriculturalLandParcel;
import com.employee.loan_system.agrimortgage.entity.ApplicantType;
import com.employee.loan_system.agrimortgage.entity.EncumbranceStatus;
import com.employee.loan_system.agrimortgage.entity.EncumbranceVerificationStatus;
import com.employee.loan_system.agrimortgage.entity.LandType;
import com.employee.loan_system.agrimortgage.entity.OwnershipStatus;
import com.employee.loan_system.agrimortgage.entity.RelationshipType;
import com.employee.loan_system.agrimortgage.gateway.EncumbranceGatewayClient.EncumbranceCheckResult;
import com.employee.loan_system.agrimortgage.gateway.EncumbranceGatewayClient.EncumbranceCheckStatus;
import com.employee.loan_system.agrimortgage.gateway.EncumbranceGatewayRetryWrapper;
import com.employee.loan_system.agrimortgage.repository.AgriMortgageApplicationRepository;
import com.employee.loan_system.agrimortgage.repository.AgriMortgageDocumentRepository;
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

    @Mock
    private AgriMortgageDocumentRepository documentRepository;

    @Mock
    private EncumbranceGatewayRetryWrapper encumbranceGatewayRetryWrapper;

    @Test
    void createDraftShouldStoreApplicantsAndLand() {
        AgriMortgageApplicationService service = new AgriMortgageApplicationService(applicationRepository, documentRepository, new AgriEligibilityService(), encumbranceGatewayRetryWrapper);
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
        assertThat(response.encumbranceVerificationStatus()).isEqualTo(EncumbranceVerificationStatus.NOT_RUN);
    }

    @Test
    void addDocumentShouldUpdateDocumentSummary() {
        AgriMortgageApplicationService service = new AgriMortgageApplicationService(applicationRepository, documentRepository, new AgriEligibilityService(), encumbranceGatewayRetryWrapper);
        AgriMortgageApplication application = buildApplication();
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(AgriMortgageApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateAgriMortgageDocumentRequest request = new CreateAgriMortgageDocumentRequest();
        request.setDocumentType(AgriMortgageDocumentType.OWNERSHIP_PROOF);
        request.setFileName("ownership.pdf");
        request.setFileReference("docs/ownership.pdf");
        request.setRemarks("Uploaded from field visit");

        var response = service.addDocument(1L, request);

        assertThat(response.documents()).hasSize(3);
        assertThat(response.documents())
                .extracting(document -> document.documentType().name() + ":" + document.documentStatus().name())
                .contains("OWNERSHIP_PROOF:UPLOADED");
        assertThat(response.documentSummary().documentsComplete()).isFalse();
        assertThat(response.documentSummary().verifiedDocuments()).isEqualTo(2);
        assertThat(response.documentSummary().missingRequiredDocuments())
                .containsExactlyInAnyOrder("OWNERSHIP_PROOF", "LAND_VALUATION_REPORT");
    }

    @Test
    void runEncumbranceCheckShouldPersistGatewayOutcome() {
        AgriMortgageApplicationService service = new AgriMortgageApplicationService(applicationRepository, documentRepository, new AgriEligibilityService(), encumbranceGatewayRetryWrapper);
        AgriMortgageApplication application = buildApplication();
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(AgriMortgageApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(encumbranceGatewayRetryWrapper.checkWithRetry("S-123ERR", "Pune")).thenReturn(new EncumbranceCheckResult(
                "S-123ERR", "Pune", EncumbranceCheckStatus.GATEWAY_ERROR, "Gateway unavailable after retries", false));

        application.getLandParcels().get(0).setSurveyNumber("S-123ERR");
        var response = service.runEncumbranceCheck(1L);

        assertThat(response.encumbranceVerificationStatus()).isEqualTo(EncumbranceVerificationStatus.GATEWAY_ERROR);
        assertThat(response.landParcels().get(0).encumbranceCheckDetails()).contains("Gateway unavailable");
    }

    @Test
    void evaluateShouldFailWhenLandIsEncumbered() {
        AgriMortgageApplicationService service = new AgriMortgageApplicationService(applicationRepository, documentRepository, new AgriEligibilityService(), encumbranceGatewayRetryWrapper);
        AgriMortgageApplication application = buildApplication();
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(AgriMortgageApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.evaluate(1L);

        assertThat(response.eligible()).isFalse();
        assertThat(response.summary()).contains("NO_ENCUMBRANCE");
    }

    @Test
    void advanceStatusShouldRejectInvalidTransition() {
        AgriMortgageApplicationService service = new AgriMortgageApplicationService(applicationRepository, documentRepository, new AgriEligibilityService(), encumbranceGatewayRetryWrapper);
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
        AgriMortgageApplicationService service = new AgriMortgageApplicationService(applicationRepository, documentRepository, new AgriEligibilityService(), encumbranceGatewayRetryWrapper);
        AgriMortgageApplication draft = buildApplication();
        draft.setStatus(AgriMortgageApplicationStatus.DRAFT);
        draft.setEncumbranceVerificationStatus(EncumbranceVerificationStatus.ENCUMBERED);
        AgriMortgageApplication sanctioned = buildApplication();
        sanctioned.setId(2L);
        sanctioned.setStatus(AgriMortgageApplicationStatus.SANCTIONED);
        sanctioned.setEligible(true);
        sanctioned.setRequestedAmount(new BigDecimal("2500000"));
        sanctioned.setEncumbranceVerificationStatus(EncumbranceVerificationStatus.CLEAR);
        sanctioned.setLandParcels(new ArrayList<>(sanctioned.getLandParcels()));
        sanctioned.addDocument(createDocument(sanctioned, AgriMortgageDocumentType.OWNERSHIP_PROOF, AgriMortgageDocumentStatus.VERIFIED));
        sanctioned.addDocument(createDocument(sanctioned, AgriMortgageDocumentType.LAND_VALUATION_REPORT, AgriMortgageDocumentStatus.VERIFIED));
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
        assertThat(response.documentReadyApplications()).isEqualTo(1);
        assertThat(response.applicationsPendingDocuments()).isEqualTo(1);
        assertThat(response.clearEncumbranceApplications()).isEqualTo(1);
        assertThat(response.encumberedApplications()).isEqualTo(1);
        assertThat(response.pendingEncumbranceApplications()).isZero();
        assertThat(response.gatewayErrorApplications()).isZero();
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
        application.setEncumbranceVerificationStatus(EncumbranceVerificationStatus.NOT_RUN);

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

        application.addDocument(createDocument(application, AgriMortgageDocumentType.PATTADAR_PASSBOOK, AgriMortgageDocumentStatus.VERIFIED));
        application.addDocument(createDocument(application, AgriMortgageDocumentType.ENCUMBRANCE_CERTIFICATE, AgriMortgageDocumentStatus.VERIFIED));

        return application;
    }

    private AgriMortgageDocument createDocument(AgriMortgageApplication application, AgriMortgageDocumentType type, AgriMortgageDocumentStatus status) {
        AgriMortgageDocument document = new AgriMortgageDocument();
        document.setApplication(application);
        document.setDocumentType(type);
        document.setDocumentStatus(status);
        document.setFileName(type.name().toLowerCase() + ".pdf");
        document.setFileReference("docs/" + type.name().toLowerCase() + ".pdf");
        document.setUploadedBy("reviewer");
        return document;
    }
}
