package com.employee.loan_system.agrimortgage.controller;

import com.employee.loan_system.agrimortgage.dto.AgriMortgageApplicationResponse;
import com.employee.loan_system.agrimortgage.service.AgriMortgageApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Agri Mortgage Encumbrance", description = "Encumbrance verification APIs for agri mortgage applications")
@RestController
@RequestMapping("/api/v1/agri-mortgage-applications")
public class AgriMortgageEncumbranceController {

    private final AgriMortgageApplicationService applicationService;

    public AgriMortgageEncumbranceController(AgriMortgageApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Operation(summary = "Run encumbrance verification across all land parcels using the retry wrapper")
    @PostMapping("/{applicationId}/encumbrance-check")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public ResponseEntity<AgriMortgageApplicationResponse> runEncumbranceCheck(@PathVariable Long applicationId) {
        return ResponseEntity.ok(applicationService.runEncumbranceCheck(applicationId));
    }
}
