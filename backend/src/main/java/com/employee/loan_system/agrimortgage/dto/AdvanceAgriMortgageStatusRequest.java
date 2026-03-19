package com.employee.loan_system.agrimortgage.dto;

import com.employee.loan_system.agrimortgage.entity.AgriMortgageApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdvanceAgriMortgageStatusRequest {

    @NotNull
    private AgriMortgageApplicationStatus targetStatus;

    @Size(max = 500)
    private String remarks;
}
