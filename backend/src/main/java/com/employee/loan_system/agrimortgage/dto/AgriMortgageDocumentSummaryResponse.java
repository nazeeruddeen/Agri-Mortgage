package com.employee.loan_system.agrimortgage.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record AgriMortgageDocumentSummaryResponse(
        boolean documentsComplete,
        long totalDocuments,
        long verifiedDocuments,
        List<String> missingRequiredDocuments
) {
}
