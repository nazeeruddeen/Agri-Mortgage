package com.employee.loan_system.repository;

import com.employee.loan_system.entity.SecurityAuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface SecurityAuditLogRepository extends JpaRepository<SecurityAuditLog, Long>, JpaSpecificationExecutor<SecurityAuditLog> {
    List<SecurityAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
