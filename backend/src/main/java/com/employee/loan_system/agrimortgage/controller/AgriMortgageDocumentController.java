package com.employee.loan_system.agrimortgage.controller;

import com.employee.loan_system.agrimortgage.dto.AgriMortgageApplicationResponse;
import com.employee.loan_system.agrimortgage.dto.AgriMortgageDocumentResponse;
import com.employee.loan_system.agrimortgage.dto.CreateAgriMortgageDocumentRequest;
import com.employee.loan_system.agrimortgage.dto.UpdateAgriMortgageDocumentStatusRequest;
import com.employee.loan_system.agrimortgage.service.AgriMortgageApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Agri Mortgage Documents", description = "Document and verification APIs for agri mortgage applications")
@RestController
@RequestMapping("/api/v1/agri-mortgage-applications/{applicationId}/documents")
public class AgriMortgageDocumentController {

    private final AgriMortgageApplicationService applicationService;

    public AgriMortgageDocumentController(AgriMortgageApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Operation(summary = "List land/legal documents for an agri mortgage application")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public ResponseEntity<List<AgriMortgageDocumentResponse>> documents(@PathVariable Long applicationId) {
        return ResponseEntity.ok(applicationService.documents(applicationId));
    }

    @Operation(summary = "Add document metadata to an agri mortgage application")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public ResponseEntity<AgriMortgageApplicationResponse> addDocument(
            @PathVariable Long applicationId,
            @Valid @RequestBody CreateAgriMortgageDocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationService.addDocument(applicationId, request));
    }

    @Operation(summary = "Update document verification status")
    @PatchMapping("/{documentId}/status")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public ResponseEntity<AgriMortgageApplicationResponse> updateDocumentStatus(
            @PathVariable Long applicationId,
            @PathVariable Long documentId,
            @Valid @RequestBody UpdateAgriMortgageDocumentStatusRequest request) {
        return ResponseEntity.ok(applicationService.updateDocumentStatus(applicationId, documentId, request));
    }
}
