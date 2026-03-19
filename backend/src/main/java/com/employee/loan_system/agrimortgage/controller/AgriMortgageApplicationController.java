package com.employee.loan_system.agrimortgage.controller;

import com.employee.loan_system.agrimortgage.dto.AgriEligibilityResponse;
import com.employee.loan_system.agrimortgage.dto.AgriMortgageApplicationResponse;
import com.employee.loan_system.agrimortgage.dto.AgriMortgageDashboardResponse;
import com.employee.loan_system.agrimortgage.dto.AdvanceAgriMortgageStatusRequest;
import com.employee.loan_system.agrimortgage.dto.CreateAgriMortgageApplicationRequest;
import com.employee.loan_system.agrimortgage.entity.AgriMortgageApplicationStatus;
import com.employee.loan_system.agrimortgage.service.AgriMortgageApplicationService;
import com.employee.loan_system.agrimortgage.service.AgriReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Agri Mortgage Applications", description = "APIs for managing agricultural mortgage loan applications")
@RestController
@RequestMapping("/api/v1/agri-mortgage-applications")
public class AgriMortgageApplicationController {

    private final AgriMortgageApplicationService applicationService;
    private final AgriReportService reportService;

    public AgriMortgageApplicationController(
            AgriMortgageApplicationService applicationService,
            AgriReportService reportService) {
        this.applicationService = applicationService;
        this.reportService = reportService;
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

    @Operation(summary = "Get application volume/status dashboard summary")
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public ResponseEntity<AgriMortgageDashboardResponse> summary() {
        return ResponseEntity.ok(applicationService.getDashboard());
    }

    @Operation(summary = "Export all applications to Excel (.xlsx) — streams file download")
    @GetMapping(value = "/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public ResponseEntity<byte[]> exportToExcel() throws IOException {
        byte[] excelBytes = reportService.exportApplicationsToExcel();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(
                ContentDisposition.attachment().filename("agri-mortgage-applications.xlsx").build());
        headers.setContentLength(excelBytes.length);
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    @Operation(summary = "Get district-level summary: total apps, sanctioned amount, avg LTV per district")
    @GetMapping("/reports/district-summary")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public ResponseEntity<List<AgriReportService.DistrictSummary>> districtSummary() {
        return ResponseEntity.ok(reportService.getDistrictSummary());
    }
}
