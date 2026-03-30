package com.employee.loan_system.agrimortgage.service;

import com.employee.loan_system.agrimortgage.dto.AgriEligibilityResponse;
import com.employee.loan_system.agrimortgage.dto.AgriMortgageApplicantResponse;
import com.employee.loan_system.agrimortgage.dto.AgriMortgageApplicationResponse;
import com.employee.loan_system.agrimortgage.dto.AgriMortgageApplicationStateHistoryResponse;
import com.employee.loan_system.agrimortgage.dto.AgriMortgageDashboardResponse;
import com.employee.loan_system.agrimortgage.dto.AgriMortgageDocumentResponse;
import com.employee.loan_system.agrimortgage.dto.AgriMortgageDocumentSummaryResponse;
import com.employee.loan_system.agrimortgage.dto.AgriculturalLandParcelResponse;
import com.employee.loan_system.agrimortgage.dto.AdvanceAgriMortgageStatusRequest;
import com.employee.loan_system.agrimortgage.dto.CreateAgriMortgageApplicationRequest;
import com.employee.loan_system.agrimortgage.dto.CreateAgriMortgageDocumentRequest;
import com.employee.loan_system.agrimortgage.dto.UpdateAgriMortgageDocumentStatusRequest;
import com.employee.loan_system.agrimortgage.entity.AgriMortgageApplication;
import com.employee.loan_system.agrimortgage.entity.AgriMortgageApplicationStateHistory;
import com.employee.loan_system.agrimortgage.entity.AgriMortgageApplicationStatus;
import com.employee.loan_system.agrimortgage.entity.AgriMortgageApplicant;
import com.employee.loan_system.agrimortgage.entity.AgriMortgageDocument;
import com.employee.loan_system.agrimortgage.entity.AgriMortgageDocumentStatus;
import com.employee.loan_system.agrimortgage.entity.AgriMortgageDocumentType;
import com.employee.loan_system.agrimortgage.entity.AgriculturalLandParcel;
import com.employee.loan_system.agrimortgage.entity.ApplicantType;
import com.employee.loan_system.agrimortgage.entity.EncumbranceStatus;
import com.employee.loan_system.agrimortgage.entity.EncumbranceVerificationStatus;
import com.employee.loan_system.agrimortgage.gateway.EncumbranceGatewayClient.EncumbranceCheckResult;
import com.employee.loan_system.agrimortgage.gateway.EncumbranceGatewayClient.EncumbranceCheckStatus;
import com.employee.loan_system.agrimortgage.gateway.EncumbranceGatewayRetryWrapper;
import com.employee.loan_system.agrimortgage.repository.AgriMortgageApplicationRepository;
import com.employee.loan_system.agrimortgage.repository.AgriMortgageDocumentRepository;
import com.employee.loan_system.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AgriMortgageApplicationService {

    private static final List<AgriMortgageDocumentType> REQUIRED_DOCUMENT_TYPES = List.of(
            AgriMortgageDocumentType.PATTADAR_PASSBOOK,
            AgriMortgageDocumentType.OWNERSHIP_PROOF,
            AgriMortgageDocumentType.ENCUMBRANCE_CERTIFICATE,
            AgriMortgageDocumentType.LAND_VALUATION_REPORT);

    private final AgriMortgageApplicationRepository applicationRepository;
    private final AgriMortgageDocumentRepository documentRepository;
    private final AgriEligibilityService eligibilityService;
    private final EncumbranceGatewayRetryWrapper encumbranceGatewayRetryWrapper;

    private final Map<AgriMortgageApplicationStatus, List<AgriMortgageApplicationStatus>> allowedTransitions = new EnumMap<>(AgriMortgageApplicationStatus.class);

    public AgriMortgageApplicationService(
            AgriMortgageApplicationRepository applicationRepository,
            AgriMortgageDocumentRepository documentRepository,
            AgriEligibilityService eligibilityService,
            EncumbranceGatewayRetryWrapper encumbranceGatewayRetryWrapper) {
        this.applicationRepository = applicationRepository;
        this.documentRepository = documentRepository;
        this.eligibilityService = eligibilityService;
        this.encumbranceGatewayRetryWrapper = encumbranceGatewayRetryWrapper;

        allowedTransitions.put(AgriMortgageApplicationStatus.DRAFT, List.of(AgriMortgageApplicationStatus.LAND_VERIFICATION, AgriMortgageApplicationStatus.REJECTED));
        allowedTransitions.put(AgriMortgageApplicationStatus.LAND_VERIFICATION, List.of(AgriMortgageApplicationStatus.ENCUMBRANCE_CHECK, AgriMortgageApplicationStatus.REJECTED));
        allowedTransitions.put(AgriMortgageApplicationStatus.ENCUMBRANCE_CHECK, List.of(AgriMortgageApplicationStatus.CREDIT_REVIEW, AgriMortgageApplicationStatus.REJECTED));
        allowedTransitions.put(AgriMortgageApplicationStatus.CREDIT_REVIEW, List.of(AgriMortgageApplicationStatus.LEGAL_REVIEW, AgriMortgageApplicationStatus.REJECTED));
        allowedTransitions.put(AgriMortgageApplicationStatus.LEGAL_REVIEW, List.of(AgriMortgageApplicationStatus.SANCTIONED, AgriMortgageApplicationStatus.REJECTED));
        allowedTransitions.put(AgriMortgageApplicationStatus.SANCTIONED, List.of(AgriMortgageApplicationStatus.DISBURSED, AgriMortgageApplicationStatus.REJECTED));
        allowedTransitions.put(AgriMortgageApplicationStatus.DISBURSED, List.of(AgriMortgageApplicationStatus.CLOSED));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    public AgriMortgageApplicationResponse createDraft(CreateAgriMortgageApplicationRequest request) {
        AgriMortgageApplication application = new AgriMortgageApplication();
        application.setApplicationNumber("AGRI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        application.setPrimaryApplicantName(request.getPrimaryApplicantName().trim());
        application.setPrimaryApplicantAadhaar(normalize(request.getPrimaryApplicantAadhaar()));
        application.setPrimaryApplicantPan(request.getPrimaryApplicantPan().trim().toUpperCase());
        application.setPrimaryMonthlyIncome(request.getPrimaryMonthlyIncome());
        application.setDistrict(request.getDistrict().trim());
        application.setTaluka(request.getTaluka().trim());
        application.setVillage(request.getVillage().trim());
        application.setRequestedAmount(request.getRequestedAmount());
        application.setRequestedTenureMonths(request.getRequestedTenureMonths());
        application.setPurpose(request.getPurpose().trim());
        application.setStatus(AgriMortgageApplicationStatus.DRAFT);
        application.setEncumbranceVerificationStatus(EncumbranceVerificationStatus.NOT_RUN);
        application.setEncumbranceVerificationSummary("Encumbrance verification has not been executed yet.");

        AgriMortgageApplicant primary = new AgriMortgageApplicant();
        primary.setApplicantType(ApplicantType.PRIMARY);
        primary.setFullName(request.getPrimaryApplicantName().trim());
        primary.setAadhaar(normalize(request.getPrimaryApplicantAadhaar()));
        primary.setPan(request.getPrimaryApplicantPan().trim().toUpperCase());
        primary.setMonthlyIncome(request.getPrimaryMonthlyIncome());
        application.addApplicant(primary);

        request.getCoBorrowers().forEach(coBorrower -> {
            AgriMortgageApplicant applicant = new AgriMortgageApplicant();
            applicant.setApplicantType(ApplicantType.CO_BORROWER);
            applicant.setFullName(coBorrower.getFullName().trim());
            applicant.setAadhaar(normalize(coBorrower.getAadhaar()));
            applicant.setPan(coBorrower.getPan().trim().toUpperCase());
            applicant.setMonthlyIncome(coBorrower.getMonthlyIncome());
            applicant.setRelationshipType(coBorrower.getRelationshipType());
            application.addApplicant(applicant);
        });

        request.getLandParcels().forEach(parcelRequest -> {
            AgriculturalLandParcel parcel = new AgriculturalLandParcel();
            parcel.setSurveyNumber(parcelRequest.getSurveyNumber().trim());
            parcel.setDistrict(parcelRequest.getDistrict().trim());
            parcel.setTaluka(parcelRequest.getTaluka().trim());
            parcel.setVillage(parcelRequest.getVillage().trim());
            parcel.setAreaInAcres(parcelRequest.getAreaInAcres());
            parcel.setLandType(parcelRequest.getLandType());
            parcel.setMarketValue(parcelRequest.getMarketValue());
            parcel.setGovtCircleRate(parcelRequest.getGovtCircleRate());
            parcel.setOwnershipStatus(parcelRequest.getOwnershipStatus());
            parcel.setEncumbranceStatus(parcelRequest.getEncumbranceStatus());
            parcel.setRemarks(parcelRequest.getRemarks());
            application.addLandParcel(parcel);
        });

        application.addStateHistory(history(null, AgriMortgageApplicationStatus.DRAFT, "Draft created"));
        AgriMortgageApplication saved = applicationRepository.save(application);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public AgriMortgageApplicationResponse getApplication(Long applicationId) {
        return toResponse(findApplication(applicationId));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public List<AgriMortgageDocumentResponse> documents(Long applicationId) {
        findApplication(applicationId);
        return documentRepository.findByApplicationIdOrderByUploadedAtAsc(applicationId).stream()
                .map(this::toDocumentResponse)
                .toList();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public AgriMortgageApplicationResponse addDocument(Long applicationId, CreateAgriMortgageDocumentRequest request) {
        AgriMortgageApplication application = findApplication(applicationId);
        AgriMortgageDocument document = new AgriMortgageDocument();
        document.setDocumentType(request.getDocumentType());
        document.setFileName(request.getFileName().trim());
        document.setFileReference(request.getFileReference().trim());
        document.setRemarks(trimToNull(request.getRemarks()));
        document.setUploadedBy(currentActor());
        application.addDocument(document);
        return toResponse(applicationRepository.save(application));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public AgriMortgageApplicationResponse updateDocumentStatus(Long applicationId, Long documentId, UpdateAgriMortgageDocumentStatusRequest request) {
        AgriMortgageApplication application = findApplication(applicationId);
        AgriMortgageDocument document = application.getDocuments().stream()
                .filter(item -> item.getId().equals(documentId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Agri mortgage document not found with id: " + documentId));
        document.setDocumentStatus(request.getDocumentStatus());
        document.setRemarks(trimToNull(request.getRemarks()));
        document.setReviewedBy(currentActor());
        document.setReviewedAt(LocalDateTime.now());
        documentRepository.save(document);
        return toResponse(application);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public AgriMortgageApplicationResponse runEncumbranceCheck(Long applicationId) {
        AgriMortgageApplication application = findApplication(applicationId);
        if (application.getLandParcels().isEmpty()) {
            throw new IllegalArgumentException("At least one land parcel is required before encumbrance verification");
        }

        int clearCount = 0;
        int encumberedCount = 0;
        int pendingCount = 0;
        int gatewayErrorCount = 0;

        for (AgriculturalLandParcel parcel : application.getLandParcels()) {
            EncumbranceCheckResult result = encumbranceGatewayRetryWrapper.checkWithRetry(parcel.getSurveyNumber(), parcel.getDistrict());
            parcel.setEncumbranceCheckedAt(LocalDateTime.now());
            parcel.setGatewayAvailable(result.gatewayAvailable());
            parcel.setEncumbranceCheckDetails(trimToNull(result.encumbranceDetails()));
            switch (result.status()) {
                case CLEAR -> {
                    parcel.setEncumbranceStatus(EncumbranceStatus.CLEAR);
                    clearCount++;
                }
                case ENCUMBERED -> {
                    parcel.setEncumbranceStatus(EncumbranceStatus.ENCUMBERED);
                    encumberedCount++;
                }
                case PENDING_VERIFICATION -> {
                    parcel.setEncumbranceStatus(EncumbranceStatus.PENDING);
                    pendingCount++;
                }
                case GATEWAY_ERROR -> {
                    parcel.setEncumbranceStatus(EncumbranceStatus.PENDING);
                    gatewayErrorCount++;
                }
            }
        }

        EncumbranceVerificationStatus verificationStatus;
        if (gatewayErrorCount > 0) {
            verificationStatus = EncumbranceVerificationStatus.GATEWAY_ERROR;
        } else if (encumberedCount > 0) {
            verificationStatus = EncumbranceVerificationStatus.ENCUMBERED;
        } else if (pendingCount > 0) {
            verificationStatus = EncumbranceVerificationStatus.PENDING_VERIFICATION;
        } else {
            verificationStatus = EncumbranceVerificationStatus.CLEAR;
        }

        application.setEncumbranceVerificationStatus(verificationStatus);
        application.setEncumbranceVerifiedAt(LocalDateTime.now());
        application.setEncumbranceVerificationSummary(buildEncumbranceSummary(clearCount, encumberedCount, pendingCount, gatewayErrorCount));
        application.addStateHistory(history(application.getStatus(), application.getStatus(), "Encumbrance verification executed"));
        return toResponse(applicationRepository.save(application));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public AgriEligibilityResponse evaluate(Long applicationId) {
        AgriMortgageApplication application = findApplication(applicationId);
        AgriEligibilityResponse evaluation = eligibilityService.evaluate(application);
        application.setEligible(evaluation.eligible());
        application.setEligibilitySummary(evaluation.summary());
        application.setTotalLandValue(evaluation.totalLandValue());
        application.setCombinedIncome(evaluation.combinedIncome());
        application.setLtvRatio(evaluation.ltvRatio());
        applicationRepository.save(application);
        return evaluation;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public AgriMortgageApplicationResponse advanceStatus(Long applicationId, AdvanceAgriMortgageStatusRequest request) {
        AgriMortgageApplication application = findApplication(applicationId);
        AgriMortgageApplicationStatus current = application.getStatus();
        AgriMortgageApplicationStatus target = request.getTargetStatus();
        if (current == target) {
            return toResponse(application);
        }
        if (!allowedTransitions.getOrDefault(current, List.of()).contains(target)) {
            throw new IllegalArgumentException("Invalid transition from " + current + " to " + target);
        }
        if (target == AgriMortgageApplicationStatus.CREDIT_REVIEW && application.getEncumbranceVerificationStatus() != EncumbranceVerificationStatus.CLEAR) {
            throw new IllegalArgumentException("Only applications with clear encumbrance verification can move to CREDIT_REVIEW");
        }
        if (target == AgriMortgageApplicationStatus.SANCTIONED && !application.isEligible()) {
            throw new IllegalArgumentException("Only eligible applications can be sanctioned");
        }
        if (target == AgriMortgageApplicationStatus.SANCTIONED && !documentSummary(application).documentsComplete()) {
            throw new IllegalArgumentException("Required land and legal documents must be verified before sanction");
        }
        if (target == AgriMortgageApplicationStatus.DISBURSED && current != AgriMortgageApplicationStatus.SANCTIONED) {
            throw new IllegalArgumentException("Only sanctioned applications can be disbursed");
        }
        if (target == AgriMortgageApplicationStatus.CLOSED && current != AgriMortgageApplicationStatus.DISBURSED) {
            throw new IllegalArgumentException("Only disbursed applications can be closed");
        }

        application.setStatus(target);
        if (target == AgriMortgageApplicationStatus.LAND_VERIFICATION && application.getSubmittedAt() == null) {
            application.setSubmittedAt(LocalDateTime.now());
        }
        if (target == AgriMortgageApplicationStatus.SANCTIONED) {
            application.setSanctionedAt(LocalDateTime.now());
        }
        if (target == AgriMortgageApplicationStatus.DISBURSED) {
            application.setDisbursedAt(LocalDateTime.now());
        }
        application.addStateHistory(history(current, target, request.getRemarks()));
        applicationRepository.save(application);
        return toResponse(application);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public Page<AgriMortgageApplicationResponse> search(
            String district,
            String taluka,
            AgriMortgageApplicationStatus status,
            BigDecimal minAmount,
            Pageable pageable) {
        Specification<AgriMortgageApplication> spec = Specification.where(null);

        if (district != null && !district.isBlank()) {
            String normalized = district.trim().toLowerCase();
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("district")), "%" + normalized + "%"));
        }
        if (taluka != null && !taluka.isBlank()) {
            String normalized = taluka.trim().toLowerCase();
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("taluka")), "%" + normalized + "%"));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (minAmount != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("requestedAmount"), minAmount));
        }

        return applicationRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public AgriMortgageDashboardResponse getDashboard() {
        List<AgriMortgageApplication> applications = applicationRepository.findAll();
        Map<AgriMortgageApplicationStatus, Long> statusCounts = new EnumMap<>(AgriMortgageApplicationStatus.class);
        for (AgriMortgageApplicationStatus status : AgriMortgageApplicationStatus.values()) {
            statusCounts.put(status, applicationRepository.countByStatus(status));
        }

        BigDecimal totalRequestedAmount = applications.stream()
                .map(AgriMortgageApplication::getRequestedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        long totalLandParcels = applications.stream()
                .mapToLong(application -> application.getLandParcels().size())
                .sum();

        BigDecimal totalAppraisedValue = applications.stream()
                .flatMap(application -> application.getLandParcels().stream())
                .map(AgriculturalLandParcel::appraisalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal averageRequestedAmount = applications.isEmpty()
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : totalRequestedAmount.divide(BigDecimal.valueOf(applications.size()), 2, RoundingMode.HALF_UP);

        long documentReadyApplications = applications.stream()
                .filter(application -> documentSummary(application).documentsComplete())
                .count();

        long clearEncumbranceApplications = applications.stream()
                .filter(application -> application.getEncumbranceVerificationStatus() == EncumbranceVerificationStatus.CLEAR)
                .count();

        long encumberedApplications = applications.stream()
                .filter(application -> application.getEncumbranceVerificationStatus() == EncumbranceVerificationStatus.ENCUMBERED)
                .count();

        long pendingEncumbranceApplications = applications.stream()
                .filter(application -> application.getEncumbranceVerificationStatus() == EncumbranceVerificationStatus.NOT_RUN
                        || application.getEncumbranceVerificationStatus() == EncumbranceVerificationStatus.PENDING_VERIFICATION)
                .count();

        long gatewayErrorApplications = applications.stream()
                .filter(application -> application.getEncumbranceVerificationStatus() == EncumbranceVerificationStatus.GATEWAY_ERROR)
                .count();

        return AgriMortgageDashboardResponse.builder()
                .totalApplications(applications.size())
                .eligibleApplications(applications.stream().filter(AgriMortgageApplication::isEligible).count())
                .documentReadyApplications(documentReadyApplications)
                .applicationsPendingDocuments(applications.size() - documentReadyApplications)
                .clearEncumbranceApplications(clearEncumbranceApplications)
                .encumberedApplications(encumberedApplications)
                .pendingEncumbranceApplications(pendingEncumbranceApplications)
                .gatewayErrorApplications(gatewayErrorApplications)
                .draftApplications(statusCounts.getOrDefault(AgriMortgageApplicationStatus.DRAFT, 0L))
                .landVerificationApplications(statusCounts.getOrDefault(AgriMortgageApplicationStatus.LAND_VERIFICATION, 0L))
                .encumbranceCheckApplications(statusCounts.getOrDefault(AgriMortgageApplicationStatus.ENCUMBRANCE_CHECK, 0L))
                .creditReviewApplications(statusCounts.getOrDefault(AgriMortgageApplicationStatus.CREDIT_REVIEW, 0L))
                .legalReviewApplications(statusCounts.getOrDefault(AgriMortgageApplicationStatus.LEGAL_REVIEW, 0L))
                .sanctionedApplications(statusCounts.getOrDefault(AgriMortgageApplicationStatus.SANCTIONED, 0L))
                .disbursedApplications(statusCounts.getOrDefault(AgriMortgageApplicationStatus.DISBURSED, 0L))
                .rejectedApplications(statusCounts.getOrDefault(AgriMortgageApplicationStatus.REJECTED, 0L))
                .closedApplications(statusCounts.getOrDefault(AgriMortgageApplicationStatus.CLOSED, 0L))
                .totalRequestedAmount(totalRequestedAmount)
                .averageRequestedAmount(averageRequestedAmount)
                .totalLandParcels(totalLandParcels)
                .totalAppraisedValue(totalAppraisedValue)
                .statusCounts(statusCounts)
                .build();
    }

    private AgriMortgageApplication findApplication(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Agri mortgage application not found with id: " + applicationId));
    }

    private AgriMortgageApplicationStateHistory history(AgriMortgageApplicationStatus from, AgriMortgageApplicationStatus to, String remarks) {
        AgriMortgageApplicationStateHistory history = new AgriMortgageApplicationStateHistory();
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setRemarks(remarks == null ? null : remarks.trim());
        history.setChangedBy(currentActor());
        return history;
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            return "SYSTEM";
        }
        return authentication.getName();
    }

    private AgriMortgageApplicationResponse toResponse(AgriMortgageApplication application) {
        List<AgriMortgageApplicantResponse> applicants = application.getApplicants().stream()
                .map(this::toApplicantResponse)
                .toList();
        List<AgriculturalLandParcelResponse> landParcels = application.getLandParcels().stream()
                .map(this::toParcelResponse)
                .toList();
        List<AgriMortgageApplicationStateHistoryResponse> history = application.getStateHistory().stream()
                .map(item -> AgriMortgageApplicationStateHistoryResponse.builder()
                        .fromStatus(item.getFromStatus())
                        .toStatus(item.getToStatus())
                        .remarks(item.getRemarks())
                        .changedBy(item.getChangedBy())
                        .changedAt(item.getChangedAt())
                        .build())
                .toList();
        List<AgriMortgageDocumentResponse> documents = application.getDocuments().stream()
                .map(this::toDocumentResponse)
                .toList();

        return AgriMortgageApplicationResponse.builder()
                .id(application.getId())
                .applicationNumber(application.getApplicationNumber())
                .primaryApplicantName(application.getPrimaryApplicantName())
                .primaryApplicantAadhaar(application.getPrimaryApplicantAadhaar())
                .primaryApplicantPan(application.getPrimaryApplicantPan())
                .primaryMonthlyIncome(application.getPrimaryMonthlyIncome())
                .district(application.getDistrict())
                .taluka(application.getTaluka())
                .village(application.getVillage())
                .requestedAmount(application.getRequestedAmount())
                .requestedTenureMonths(application.getRequestedTenureMonths())
                .purpose(application.getPurpose())
                .status(application.getStatus())
                .eligible(application.isEligible())
                .eligibilitySummary(application.getEligibilitySummary())
                .encumbranceVerificationStatus(application.getEncumbranceVerificationStatus())
                .encumbranceVerificationSummary(application.getEncumbranceVerificationSummary())
                .encumbranceVerifiedAt(application.getEncumbranceVerifiedAt())
                .totalLandValue(application.getTotalLandValue())
                .combinedIncome(application.getCombinedIncome())
                .ltvRatio(application.getLtvRatio())
                .submittedAt(application.getSubmittedAt())
                .sanctionedAt(application.getSanctionedAt())
                .disbursedAt(application.getDisbursedAt())
                .documentSummary(documentSummary(application))
                .documents(documents)
                .applicants(applicants)
                .landParcels(landParcels)
                .stateHistory(history)
                .build();
    }

    private AgriMortgageApplicantResponse toApplicantResponse(AgriMortgageApplicant applicant) {
        return AgriMortgageApplicantResponse.builder()
                .id(applicant.getId())
                .applicantType(applicant.getApplicantType())
                .fullName(applicant.getFullName())
                .aadhaar(applicant.getAadhaar())
                .pan(applicant.getPan())
                .monthlyIncome(applicant.getMonthlyIncome())
                .relationshipType(applicant.getRelationshipType())
                .build();
    }

    private AgriculturalLandParcelResponse toParcelResponse(AgriculturalLandParcel parcel) {
        return AgriculturalLandParcelResponse.builder()
                .id(parcel.getId())
                .surveyNumber(parcel.getSurveyNumber())
                .district(parcel.getDistrict())
                .taluka(parcel.getTaluka())
                .village(parcel.getVillage())
                .areaInAcres(parcel.getAreaInAcres())
                .landType(parcel.getLandType())
                .marketValue(parcel.getMarketValue())
                .govtCircleRate(parcel.getGovtCircleRate())
                .ownershipStatus(parcel.getOwnershipStatus())
                .encumbranceStatus(parcel.getEncumbranceStatus())
                .remarks(parcel.getRemarks())
                .appraisalValue(parcel.appraisalValue())
                .encumbranceCheckDetails(parcel.getEncumbranceCheckDetails())
                .gatewayAvailable(parcel.getGatewayAvailable())
                .encumbranceCheckedAt(parcel.getEncumbranceCheckedAt())
                .build();
    }

    private AgriMortgageDocumentResponse toDocumentResponse(AgriMortgageDocument document) {
        return AgriMortgageDocumentResponse.builder()
                .id(document.getId())
                .documentType(document.getDocumentType())
                .documentStatus(document.getDocumentStatus())
                .fileName(document.getFileName())
                .fileReference(document.getFileReference())
                .remarks(document.getRemarks())
                .uploadedBy(document.getUploadedBy())
                .uploadedAt(document.getUploadedAt())
                .reviewedBy(document.getReviewedBy())
                .reviewedAt(document.getReviewedAt())
                .build();
    }

    private AgriMortgageDocumentSummaryResponse documentSummary(AgriMortgageApplication application) {
        List<String> missingRequiredDocuments = new ArrayList<>();
        for (AgriMortgageDocumentType type : REQUIRED_DOCUMENT_TYPES) {
            boolean verified = application.getDocuments().stream().anyMatch(document ->
                    document.getDocumentType() == type && document.getDocumentStatus() == AgriMortgageDocumentStatus.VERIFIED);
            if (!verified) {
                missingRequiredDocuments.add(type.name());
            }
        }
        long verifiedCount = application.getDocuments().stream()
                .filter(document -> document.getDocumentStatus() == AgriMortgageDocumentStatus.VERIFIED)
                .count();
        return AgriMortgageDocumentSummaryResponse.builder()
                .documentsComplete(missingRequiredDocuments.isEmpty())
                .totalDocuments(application.getDocuments().size())
                .verifiedDocuments(verifiedCount)
                .missingRequiredDocuments(missingRequiredDocuments)
                .build();
    }

    private String buildEncumbranceSummary(int clearCount, int encumberedCount, int pendingCount, int gatewayErrorCount) {
        List<String> parts = new ArrayList<>();
        parts.add(clearCount + " clear");
        if (encumberedCount > 0) {
            parts.add(encumberedCount + " encumbered");
        }
        if (pendingCount > 0) {
            parts.add(pendingCount + " pending verification");
        }
        if (gatewayErrorCount > 0) {
            parts.add(gatewayErrorCount + " gateway retry failures");
        }
        return String.join(", ", parts);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalize(String value) {
        return value == null ? null : value.replaceAll("\\s+", "").trim();
    }
}
