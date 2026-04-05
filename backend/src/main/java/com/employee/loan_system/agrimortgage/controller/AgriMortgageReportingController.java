package com.employee.loan_system.agrimortgage.controller;

import com.employee.loan_system.agrimortgage.dto.AgriMortgageDashboardResponse;
import com.employee.loan_system.agrimortgage.service.AgriMortgageApplicationService;
import com.employee.loan_system.agrimortgage.service.AgriReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@Tag(name = "Agri Mortgage Reporting", description = "Dashboard and reporting APIs for agri mortgage operations")
@RestController
@RequestMapping("/api/v1/agri-mortgage-applications")
public class AgriMortgageReportingController {

    private final AgriMortgageApplicationService applicationService;
    private final AgriReportService reportService;

    public AgriMortgageReportingController(
            AgriMortgageApplicationService applicationService,
            AgriReportService reportService) {
        this.applicationService = applicationService;
        this.reportService = reportService;
    }

    @Operation(summary = "Get application volume/status dashboard summary")
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public ResponseEntity<AgriMortgageDashboardResponse> summary() {
        return ResponseEntity.ok(applicationService.getDashboard());
    }

    @Operation(summary = "Export all applications to Excel (.xlsx) - streams file download")
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
