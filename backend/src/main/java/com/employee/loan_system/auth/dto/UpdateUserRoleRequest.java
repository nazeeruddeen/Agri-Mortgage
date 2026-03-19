package com.employee.loan_system.auth.dto;

import com.employee.loan_system.entity.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserRoleRequest {
    @NotNull(message = "Role is required")
    private UserRole role;
}
