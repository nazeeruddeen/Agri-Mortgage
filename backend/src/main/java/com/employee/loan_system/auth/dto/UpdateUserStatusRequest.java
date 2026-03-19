package com.employee.loan_system.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserStatusRequest {
    @NotNull(message = "Active flag is required")
    private Boolean active;
}
