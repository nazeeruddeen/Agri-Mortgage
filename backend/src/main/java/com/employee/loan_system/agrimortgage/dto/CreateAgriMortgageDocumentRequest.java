package com.employee.loan_system.agrimortgage.dto;

import com.employee.loan_system.agrimortgage.entity.AgriMortgageDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAgriMortgageDocumentRequest {

    @NotNull
    private AgriMortgageDocumentType documentType;

    @NotBlank
    @Size(max = 180)
    private String fileName;

    @NotBlank
    @Size(max = 255)
    private String fileReference;

    @Size(max = 500)
    private String remarks;
}
