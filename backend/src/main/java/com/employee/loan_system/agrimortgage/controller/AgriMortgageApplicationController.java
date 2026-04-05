package com.employee.loan_system.agrimortgage.controller;

import com.employee.loan_system.agrimortgage.dto.AdvanceAgriMortgageStatusRequest;
import com.employee.loan_system.agrimortgage.dto.AgriEligibilityResponse;
import com.employee.loan_system.agrimortgage.dto.AgriMortgageApplicationResponse;
import com.employee.loan_system.agrimortgage.dto.CreateAgriMortgageApplicationRequest;
import com.employee.loan_system.agrimortgage.entity.AgriMortgageApplicationStatus;
import com.employee.loan_system.agrimortgage.service.AgriMortgageApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Tag(name = "Agri Mortgage Applications", description = "Application lifecycle APIs for agricultural mortgage loans")
@RestController
@RequestMapping("/api/v1/agri-mortgage-applications")
public class AgriMortgageApplicationController {

    private final AgriMortgageApplicationService applicationService;

    public AgriMortgageApplicationController(AgriMortgageApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Operation(summary = "Create a new agri mortgage application in DRAFT state")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    public ResponseEntity<AgriMortgageApplicationResponse> createDraft(@Valid @RequestBody CreateAgriMortgageApplicationRequest request) {
        return new ResponseEntity<>(applicationService.createDraft(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Get an agri mortgage application by ID")
    @GetMapping("/{applicationId}")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public ResponseEntity<AgriMortgageApplicationResponse> getApplication(@PathVariable Long applicationId) {
        return ResponseEntity.ok(applicationService.getApplication(applicationId));
    }

    @Operation(summary = "Run composite eligibility evaluation for an application")
    @PostMapping("/{applicationId}/evaluate")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public ResponseEntity<AgriEligibilityResponse> evaluate(@PathVariable Long applicationId) {
        return ResponseEntity.ok(applicationService.evaluate(applicationId));
    }

    @Operation(summary = "Advance the application to the next state in the state machine")
    @PostMapping("/{applicationId}/status")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public ResponseEntity<AgriMortgageApplicationResponse> advanceStatus(
            @PathVariable Long applicationId,
            @Valid @RequestBody AdvanceAgriMortgageStatusRequest request) {
        return ResponseEntity.ok(applicationService.advanceStatus(applicationId, request));
    }

    @Operation(summary = "Search/filter applications with pagination")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public ResponseEntity<Page<AgriMortgageApplicationResponse>> search(
            @Parameter(description = "Filter by district name (partial match)") @RequestParam(required = false) String district,
            @Parameter(description = "Filter by taluka name (partial match)") @RequestParam(required = false) String taluka,
            @Parameter(description = "Filter by application status") @RequestParam(required = false) AgriMortgageApplicationStatus status,
            @Parameter(description = "Filter by minimum requested amount") @RequestParam(required = false) BigDecimal minAmount,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(applicationService.search(district, taluka, status, minAmount, pageable));
    }
}
