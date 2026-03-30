package com.employee.loan_system.agrimortgage.dto;

import com.employee.loan_system.agrimortgage.entity.AgriMortgageDocumentStatus;
import com.employee.loan_system.agrimortgage.entity.AgriMortgageDocumentType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AgriMortgageDocumentResponse(
        Long id,
        AgriMortgageDocumentType documentType,
        AgriMortgageDocumentStatus documentStatus,
        String fileName,
        String fileReference,
        String remarks,
        String uploadedBy,
        LocalDateTime uploadedAt,
        String reviewedBy,
        LocalDateTime reviewedAt
) {
}
