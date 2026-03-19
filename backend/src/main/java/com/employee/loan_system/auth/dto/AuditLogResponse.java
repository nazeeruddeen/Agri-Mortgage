package com.employee.loan_system.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private Long id;
    private String action;
    private String actorUsername;
    private Long targetUserId;
    private String targetUsername;
    private String details;
    private LocalDateTime createdAt;
}
