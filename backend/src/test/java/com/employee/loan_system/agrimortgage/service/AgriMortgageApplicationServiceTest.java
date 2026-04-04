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
import com.employee.loan_system.agrimortgage.entity.AgriMortgageLoanAccount;
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
import com.employee.loan_system.agrimortgage.repository.AgriMortgageLoanAccountRepository;
import com.employee.loan_system.agrimortgage.repository.AgriculturalLandParcelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgriMortgageApplicationServiceTest {

    @Mock
    private AgriMortgageApplicationRepository applicationRepository;

    @Mock
    private AgriMortgageDocumentRepository documentRepository;

    @Mock
    private AgriculturalLandParcelRepository landParcelRepository;

    @Mock
    private AgriMortgageLoanAccountRepository loanAccountRepository;

    @Mock
    private EncumbranceGatewayRetryWrapper encumbranceGatewayRetryWrapper;

    @Mock
    private AgriMortgageServicingService servicingService;

    @Test
    void createDraftShouldStoreApplicantsAndLand() {
        AgriMortgageApplicationService service = service();
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
        AgriMortgageApplicationService service = service();
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
        AgriMortgageApplicationService service = service();
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
        AgriMortgageApplicationService service = service();
        AgriMortgageApplication application = buildApplication();
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(AgriMortgageApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.evaluate(1L);

        assertThat(response.eligible()).isFalse();
        assertThat(response.summary()).contains("NO_ENCUMBRANCE");
    }

    @Test
    void advanceStatusShouldRejectInvalidTransition() {
        AgriMortgageApplicationService service = service();
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
        AgriMortgageApplicationService service = service();
        when(applicationRepository.findStatusCounts()).thenReturn(List.of(
                statusCount(AgriMortgageApplicationStatus.DRAFT, 1L),
                statusCount(AgriMortgageApplicationStatus.SANCTIONED, 1L)
        ));
        when(applicationRepository.count()).thenReturn(2L);
        when(applicationRepository.countByEligibleTrue()).thenReturn(1L);
        when(documentRepository.countApplicationsWithVerifiedRequiredDocuments()).thenReturn(1L);
        when(applicationRepository.countByEncumbranceVerificationStatus(EncumbranceVerificationStatus.CLEAR)).thenReturn(1L);
        when(applicationRepository.countByEncumbranceVerificationStatus(EncumbranceVerificationStatus.ENCUMBERED)).thenReturn(1L);
        when(applicationRepository.countByEncumbranceVerificationStatusIn(List.of(
                EncumbranceVerificationStatus.NOT_RUN,
                EncumbranceVerificationStatus.PENDING_VERIFICATION))).thenReturn(0L);
        when(applicationRepository.countByEncumbranceVerificationStatus(EncumbranceVerificationStatus.GATEWAY_ERROR)).thenReturn(0L);
        when(applicationRepository.sumRequestedAmount()).thenReturn(new BigDecimal("5500000.00"));
        when(landParcelRepository.countAllParcels()).thenReturn(2L);
        when(landParcelRepository.sumTotalAppraisedValue()).thenReturn(new BigDecimal("11500000.00"));

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
        verify(applicationRepository, never()).findAll();
    }

    @Test
    void searchShouldBatchLoanAccountEnrichmentForDisbursedRows() {
        AgriMortgageApplicationService service = service();
        AgriMortgageApplication disbursed = buildApplication();
        disbursed.setStatus(AgriMortgageApplicationStatus.DISBURSED);
        disbursed.setDisbursedAt(java.time.LocalDateTime.now());
        AgriMortgageLoanAccount loanAccount = new AgriMortgageLoanAccount();
        loanAccount.setId(44L);
        loanAccount.setApplication(disbursed);
        loanAccount.setAccountNumber("AGL-000044");
        loanAccount.setOutstandingPrincipal(new BigDecimal("2500000.00"));

        when(applicationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(disbursed), PageRequest.of(0, 10), 1));
        when(loanAccountRepository.findByApplication_IdIn(any()))
                .thenReturn(List.of(loanAccount));

        var page = service.search(null, null, null, null, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).loanAccountNumber()).isEqualTo("AGL-000044");
        verify(servicingService, never()).findByApplicationId(any());
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
        application.setStatus(AgriMortgageApplicationStatus.DRAFT);
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

    private AgriMortgageApplicationService service() {
        return new AgriMortgageApplicationService(
                applicationRepository,
                documentRepository,
                landParcelRepository,
                loanAccountRepository,
                new AgriEligibilityService(),
                encumbranceGatewayRetryWrapper,
                servicingService);
    }

    private AgriMortgageApplicationRepository.StatusCountRow statusCount(AgriMortgageApplicationStatus status, long total) {
        return new AgriMortgageApplicationRepository.StatusCountRow() {
            @Override
            public AgriMortgageApplicationStatus getStatus() {
                return status;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }
}
