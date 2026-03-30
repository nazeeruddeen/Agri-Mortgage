package com.employee.loan_system.agrimortgage.dto;

import com.employee.loan_system.agrimortgage.entity.AgriMortgageDocumentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateAgriMortgageDocumentStatusRequest {

    @NotNull
    private AgriMortgageDocumentStatus documentStatus;

    @Size(max = 500)
    private String remarks;
}
