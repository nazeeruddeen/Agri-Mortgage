package com.employee.loan_system.auth;

import com.employee.loan_system.auth.dto.AuditLogResponse;
import com.employee.loan_system.entity.SecurityAuditLog;
import com.employee.loan_system.repository.SecurityAuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class SecurityAuditService {

    private static final DateTimeFormatter CSV_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private SecurityAuditLogRepository securityAuditLogRepository;

    public void log(String action, Long targetUserId, String targetUsername, String details) {
        SecurityAuditLog log = new SecurityAuditLog();
        log.setAction(action);
        log.setActorUsername(resolveActor());
        log.setTargetUserId(targetUserId);
        log.setTargetUsername(targetUsername);
        log.setDetails(details);
        securityAuditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getRecentLogs(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return securityAuditLogRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, safeLimit))
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> searchLogs(String action, String actor, LocalDateTime from, LocalDateTime to, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        Pageable pageable = PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<SecurityAuditLog> spec = Specification.where(null);

        if (action != null && !action.isBlank()) {
            String normalizedAction = action.trim().toUpperCase();
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("action")), normalizedAction));
        }

        if (actor != null && !actor.isBlank()) {
            String normalizedActor = actor.trim().toLowerCase();
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("actorUsername")), "%" + normalizedActor + "%"));
        }

        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }

        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }

        return securityAuditLogRepository.findAll(spec, pageable)
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public byte[] exportLogsCsv(String action, String actor, LocalDateTime from, LocalDateTime to, int limit) {
        List<AuditLogResponse> logs = searchLogs(action, actor, from, to, limit);

        StringBuilder csv = new StringBuilder();
        csv.append("id,createdAt,action,actorUsername,targetUserId,targetUsername,details\n");

        for (AuditLogResponse log : logs) {
            csv.append(log.getId()).append(',');
            csv.append(escapeCsv(log.getCreatedAt() == null ? "" : CSV_TIME_FORMATTER.format(log.getCreatedAt()))).append(',');
            csv.append(escapeCsv(log.getAction())).append(',');
            csv.append(escapeCsv(log.getActorUsername())).append(',');
            csv.append(log.getTargetUserId() == null ? "" : log.getTargetUserId()).append(',');
            csv.append(escapeCsv(log.getTargetUsername())).append(',');
            csv.append(escapeCsv(log.getDetails())).append('\n');
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        boolean needsQuotes = escaped.contains(",") || escaped.contains("\n") || escaped.contains("\r") || escaped.contains("\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }

    private AuditLogResponse map(SecurityAuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .action(log.getAction())
                .actorUsername(log.getActorUsername())
                .targetUserId(log.getTargetUserId())
                .targetUsername(log.getTargetUsername())
                .details(log.getDetails())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private String resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            return "SYSTEM";
        }
        return authentication.getName();
    }
}
