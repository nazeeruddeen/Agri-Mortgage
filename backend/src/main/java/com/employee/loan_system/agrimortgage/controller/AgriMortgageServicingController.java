package com.employee.loan_system.agrimortgage.controller;

import com.employee.loan_system.agrimortgage.dto.AgriMortgageLoanAccountResponse;
import com.employee.loan_system.agrimortgage.dto.AgriRepaymentTransactionResponse;
import com.employee.loan_system.agrimortgage.dto.RecordAgriRepaymentRequest;
import com.employee.loan_system.agrimortgage.service.AgriMortgageServicingService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Agri Mortgage Servicing", description = "Servicing account and repayment APIs for agri mortgages")
@RestController
@RequestMapping("/api/v1/agri-mortgage-applications")
public class AgriMortgageServicingController {

    private final AgriMortgageServicingService servicingService;

    public AgriMortgageServicingController(AgriMortgageServicingService servicingService) {
        this.servicingService = servicingService;
    }

    @Operation(summary = "Get the servicing account for a disbursed mortgage application")
    @GetMapping("/{applicationId}/loan-account")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public ResponseEntity<AgriMortgageLoanAccountResponse> loanAccount(@PathVariable Long applicationId) {
        return ResponseEntity.ok(servicingService.getByApplicationId(applicationId));
    }

    @Operation(summary = "List disbursed mortgage servicing accounts with pagination")
    @GetMapping("/loan-accounts")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public ResponseEntity<Page<AgriMortgageLoanAccountResponse>> loanAccounts(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(servicingService.listAccounts(pageable));
    }

    @Operation(summary = "Record a mortgage repayment against a servicing account")
    @PostMapping("/loan-accounts/{accountId}/repayments")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    public ResponseEntity<AgriRepaymentTransactionResponse> recordRepayment(
            @PathVariable Long accountId,
            @Valid @RequestBody RecordAgriRepaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicingService.recordRepayment(accountId, request));
    }
}
