package com.employee.loan_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "security_audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecurityAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action", nullable = false, length = 60)
    private String action;

    @Column(name = "actor_username", nullable = false, length = 120)
    private String actorUsername;

    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "target_username", length = 120)
    private String targetUsername;

    @Column(name = "details", length = 1000)
    private String details;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
